package dev.heypr.buildersWand;

import dev.heypr.buildersWand.api.BuildersWandAPI;
import dev.heypr.buildersWand.commands.BuildersWandCommand;
import dev.heypr.buildersWand.gui.WandStorageGuiListener;
import dev.heypr.buildersWand.impl.ApiImplementation;
import dev.heypr.buildersWand.listeners.CraftListener;
import dev.heypr.buildersWand.listeners.FurnaceListener;
import dev.heypr.buildersWand.listeners.WandUseListener;
import dev.heypr.buildersWand.managers.RecipeManager;
import dev.heypr.buildersWand.managers.WandManager;
import dev.heypr.buildersWand.managers.WandStorageManager;
import dev.heypr.buildersWand.managers.io.ConfigManager;
import dev.heypr.buildersWand.managers.io.MessageManager;
import dev.heypr.buildersWand.metrics.Metrics;
import dev.heypr.buildersWand.utility.ComponentUtil;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public class BuildersWand extends JavaPlugin {
    private static final WandManager wandManager = new WandManager();
    private static RecipeManager recipeManager;
    private static WandStorageManager storageManager;
    private static BuildersWand instance;
    private static boolean wandStorageAvailable = true;
    public static NamespacedKey PDC_KEY_ID;
    public static NamespacedKey PDC_KEY_DURABILITY;
    public static NamespacedKey PDC_KEY_MAX_SIZE;
    public static NamespacedKey PDC_KEY_UUID;

    @Override
    public void onEnable() {
        instance = this;
        PDC_KEY_ID = new NamespacedKey(instance, "builders_wand_id");
        PDC_KEY_DURABILITY = new NamespacedKey(instance, "builders_wand_durability");
        PDC_KEY_UUID = new NamespacedKey(instance, "builders_wand_uuid");
        PDC_KEY_MAX_SIZE = new NamespacedKey(instance, "builders_wand_max_size");
        recipeManager = new RecipeManager(instance);
        wandManager.registerWands();
        MessageManager.initialize();
        ConfigManager.load();
        if (!ConfigManager.isWandStorageEnabled()) {
            wandStorageAvailable = false;
        }
        if (wandStorageAvailable) {
            storageManager = new WandStorageManager(instance);
            storageManager.init();
            ComponentUtil.debug("Wand storage initialized.");
        }
        register(new WandUseListener());
        register(new CraftListener());
        register(new FurnaceListener());
        register(new WandStorageGuiListener());
        BuildersWandAPI.setInstance(new ApiImplementation(instance));
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            new BuildersWandCommand().register(commands);
        });
        new Metrics(instance, 27729);
        Updater.start(instance);
        ConfigManager.reload();
        ComponentUtil.log("BuildersWand enabled!");
    }

    @Override
    public void onDisable() {
        Updater.stop();
        if (storageManager != null) {
            storageManager.shutdown();
        }
        ComponentUtil.log("BuildersWand disabled.");
    }

    private void register(Listener listener) {
        instance.getServer().getPluginManager().registerEvents(listener, instance);
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

    public static WandStorageManager getStorageManager() {
        return storageManager;
    }

    public static boolean isWandStorageAvailable() {
        return wandStorageAvailable;
    }

    public static boolean isSuperiorSkyblockEnabled() {
        return instance.getServer().getPluginManager().isPluginEnabled("SuperiorSkyblock2");
    }

    public static boolean isBentoBoxEnabled() {
        return instance.getServer().getPluginManager().isPluginEnabled("BentoBox");
    }

    public static boolean isWorldGuardEnabled() {
        return instance.getServer().getPluginManager().isPluginEnabled("WorldGuard");
    }

    public static boolean isLandsEnabled() {
        return instance.getServer().getPluginManager().isPluginEnabled("Lands");
    }
}
