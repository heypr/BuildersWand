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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WandStorageManager {

    private final BuildersWand plugin;
    private final WandStorageSerializer serializer = new WandStorageSerializer();
    private final ConcurrentHashMap<String, WandStorage> storage = new ConcurrentHashMap<>();
    private final HikariDataSource dataSource;
    private BukkitTask autosaveTask;

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

    public void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS wand_storage (wand_id TEXT PRIMARY KEY, content TEXT)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
        catch (SQLException e) {
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

    public void save() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO wand_storage (wand_id, content) VALUES (?, ?)")) {
            for (Map.Entry<String, WandStorage> entry : storage.entrySet()) {
                String json = serializer.serializeMap(entry.getValue().getAllContent());
                stmt.setString(1, entry.getKey());
                stmt.setString(2, json);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
        catch (SQLException e) {
            ComponentUtil.error("Failed to save storage: " + e.getMessage());
        }
    }

    public void startAutosave() {
        if (!ConfigManager.isWandStorageAutosaveEnabled()) return;
        long interval = ConfigManager.getWandStorageAutosaveIntervalSeconds() * 20L;
        autosaveTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::save, interval, interval);
    }

    public void stopAutosave() {
        if (autosaveTask != null) autosaveTask.cancel();
    }

    public void close() {
        stopAutosave();
        save();
        if (dataSource != null) dataSource.close();
    }

    public Collection<WandStorage> getAllStorages() {
        return storage.values();
    }
}
