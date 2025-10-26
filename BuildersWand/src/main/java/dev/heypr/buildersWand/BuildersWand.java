package dev.heypr.buildersWand;

import dev.heypr.buildersWand.api.BuildersWandAPI;
import dev.heypr.buildersWand.commands.GiveWandCommand;
import dev.heypr.buildersWand.commands.ReloadWandCommand;
import dev.heypr.buildersWand.impl.ApiImplementation;
import dev.heypr.buildersWand.listeners.WandListener;
import dev.heypr.buildersWand.managers.ConfigManager;
import dev.heypr.buildersWand.managers.RecipeManager;
import dev.heypr.buildersWand.managers.WandManager;
import dev.heypr.buildersWand.metrics.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class BuildersWand extends JavaPlugin {

    private static final WandManager wandManager = new WandManager();
    private static RecipeManager recipeManager;

    private static BuildersWand instance;
    public static NamespacedKey PDC_KEY_ID;
    public static NamespacedKey PDC_KEY_DURABILITY;
    public static NamespacedKey PDC_KEY_MAX_SIZE;
    public static NamespacedKey PDC_KEY_UUID;

    @Override
    public void onEnable() {
        instance = this;
        PDC_KEY_ID = new NamespacedKey(this, "builders_wand_id");
        PDC_KEY_DURABILITY = new NamespacedKey(this, "builders_wand_durability");
        PDC_KEY_UUID = new NamespacedKey(this, "builders_wand_uuid");
        PDC_KEY_MAX_SIZE = new NamespacedKey(this, "builders_wand_max_size");
        recipeManager = new RecipeManager(this);

        saveDefaultConfig();
        ConfigManager.load();
        BuildersWandAPI.setInstance(new ApiImplementation(this));
        BuildersWand.getWandManager().registerWands();
        Bukkit.getPluginManager().registerEvents(new WandListener(), this);
        getCommand("reloadbuilderswand").setExecutor(new ReloadWandCommand());
        getCommand("givewand").setExecutor(new GiveWandCommand());
        new Metrics(this, 27729);
        getLogger().info("BuildersWand enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BuildersWand disabled.");
    }

    public static BuildersWand getInstance() {
        return instance;
    }

    public static WandManager getWandManager() {
        return wandManager;
    }

    public static RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public static boolean isSkyblockEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2");
    }

    public static boolean isWorldGuardEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    public static boolean isLandsEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("Lands");
    }
}
