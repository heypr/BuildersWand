package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.Wand;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private static final Map<Integer, Wand> wandConfigs = new HashMap<>();
    private static boolean placementQueueEnabled;
    private static int maxBlocksPerTick;

    public static void load() {
        BuildersWand plugin = BuildersWand.getInstance();
        plugin.saveDefaultConfig();
        wandConfigs.clear();
        loadWandConfigs();
        FileConfiguration config = plugin.getConfig();
        placementQueueEnabled = config.getBoolean("placementQueue.enabled", true);
        maxBlocksPerTick = config.getInt("placementQueue.maxBlocksPerTick", 20);
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
            float cooldown = config.getInt("wands." + wandKey + ".cooldown", 0);
            List<Material> blockedMaterials = config.getStringList("wands." + wandKey + ".blockedMaterials").stream().map(Material::valueOf).toList();
            boolean isCraftable = config.getBoolean("wands." + wandKey + ".craftable", false);

            Wand wand = new Wand(wandId, wandName, wandMaterial, wandLore, maxSize, maxSizeText,
                    maxRayTraceDistance, consumeItems, generatePreviewOnMove, durabilityAmount,
                    durabilityEnabled, durabilityText, cooldown, blockedMaterials, isCraftable);

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

    public static int getMaxBlocksPerTick() {
        return maxBlocksPerTick;
    }

    public static boolean isPlacementQueueEnabled() {
        return placementQueueEnabled;
    }
}
