package dev.heypr.buildersWand.managers.io;

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
    private static final String CURRENT_VERSION = "1.5.0";
    private static final Map<String, Wand> wandConfigs = new HashMap<>();
    private static boolean placementQueueEnabled;
    private static boolean fireWandBlockPlaceEvent;
    private static boolean fireWandPreviewEvent;
    private static boolean debugModeEnabled;
    private static boolean destroyInvalidWands;
    private static boolean updaterEnabled;
    private static boolean updaterNotifyConsole;
    private static boolean updaterNotifyInGame;
    private static int maxBlocksPerTick;
    private static long updaterIntervalMinutes;

    public static void load() {
        BuildersWand plugin = BuildersWand.getInstance();
        plugin.saveDefaultConfig();
        String fileVersion = plugin.getConfig().getString("config-version", "unknown");
        if (!fileVersion.equals(CURRENT_VERSION)) {
            Util.error("OUTDATED config.yml: Expected '" + CURRENT_VERSION + "' but found '" + fileVersion + "'.");
            Util.error("Please update your config.yml to the latest version. A default config.yml can be found on the plugin page and on GitHub. If you need help, please get in touch via the support Discord.");
        }
        Util.debug("Starting ConfigManager load sequence...");
        if (BuildersWand.getRecipeManager() != null) {
            BuildersWand.getRecipeManager().unregisterRecipes();
        }
        wandConfigs.clear();
        loadWandConfigs();
        BuildersWand.getRecipeManager().registerRecipes();
        FileConfiguration config = plugin.getConfig();
        placementQueueEnabled = config.getBoolean("placementQueue.enabled", true);
        fireWandBlockPlaceEvent = config.getBoolean("fireWandBlockPlaceEvent", true);
        fireWandPreviewEvent = config.getBoolean("fireWandPreviewEvent", true);
        maxBlocksPerTick = config.getInt("placementQueue.maxBlocksPerTick", 20);
        destroyInvalidWands = config.getBoolean("destroyInvalidWands.enabled", false);
        debugModeEnabled = config.getBoolean("debug", false);
        updaterEnabled = config.getBoolean("updater.enabled", true);
        updaterIntervalMinutes = config.getLong("updater.checkIntervalMinutes", 60L);
        updaterNotifyConsole = config.getBoolean("updater.notify.console", true);
        updaterNotifyInGame = config.getBoolean("updater.notify.ingame", true);
        Util.PREFIX = MessageManager.getRegularMessage(MessageManager.Messages.PREFIX);
    }

    public static List<Wand> loadWandConfigs() {
        wandConfigs.clear();
        List<Wand> wandList = new ArrayList<>();
        FileConfiguration config = BuildersWand.getInstance().getConfig();
        ConfigurationSection wandsSection = config.getConfigurationSection("wands");
        if (wandsSection == null) {
            Util.debug("Critical: 'wands' section is missing from config.yml!");
            return wandList;
        }
        for (String wandId : wandsSection.getKeys(false)) {
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
                Sound breakSound;
                try {
                    breakSound = Sound.valueOf(breakSoundName);
                }
                catch (Exception e) {
                    Util.debug("Invalid break sound for wand '" + wandId + "': " + breakSoundName + ". Defaulting to ENTITY_ITEM_BREAK.");
                    breakSound = Sound.ENTITY_ITEM_BREAK;
                }
                String breakSoundMessage = config.getString(path + "durability.breakSound.message", "&cYour wand broke!");
                String previewParticle = config.getString(path + "previewParticle.particle");
                int previewParticleCount = config.getInt(path + "previewParticle.count", 1);
                double pOffsetX = config.getDouble(path + "previewParticle.offset.x", 0);
                double pOffsetY = config.getDouble(path + "previewParticle.offset.y", 0);
                double pOffsetZ = config.getDouble(path + "previewParticle.offset.z", 0);
                double pSpeed = config.getDouble(path + "previewParticle.speed", 0);
                int pRed = config.getInt(path + "previewParticle.options.color.red", 0);
                int pGreen = config.getInt(path + "previewParticle.options.color.green", 0);
                int pBlue = config.getInt(path + "previewParticle.options.color.blue", 0);
                int pSize = config.getInt(path + "previewParticle.options.size", 1);
                float cooldown = (float) config.getDouble(path + "cooldown", 0);
                int undoHistorySize = config.getInt(path + "undoHistorySize", 10);
                List<Material> blockedMaterials = new ArrayList<>();
                for (String mat : config.getStringList(path + "blockedMaterials")) {
                    try {
                        blockedMaterials.add(Material.valueOf(mat));
                    }
                    catch (Exception ignored) {}
                }
                boolean isCraftable = config.getBoolean(path + "craftable", false);
                boolean craftingRecipeEnabled = config.getBoolean(path + "craftingRecipe.enabled", false);
                List<String> recipeShape = config.getStringList(path + "craftingRecipe.shape");
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
                Wand wand = new Wand(wandId, wandName, wandMaterial, wandLore, wandType, staticLength,
                        staticWidth, maxSize, maxSizeText, maxRayTraceDistance, consumeItems, generatePreviewOnMove,
                        durabilityAmount, durabilityEnabled, durabilityText, breakSoundEnabled, breakSound, breakSoundMessage,
                        previewParticle, previewParticleCount, pOffsetX, pOffsetY, pOffsetZ, pSpeed,
                        pRed, pGreen, pBlue, pSize, cooldown, blockedMaterials,
                        isCraftable, craftingRecipeEnabled, recipeShape, recipeIngredients, undoHistorySize);
                wandConfigs.put(wandId, wand);
                wandList.add(wand);
            }
            catch (Exception e) {
                Util.error("Failed to load wand: " + wandId);
            }
        }
        return wandList;
    }

    public static void reload() {
        BuildersWand plugin = BuildersWand.getInstance();
        plugin.reloadConfig();
        load();
        BuildersWand.getWandManager().registerWands();
        Updater.start(plugin);
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
}
