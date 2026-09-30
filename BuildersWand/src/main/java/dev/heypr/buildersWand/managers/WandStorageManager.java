package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.managers.io.ConfigManager;
import dev.heypr.buildersWand.utility.ComponentUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class WandStorageManager {

    private static final String FILE_SUFFIX = ".dat";

    private final BuildersWand plugin;
    private final WandStorageSerializer serializer = new WandStorageSerializer();
    private final Path storageFolder;
    private final ConcurrentHashMap<String, WandStorage> storage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> dirtyMap = new ConcurrentHashMap<>();
    private BukkitTask autosaveTask;
    private volatile boolean saving = false;
    private volatile boolean shuttingDown = false;

    public WandStorageManager(BuildersWand plugin) {
        this.plugin = plugin;
        this.storageFolder = plugin.getDataFolder().toPath().resolve("storage");
    }

    public void init() {
        try {
            Files.createDirectories(storageFolder);
        }
        catch (IOException exception) {
            ComponentUtil.error("Failed to create wand storage folder: " + exception.getMessage());
        }
        startAutosave();
    }

    public void shutdown() {
        shuttingDown = true;
        stopAutosave();
        waitForSaveCompletion();
        flushDirty();
    }

    private WandStorage loadOrCreate(String wandUUID, String wandTypeId) {
        Path file = storageFolder.resolve(wandUUID + FILE_SUFFIX);
        if (Files.exists(file)) {
            try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
                String storedTypeId = in.readUTF();
                int length = in.readInt();
                byte[] blob = in.readNBytes(length);
                WandStorage wandStorage = new WandStorage(storedTypeId, wandUUID);
                serializer.deserialize(blob).forEach(wandStorage::setItem);
                return wandStorage;
            }
            catch (IOException exception) {
                ComponentUtil.error("Failed to read wand storage file " + file.getFileName() + ": " + exception.getMessage());
            }
        }
        return new WandStorage(wandTypeId, wandUUID);
    }

    public synchronized WandStorage getStorage(Wand wand, ItemStack wandItem) {
        if (wand == null || wandItem == null) return null;
        String wandUUID = wandItem.getItemMeta().getPersistentDataContainer().get(BuildersWand.PDC_KEY_UUID, PersistentDataType.STRING);
        if (wandUUID == null) return null;
        return storage.computeIfAbsent(wandUUID, key -> loadOrCreate(key, wand.getId()));
    }

    public synchronized Optional<WandStorage> findByUUID(String wandUUID) {
        WandStorage cached = storage.get(wandUUID);
        if (cached != null) return Optional.of(cached);
        Path file = storageFolder.resolve(wandUUID + FILE_SUFFIX);
        if (!Files.exists(file)) return Optional.empty();
        WandStorage loaded = loadOrCreate(wandUUID, null);
        storage.put(wandUUID, loaded);
        return Optional.of(loaded);
    }

    public void save(String wandUUID) {
        if (shuttingDown) return;
        dirtyMap.put(wandUUID, true);
    }

    public void saveNow(String wandUUID) {
        if (shuttingDown) return;
        WandStorage wandStorage = storage.get(wandUUID);
        if (wandStorage == null) return;
        if (writeFile(wandStorage)) {
            dirtyMap.remove(wandUUID);
        }
    }

    private void flushDirty() {
        if (saving || dirtyMap.isEmpty()) return;
        saving = true;
        try {
            for (String wandUUID : dirtyMap.keySet()) {
                WandStorage wandStorage = storage.get(wandUUID);
                if (wandStorage == null) continue;
                writeFile(wandStorage);
            }
            dirtyMap.clear();
        }
        finally {
            saving = false;
        }
    }

    private boolean writeFile(WandStorage wandStorage) {
        Path target = storageFolder.resolve(wandStorage.getWandItemUUID() + FILE_SUFFIX);
        Path temp = storageFolder.resolve(wandStorage.getWandItemUUID() + FILE_SUFFIX + ".tmp");
        byte[] contentBlob = serializer.serialize(wandStorage.getContent());
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(temp))) {
            out.writeUTF(wandStorage.getWandTypeId());
            out.writeInt(contentBlob.length);
            out.write(contentBlob);
        }
        catch (IOException exception) {
            ComponentUtil.error("Failed to save wand storage: " + exception.getMessage());
            return false;
        }
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException exception) {
            ComponentUtil.error("Failed to finalize wand storage save: " + exception.getMessage());
            return false;
        }
        return true;
    }

    private void waitForSaveCompletion() {
        while (saving) {
            try {
                Thread.sleep(5);
            }
            catch (InterruptedException ignored) {
            }
        }
    }

    public void startAutosave() {
        if (!ConfigManager.isWandStorageAutosaveEnabled()) return;
        long interval = ConfigManager.getWandStorageAutosaveIntervalSeconds() * 20L;
        autosaveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::flushDirty, interval, interval);
    }

    public void stopAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    public record PruneResult(int deletedEmpty, List<String> staleNonEmpty) {}

    public PruneResult pruneStale(long staleAfterMillis) {
        int deletedEmpty = 0;
        List<String> staleNonEmpty = new ArrayList<>();
        if (!Files.isDirectory(storageFolder)) return new PruneResult(0, staleNonEmpty);
        long cutoff = System.currentTimeMillis() - staleAfterMillis;
        try (Stream<Path> files = Files.list(storageFolder)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(FILE_SUFFIX)).toList()) {
                try {
                    if (Files.getLastModifiedTime(file).toMillis() > cutoff) continue;
                    String fileName = file.getFileName().toString();
                    String wandUUID = fileName.substring(0, fileName.length() - FILE_SUFFIX.length());
                    if (isEmptyStorageFile(file)) {
                        Files.delete(file);
                        storage.remove(wandUUID);
                        deletedEmpty++;
                    }
                    else {
                        staleNonEmpty.add(wandUUID);
                    }
                }
                catch (IOException exception) {
                    ComponentUtil.error("Failed to inspect wand storage file " + file.getFileName() + ": " + exception.getMessage());
                }
            }
        }
        catch (IOException exception) {
            ComponentUtil.error("Failed to list wand storage folder: " + exception.getMessage());
        }
        return new PruneResult(deletedEmpty, staleNonEmpty);
    }

    private boolean isEmptyStorageFile(Path file) throws IOException {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            in.readUTF();
            int length = in.readInt();
            byte[] blob = in.readNBytes(length);
            return serializer.deserialize(blob).isEmpty();
        }
    }

    public record StorageSummary(int totalFiles, int emptyFiles, long totalBytes, long oldestModifiedMillis, long newestModifiedMillis) {}

    public StorageSummary summarize() {
        int total = 0;
        int empty = 0;
        long bytes = 0;
        long oldest = Long.MAX_VALUE;
        long newest = Long.MIN_VALUE;
        if (!Files.isDirectory(storageFolder)) return new StorageSummary(0, 0, 0, 0, 0);
        try (Stream<Path> files = Files.list(storageFolder)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(FILE_SUFFIX)).toList()) {
                try {
                    total++;
                    bytes += Files.size(file);
                    long modified = Files.getLastModifiedTime(file).toMillis();
                    oldest = Math.min(oldest, modified);
                    newest = Math.max(newest, modified);
                    if (isEmptyStorageFile(file)) empty++;
                }
                catch (IOException exception) {
                    ComponentUtil.error("Failed to inspect wand storage file " + file.getFileName() + ": " + exception.getMessage());
                }
            }
        }
        catch (IOException exception) {
            ComponentUtil.error("Failed to list wand storage folder: " + exception.getMessage());
        }
        if (total == 0) {
            oldest = 0;
            newest = 0;
        }
        return new StorageSummary(total, empty, bytes, oldest, newest);
    }
}
