package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.Updater;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.utility.Util;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private static final Map<String, Wand> wandConfigs = new HashMap<>();
    private static boolean placementQueueEnabled;
    private static boolean fireWandBlockPlaceEvent;
    private static boolean fireWandPreviewEvent;
    private static boolean debugModeEnabled;
    private static boolean destroyInvalidWands;
    private static boolean updaterEnabled;
    private static boolean updaterNotifyConsole;
    private static boolean updaterNotifyInGame;
    private static String invalidWandMessage;
    private static String updaterNotifyMessage;
    private static String updaterNotifyPermission;
    private static int maxBlocksPerTick;
    private static long updaterIntervalMinutes;

    public static void load() {
        Util.debug("Starting ConfigManager load sequence...");
        BuildersWand plugin = BuildersWand.getInstance();
        plugin.saveDefaultConfig();

        if (BuildersWand.getRecipeManager() != null) {
            Util.debug("Unregistering existing recipes...");
            BuildersWand.getRecipeManager().unregisterRecipes();
        }

        wandConfigs.clear();
        loadWandConfigs();

        Util.debug("Registering new recipes based on loaded configs...");
        BuildersWand.getRecipeManager().registerRecipes();

        FileConfiguration config = plugin.getConfig();
        placementQueueEnabled = config.getBoolean("placementQueue.enabled", true);
        fireWandBlockPlaceEvent = config.getBoolean("fireWandBlockPlaceEvent", true);
        fireWandPreviewEvent = config.getBoolean("fireWandPreviewEvent", true);
        maxBlocksPerTick = config.getInt("placementQueue.maxBlocksPerTick", 20);

        destroyInvalidWands = config.getBoolean("destroyInvalidWands.enabled", false);
        invalidWandMessage = config.getString("destroyInvalidWands.message", "&cYour wand has been removed because it is no longer valid. Please contact an administrator if you believe this is an error.");

        debugModeEnabled = config.getBoolean("debug", false);

        updaterEnabled = config.getBoolean("updater.enabled", true);
        updaterIntervalMinutes = config.getLong("updater.checkIntervalMinutes", 60L);
        updaterNotifyConsole = config.getBoolean("updater.notify.console", true);
        updaterNotifyInGame = config.getBoolean("updater.notify.ingame", true);
        updaterNotifyMessage = config.getString("updater.notifyMessage", "&aAn update for BuildersWand is available! Check console for more info.");
        updaterNotifyPermission = config.getString("updater.notifyPermission", "builderswand.notify.update");

        Util.debug("Queue Enabled: " + placementQueueEnabled);
        Util.debug("Max Blocks Per Tick: " + maxBlocksPerTick);
        Util.debug("Fire Block Place Event: " + fireWandBlockPlaceEvent);
        Util.debug("Fire Preview Event: " + fireWandPreviewEvent);
        Util.debug("Destroy Invalid Wands: " + destroyInvalidWands);
        Util.debug("Invalid Wand Message: " + invalidWandMessage);
        Util.debug("Debug Mode: " + debugModeEnabled);

        String prefix = config.getString("prefix", "&7[&bBuildersWand&7] &r");
        Util.PREFIX = Util.toComponent(prefix);
        Util.debug("Prefix loaded and converted to component.");
    }

    public static List<Wand> loadWandConfigs() {
        Util.debug("Parsing wands section in config...");
        wandConfigs.clear();
        List<Wand> wandList = new ArrayList<>();
        FileConfiguration config = BuildersWand.getInstance().getConfig();
        ConfigurationSection wandsSection = config.getConfigurationSection("wands");

        if (wandsSection == null) {
            Util.debug("Critical: 'wands' section is missing from config.yml!");
            return wandList;
        }

        for (String wandId : wandsSection.getKeys(false)) {
            Util.debug("Attempting to load wand key: " + wandId);
            try {
                String path = "wands." + wandId + ".";

                String wandName = config.getString(path + "name", "&3Builders Wand");
                Material wandMaterial = Material.valueOf(config.getString(path + "material", "BLAZE_ROD"));
                List<String> wandLore = config.getStringList(path + "lore");

                Wand.WandType wandType = Wand.WandType.valueOf(config.getString(path + "wandType", "STANDARD"));
                int staticLength = config.getInt(path + "staticLength", 3);
                int staticWidth = config.getInt(path + "staticWidth", 3);

                int maxSize = config.getInt(path + "maxSize", 8);
                String maxSizeText = config.getString(path + "maxSizeText", "&3Max Size: {maxSize}");
                int maxRayTraceDistance = config.getInt(path + "maxRayTraceDistance", 16);

                boolean consumeItems = config.getBoolean(path + "consumeItems", true);
                boolean generatePreviewOnMove = config.getBoolean(path + "generatePreviewOnMove", false);

                int durabilityAmount = config.getInt(path + "durability.amount", 100);
                boolean durabilityEnabled = config.getBoolean(path + "durability.enabled", true);
                String durabilityText = config.getString(path + "durability.text", "&3Durability: {durability}");

                boolean breakSoundEnabled = config.getBoolean(path + "durability.breakSound.enabled", false);
                String breakSoundName = config.getString(path + "durability.breakSound.sound", "ENTITY_ITEM_BREAK");
                Sound breakSound = Sound.ENTITY_ITEM_BREAK;
                try {
                    breakSound = Sound.valueOf(breakSoundName);
                }
                catch (Exception e) {
                    Util.debug("Invalid break sound for wand " + wandId + ": " + breakSound + ". Defaulting to ENTITY_ITEM_BREAK.");
                }
                String breakSoundMessage = config.getString(path + "durability.breakSound.message", "&cYour wand broke!");

                String previewParticle = config.getString(path + "previewParticle.particle");
                int previewParticleCount = config.getInt(path + "previewParticle.count", 1);
                double pOffsetX = config.getDouble(path + "previewParticle.offset.x", 0);
                double pOffsetY = config.getDouble(path + "previewParticle.offset.y", 0);
                double pOffsetZ = config.getDouble(path + "previewParticle.offset.z", 0);
                double pSpeed = config.getDouble(path + "previewParticle.speed", 0);

                int pRed = config.getInt(path + "previewParticle.options.red", 0);
                int pGreen = config.getInt(path + "previewParticle.options.green", 0);
                int pBlue = config.getInt(path + "previewParticle.options.blue", 0);
                int pSize = config.getInt(path + "previewParticle.options.size", 1);

                float cooldown = (float) config.getDouble(path + "cooldown", 0);
                int undoHistorySize = config.getInt(path + "undoHistorySize", 10);

                Util.debug("Loading blocked materials for wand " + wandId + "...");
                List<Material> blockedMaterials = new ArrayList<>();
                for (String mat : config.getStringList(path + "blockedMaterials")) {
                    try {
                        blockedMaterials.add(Material.valueOf(mat));
                    }
                    catch (IllegalArgumentException e) {
                        Util.debug("Invalid blocked material in wand " + wandId + ": " + mat);
                    }
                }

                boolean isCraftable = config.getBoolean(path + "craftable", false);
                boolean craftingRecipeEnabled = config.getBoolean(path + "craftingRecipe.enabled", false);
                List<String> recipeShape = new ArrayList<>();
                Map<Character, Material> recipeIngredients = new HashMap<>();

                if (craftingRecipeEnabled) {
                    Util.debug("Loading recipe for wand " + wandId + "...");
                    recipeShape = config.getStringList(path + "craftingRecipe.shape");
                    if (recipeShape.isEmpty() || recipeShape.size() > 3 || recipeShape.stream().anyMatch(row -> row.length() > 3)) {
                        Util.error("Wand " + wandId + " has an invalid recipe shape. Disabling crafting.");
                        craftingRecipeEnabled = false;
                    }
                    else {
                        ConfigurationSection ingredientsSection = config.getConfigurationSection(path + "craftingRecipe.ingredients");
                        if (ingredientsSection == null) {
                            Util.error("Wand " + wandId + " has no ingredients defined. Disabling crafting.");
                            craftingRecipeEnabled = false;
                        }
                        else {
                            for (String key : ingredientsSection.getKeys(false)) {
                                char ingredientChar = key.charAt(0);
                                String materialName = ingredientsSection.getString(key);
                                try {
                                    if (materialName != null) {
                                        Material mat = Material.valueOf(materialName.toUpperCase());
                                        recipeIngredients.put(ingredientChar, mat);
                                        Util.debug("Registered ingredient: " + ingredientChar + " -> " + mat.name());
                                    }
                                }
                                catch (IllegalArgumentException e) {
                                    Util.error("Wand " + wandId + " invalid ingredient: " + materialName);
                                    craftingRecipeEnabled = false;
                                    break;
                                }
                            }
                        }
                    }
                }

                Wand wand = new Wand(wandId, wandName, wandMaterial, wandLore,
                        wandType, staticLength, staticWidth, maxSize, maxSizeText,
                        maxRayTraceDistance, consumeItems, generatePreviewOnMove, durabilityAmount,
                        durabilityEnabled, durabilityText, breakSoundEnabled, breakSound, breakSoundMessage,
                        previewParticle, previewParticleCount, pOffsetX, pOffsetY, pOffsetZ,
                        pSpeed, pRed, pGreen, pBlue, pSize, cooldown, blockedMaterials,
                        isCraftable, craftingRecipeEnabled, recipeShape, recipeIngredients, undoHistorySize);

                wandConfigs.put(wandId, wand);
                wandList.add(wand);
                Util.debug("Wand " + wandId + " successfully instantiated and added to cache.");

            }
            catch (Exception e) {
                Util.error("Error loading wand configuration for key: " + wandId + ". Skipping this wand.");
            }
        }
        Util.debug("Finished loading " + wandList.size() + " wands.");
        return wandList;
    }

    public static void reload() {
        BuildersWand plugin = BuildersWand.getInstance();
        plugin.reloadConfig();
        load();
        WandManager manager = BuildersWand.getWandManager();
        manager.registerWands();
        Updater.start(plugin);
        Util.debug("BuildersWand configuration reloaded.");
    }

    public static List<Wand> getAllWands() {
        return new ArrayList<>(wandConfigs.values());
    }

    public static boolean isPlacementQueueEnabled() {
        return placementQueueEnabled;
    }

    public static boolean shouldFireBlockPlaceEvent() {
        return fireWandBlockPlaceEvent;
    }

    public static boolean shouldFireWandPreviewEvent() {
        return fireWandPreviewEvent;
    }

    public static int getMaxBlocksPerTick() {
        return maxBlocksPerTick;
    }

    public static boolean shouldDestroyInvalidWands() {
        return destroyInvalidWands;
    }

    public static String getInvalidWandMessage() {
        return invalidWandMessage;
    }

    public static boolean getDebugMode() {
        return debugModeEnabled;
    }

    public static boolean isUpdaterEnabled() {
        return updaterEnabled;
    }

    public static long getUpdaterIntervalMinutes() {
        return updaterIntervalMinutes;
    }

    public static boolean notifyUpdateInConsole() {
        return updaterNotifyConsole;
    }

    public static boolean notifyUpdateInGame() {
        return updaterNotifyInGame;
    }

    public static String getUpdaterNotifyMessage() {
        return updaterNotifyMessage;
    }

    public static String getUpdaterNotifyPermission() {
        return updaterNotifyPermission;
    }
}
