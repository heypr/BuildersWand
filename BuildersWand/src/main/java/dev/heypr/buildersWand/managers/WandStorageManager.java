package dev.heypr.buildersWand.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.heypr.buildersWand.BuildersWand;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class WandStorageManager {

    private BuildersWand plugin;
    private ConcurrentHashMap<Player, WandStorage> storage = new ConcurrentHashMap<>();
    private final HikariDataSource dataSource;

    public WandStorageManager(BuildersWand plugin) {
        this.plugin = plugin;
        File dataFolder = BuildersWand.getInstance().getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, "data.db");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setPoolName("WandStoragePool");
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");

        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");

        this.dataSource = new HikariDataSource(config);
    }

    public void createTables() {
        // create tables for sql storage if it doesn't exist.
    }

    public void load() {
        // implement sql storage, loading from db to storage map in memory
    }

    public synchronized WandStorage getStorage(Player player) {
        return storage.get(player);
    }

    public void save() {
        // implement sql storage, saving storage map in memory to db
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
