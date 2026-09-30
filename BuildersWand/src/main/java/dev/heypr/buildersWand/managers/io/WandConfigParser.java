package dev.heypr.buildersWand.managers.io;

import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.utility.ComponentUtil;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class WandConfigParser {

    private WandConfigParser() {
    }

    static Wand parse(FileConfiguration config, String wandId) {
        String path = "wands." + wandId + ".";
        Wand.Builder builder = Wand.builder(wandId);
        parseAppearance(config, path, wandId, builder);
        parsePlacementMechanics(config, path, wandId, builder);
        parseDurability(config, path, wandId, builder);
        parsePreviewParticle(config, path, wandId, builder);
        parseMiscMechanics(config, path, wandId, builder);
        parseCrafting(config, path, wandId, builder);
        parseStorage(config, path, wandId, builder);
        return builder.build();
    }

    private static void parseAppearance(FileConfiguration config, String path, String wandId, Wand.Builder builder) {
        builder.setName(config.getString(path + "name", "&3Builders Wand"));
        builder.setMaterial(Material.valueOf(config.getString(path + "material", "BLAZE_ROD")));
        builder.setLore(config.getStringList(path + "lore"));
    }

    private static void parsePlacementMechanics(FileConfiguration config, String path, String wandId, Wand.Builder builder) {
        builder.setWandType(Wand.WandType.valueOf(config.getString(path + "wandType", "STANDARD")));
        builder.setStaticLength(config.getInt(path + "staticLength", 3));
        builder.setStaticWidth(config.getInt(path + "staticWidth", 3));
        builder.setMaxSize(config.getInt(path + "maxSize", 8));
        builder.setMaxSizeText(config.getString(path + "maxSizeText", "&3Max Size: {maxSize}"));
        builder.setMaxRayTraceDistance(config.getInt(path + "maxRayTraceDistance", 16));
        builder.setConsumesItems(config.getBoolean(path + "consumeItems", true));
        builder.setGeneratePreviewOnMove(config.getBoolean(path + "generatePreviewOnMove", false));

        List<Material> blockedMaterials = new ArrayList<>();
        for (String mat : config.getStringList(path + "blockedMaterials")) {
            try {
                blockedMaterials.add(Material.valueOf(mat));
            }
            catch (Exception ignored) {}
        }
        builder.setBlockedMaterials(blockedMaterials);
    }

    private static void parseDurability(FileConfiguration config, String path, String wandId, Wand.Builder builder) {
        builder.setDurabilityAmount(config.getInt(path + "durability.amount", 100));
        builder.setDurabilityEnabled(config.getBoolean(path + "durability.enabled", true));
        builder.setDurabilityText(config.getString(path + "durability.text", "&3Durability: {durability}"));
        builder.setBreakSoundEnabled(config.getBoolean(path + "durability.breakSound.enabled", false));

        String breakSoundName = config.getString(path + "durability.breakSound.sound", "ENTITY_ITEM_BREAK");
        Sound breakSound;
        try {
            breakSound = Sound.valueOf(breakSoundName);
        }
        catch (Exception e) {
            ComponentUtil.debug("Invalid break sound for wand '" + wandId + "': " + breakSoundName + ". Defaulting to ENTITY_ITEM_BREAK.");
            breakSound = Sound.ENTITY_ITEM_BREAK;
        }
        builder.setBreakSound(breakSound);
        builder.setBreakSoundMessage(config.getString(path + "durability.breakSound.message", "&cYour wand broke!"));
    }

    private static void parsePreviewParticle(FileConfiguration config, String path, String wandId, Wand.Builder builder) {
        builder.setPreviewParticle(config.getString(path + "previewParticle.particle"));
        builder.setPreviewParticleCount(config.getInt(path + "previewParticle.count", 1));
        builder.setPreviewParticleOffsetX(config.getDouble(path + "previewParticle.offset.x", 0));
        builder.setPreviewParticleOffsetY(config.getDouble(path + "previewParticle.offset.y", 0));
        builder.setPreviewParticleOffsetZ(config.getDouble(path + "previewParticle.offset.z", 0));
        builder.setPreviewParticleSpeed(config.getDouble(path + "previewParticle.speed", 0));
        int red = config.getInt(path + "previewParticle.options.color.red", 0);
        int green = config.getInt(path + "previewParticle.options.color.green", 0);
        int blue = config.getInt(path + "previewParticle.options.color.blue", 0);
        builder.setPreviewParticleColor(Color.fromRGB(red, green, blue));
        builder.setPreviewParticleOptionsSize(config.getInt(path + "previewParticle.options.size", 1));
    }

    private static void parseMiscMechanics(FileConfiguration config, String path, String wandId, Wand.Builder builder) {
        builder.setCooldown((float) config.getDouble(path + "cooldown", 0));
        builder.setUndoHistorySize(config.getInt(path + "undoHistorySize", 10));
        builder.setCanBreakBlocksWhileCrouched(config.getBoolean(path + "canBreakBlocksWhileCrouched", false));
    }

    private static void parseCrafting(FileConfiguration config, String path, String wandId, Wand.Builder builder) {
        boolean isCraftable = config.getBoolean(path + "craftable", false);
        boolean craftingRecipeEnabled = config.getBoolean(path + "craftingRecipe.enabled", false);
        List<String> recipeShape = config.getStringList(path + "craftingRecipe.shape");
        Map<Character, Material> recipeIngredients = new HashMap<>();

        if (craftingRecipeEnabled) {
            ComponentUtil.debug("Loading recipe for wand " + wandId + "...");
            if (recipeShape.isEmpty() || recipeShape.size() > 3 || recipeShape.stream().anyMatch(row -> row.length() > 3)) {
                ComponentUtil.error("Wand " + wandId + " has an invalid recipe shape. Disabling crafting.");
                craftingRecipeEnabled = false;
            }
            else {
                ConfigurationSection ingredientsSection = config.getConfigurationSection(path + "craftingRecipe.ingredients");
                if (ingredientsSection == null) {
                    ComponentUtil.error("Wand " + wandId + " has no ingredients defined. Disabling crafting.");
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
                                ComponentUtil.debug("Registered ingredient: " + ingredientChar + " -> " + mat.name());
                            }
                        }
                        catch (IllegalArgumentException e) {
                            ComponentUtil.error("Wand " + wandId + " invalid ingredient: " + materialName);
                            craftingRecipeEnabled = false;
                            recipeIngredients.clear();
                            break;
                        }
                    }
                }
            }
        }

        builder.setCraftable(isCraftable)
                .setCraftingRecipeEnabled(craftingRecipeEnabled)
                .setRecipeShape(recipeShape)
                .setRecipeIngredients(recipeIngredients);
    }

    private static void parseStorage(FileConfiguration config, String path, String wandId, Wand.Builder builder) {
        int storageMinLines = Math.max(1, config.getInt(path + "storageInventory.minimumLines", 1));
        int storageMaxLines = Math.max(storageMinLines, config.getInt(path + "storageInventory.maximumLines", 5));
        builder.setStorageMinLines(storageMinLines);
        builder.setStorageMaxLines(storageMaxLines);
    }
}
