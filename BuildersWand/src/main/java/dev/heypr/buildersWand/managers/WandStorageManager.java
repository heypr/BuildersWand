package dev.heypr.buildersWand.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.managers.io.ConfigManager;
import dev.heypr.buildersWand.utility.ComponentUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WandStorageManager {

    private final BuildersWand plugin;
    private final WandStorageSerializer serializer = new WandStorageSerializer();
    private final ConcurrentHashMap<String, WandStorage> storage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> dirtyMap = new ConcurrentHashMap<>();
    private final HikariDataSource dataSource;
    private BukkitTask autosaveTask;
    private volatile boolean saving = false;
    private volatile boolean shuttingDown = false;

    public WandStorageManager(BuildersWand plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + new File(dataFolder, "data.db").getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.addDataSourceProperty("journal_mode", "WAL");
        this.dataSource = new HikariDataSource(config);
    }

    public void init() {
        createTables();
        load();
        startAutosave();
    }

    public void shutdown() {
        shuttingDown = true;
        stopAutosave();
        waitForSaveCompletion();
        flushDirtySync();
        dataSource.close();
    }

    public void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS wand_storage (wand_id TEXT PRIMARY KEY, content TEXT)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        } catch (SQLException e) {
            ComponentUtil.error("Failed to create tables: " + e.getMessage());
        }
    }

    public void load() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT wand_id, content FROM wand_storage");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String wandId = rs.getString("wand_id");
                String json = rs.getString("content");
                Wand wand = WandManager.getWandConfig(wandId);
                if (wand != null) {
                    WandStorage ws = storage.computeIfAbsent(wandId, k -> new WandStorage(wand));
                    Map<Integer, ItemStack> items = serializer.deserializeMap(json);
                    items.forEach(ws::setItem);
                }
            }
        }
        catch (SQLException e) {
            ComponentUtil.error("Failed to load storage: " + e.getMessage());
        }
    }

    public synchronized WandStorage getStorage(Wand wand) {
        return wand == null ? null : storage.computeIfAbsent(wand.getId(), k -> new WandStorage(wand));
    }

    public void save(Wand wand) {
        if (shuttingDown) return;
        dirtyMap.put(wand.getId(), true);
    }

    private void flushDirty() {
        if (saving || dirtyMap.isEmpty()) return;

        saving = true;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT OR REPLACE INTO wand_storage (wand_id, content) VALUES (?, ?)")) {

            for (String wandId : dirtyMap.keySet()) {
                WandStorage ws = storage.get(wandId);
                if (ws == null) continue;

                String json = serializer.serializeMap(ws.getAllContent());

                stmt.setString(1, wandId);
                stmt.setString(2, json);
                stmt.addBatch();
            }

            stmt.executeBatch();
            dirtyMap.clear();

        }
        catch (SQLException e) {
            ComponentUtil.error("Failed to flush dirty storage: " + e.getMessage());
        }
        finally {
            saving = false;
        }
    }

    private void flushDirtySync() {
        if (dirtyMap.isEmpty()) return;

        saving = true;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT OR REPLACE INTO wand_storage (wand_id, content) VALUES (?, ?)")) {

            for (String wandId : dirtyMap.keySet()) {
                WandStorage ws = storage.get(wandId);
                if (ws == null) continue;

                String json = serializer.serializeMap(ws.getAllContent());

                stmt.setString(1, wandId);
                stmt.setString(2, json);
                stmt.addBatch();
            }

            stmt.executeBatch();
            dirtyMap.clear();

        } catch (SQLException e) {
            ComponentUtil.error("Failed to flush dirty storage (sync): " + e.getMessage());
        } finally {
            saving = false;
        }
    }

    private void waitForSaveCompletion() {
        while (saving) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void startAutosave() {
        if (!ConfigManager.isWandStorageAutosaveEnabled()) return;

        long interval = ConfigManager.getWandStorageAutosaveIntervalSeconds() * 20L;

        autosaveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::flushDirty,
                interval,
                interval
        );
    }

    public void stopAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    public Collection<WandStorage> getAllStorages() {
        return storage.values();
    }
}
