package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.Util;
import dev.heypr.buildersWand.api.Wand;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private static final Map<Integer, Wand> wandConfigs = new HashMap<>();
    private static boolean placementQueueEnabled;
    private static boolean fireWandBlockPlaceEvent;
    private static boolean fireWandPreviewEvent;
    private static int maxBlocksPerTick;

    public static void load() {
        BuildersWand plugin = BuildersWand.getInstance();
        plugin.saveDefaultConfig();
        wandConfigs.clear();
        loadWandConfigs();
        BuildersWand.getRecipeManager().unregisterRecipes();
        BuildersWand.getRecipeManager().registerRecipes();
        FileConfiguration config = plugin.getConfig();
        placementQueueEnabled = config.getBoolean("placementQueue.enabled", true);
        fireWandBlockPlaceEvent = config.getBoolean("fireWandBlockPlaceEvent", true);
        fireWandPreviewEvent = config.getBoolean("fireWandPreviewEvent", true);
        maxBlocksPerTick = config.getInt("placementQueue.maxBlocksPerTick", 20);
        Util.PREFIX = Util.toComponent(config.getString("prefix", "&7[&bBuildersWand&7] &r"));
    }

    public static List<Wand> loadWandConfigs() {
        wandConfigs.clear();
        List<Wand> wandList = new ArrayList<>();
        FileConfiguration config = BuildersWand.getInstance().getConfig();
        ConfigurationSection wandsSection = config.getConfigurationSection("wands");
        if (wandsSection == null) return null;

        for (String wandKey : wandsSection.getKeys(false)) {
            int wandId = Integer.parseInt(wandKey);

            String wandName = config.getString("wands." + wandKey + ".name", "&3Builders Wand");
            Material wandMaterial = Material.valueOf(config.getString("wands." + wandKey + ".material", "BLAZE_ROD"));
            List<String> wandLore = config.getStringList("wands." + wandKey + ".lore");
            int maxSize = config.getInt("wands." + wandKey + ".maxSize", 8);
            String maxSizeText = config.getString("wands." + wandKey + ".maxSizeText", "&3Max Size: {maxSize}");
            int maxRayTraceDistance = config.getInt("wands." + wandKey + ".maxRayTraceDistance", 16);
            boolean consumeItems = config.getBoolean("wands." + wandKey + ".consumeItems", true);
            boolean generatePreviewOnMove = config.getBoolean("wands." + wandKey + ".generatePreviewOnMove", false);

            int durabilityAmount = config.getInt("wands." + wandKey + ".durability.amount", 100);
            boolean durabilityEnabled = config.getBoolean("wands." + wandKey + ".durability.enabled", true);
            String durabilityText = config.getString("wands." + wandKey + ".durability.text", "&3Durability: {durability}");

            String previewParticle = config.getString("wands." + wandKey + ".previewParticle.particle");
            int previewParticleCount = config.getInt("wands." + wandKey + ".previewParticle.count", 1);
            double previewParticleOffsetX = config.getDouble("wands." + wandKey + ".previewParticle.offset.x", 0);
            double previewParticleOffsetY = config.getDouble("wands." + wandKey + ".previewParticle.offset.y", 0);
            double previewParticleOffsetZ = config.getDouble("wands." + wandKey + ".previewParticle.offset.z", 0);
            double previewParticleSpeed = config.getDouble("wands." + wandKey + ".previewParticle.speed", 0);
            int previewParticleOptionsRed = config.getInt("wands." + wandKey + ".previewParticle.options.red", 0);
            int previewParticleOptionsGreen = config.getInt("wands." + wandKey + ".previewParticle.options.green", 0);
            int previewParticleOptionsBlue = config.getInt("wands." + wandKey + ".previewParticle.options.blue", 0);
            int previewParticleOptionsSize = config.getInt("wands." + wandKey + ".previewParticle.options.size", 1);

            float cooldown = config.getInt("wands." + wandKey + ".cooldown", 0);
            List<Material> blockedMaterials = config.getStringList("wands." + wandKey + ".blockedMaterials").stream().map(Material::valueOf).toList();
            boolean isCraftable = config.getBoolean("wands." + wandKey + ".craftable", false);
            boolean craftingRecipeEnabled = config.getBoolean("wands." + wandKey + ".craftingRecipe.enabled", false);
            List<String> recipeShape = List.of();
            Map<Character, Material> recipeIngredients = new HashMap<>();
            if (craftingRecipeEnabled) {
                recipeShape = config.getStringList("wands." + wandKey + ".craftingRecipe.shape");
                recipeIngredients = new HashMap<>();

                if (recipeShape.isEmpty() || recipeShape.size() > 3 || recipeShape.stream().anyMatch(row -> row.length() > 3)) {
                    BuildersWand.getInstance().getLogger().warning("Wand " + wandKey + " has an invalid recipe shape. Must be 1-3 rows of 1-3 characters. Disabling crafting for this wand.");
                    craftingRecipeEnabled = false;
                }
                else {
                    ConfigurationSection ingredientsSection = config.getConfigurationSection("wands." + wandKey + ".craftingRecipe.ingredients");
                    if (ingredientsSection == null) {
                        BuildersWand.getInstance().getLogger().warning("Wand " + wandKey + " has crafting enabled but no ingredients defined. Disabling crafting.");
                        craftingRecipeEnabled = false;
                    }
                    else {
                        for (String key : ingredientsSection.getKeys(false)) {
                            char ingredientChar = key.charAt(0);
                            String materialName = ingredientsSection.getString(key);
                            try {
                                Material ingredientMaterial = null;
                                if (materialName != null) {
                                    ingredientMaterial = Material.valueOf(materialName.toUpperCase());
                                }
                                recipeIngredients.put(ingredientChar, ingredientMaterial);
                            }
                            catch (IllegalArgumentException e) {
                                BuildersWand.getInstance().getLogger().warning("Wand " + wandKey + " has an invalid ingredient material: '" + materialName + "'. Disabling crafting.");
                                craftingRecipeEnabled = false;
                                break;
                            }
                        }
                    }
                }
            }

            Wand wand = new Wand(wandId, wandName, wandMaterial, wandLore, maxSize, maxSizeText,
                    maxRayTraceDistance, consumeItems, generatePreviewOnMove, durabilityAmount,
                    durabilityEnabled, durabilityText, previewParticle,
                    previewParticleCount, previewParticleOffsetX, previewParticleOffsetY, previewParticleOffsetZ,
                    previewParticleSpeed, previewParticleOptionsRed, previewParticleOptionsGreen,
                    previewParticleOptionsBlue, previewParticleOptionsSize, cooldown, blockedMaterials,
                    isCraftable, craftingRecipeEnabled, recipeShape, recipeIngredients);

            wandConfigs.put(wandId, wand);
            wandList.add(wand);
        }
        return wandList;
    }

    public static void reload() {
        BuildersWand plugin = BuildersWand.getInstance();
        plugin.reloadConfig();
        load();
        WandManager manager = BuildersWand.getWandManager();
        manager.registerWands();
        plugin.getLogger().info("BuildersWand configuration reloaded.");
    }

    public static List<Wand> getAllWands() {
        return wandConfigs.values().stream().toList();
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
}
