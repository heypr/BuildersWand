package dev.heypr.buildersWand;

import dev.heypr.buildersWand.api.BuildersWandAPI;
import dev.heypr.buildersWand.commands.BuildersWandCommand;
import dev.heypr.buildersWand.impl.ApiImplementation;
import dev.heypr.buildersWand.listeners.CraftListener;
import dev.heypr.buildersWand.listeners.FurnaceListener;
import dev.heypr.buildersWand.listeners.WandStorageListener;
import dev.heypr.buildersWand.listeners.WandUseListener;
import dev.heypr.buildersWand.managers.RecipeManager;
import dev.heypr.buildersWand.managers.WandManager;
import dev.heypr.buildersWand.managers.io.ConfigManager;
import dev.heypr.buildersWand.managers.io.MessageManager;
import dev.heypr.buildersWand.metrics.Metrics;
import dev.heypr.buildersWand.utility.ComponentUtil;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
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
    @SuppressWarnings("UnstableApiUsage")
    public void onEnable() {
        instance = this;
        PDC_KEY_ID = new NamespacedKey(instance, "builders_wand_id");
        PDC_KEY_DURABILITY = new NamespacedKey(instance, "builders_wand_durability");
        PDC_KEY_UUID = new NamespacedKey(instance, "builders_wand_uuid");
        PDC_KEY_MAX_SIZE = new NamespacedKey(instance, "builders_wand_max_size");
        recipeManager = new RecipeManager(instance);
        BuildersWand.getWandManager().registerWands();
        MessageManager.initialize();
        ConfigManager.load();
        registerEvents();
        BuildersWandAPI.setInstance(new ApiImplementation(instance));
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            new BuildersWandCommand().register(commands);
        });
        new Metrics(instance, 27729);
        Updater.start(instance);
        ComponentUtil.log("BuildersWand enabled!");
    }

    @Override
    public void onDisable() {
        Updater.stop();
        ComponentUtil.log("BuildersWand disabled.");
    }

    private void registerEvents() {
        register(new WandUseListener());
        register(new CraftListener());
        register(new FurnaceListener());
        register(new WandStorageListener());
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
