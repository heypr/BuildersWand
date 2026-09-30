package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.utility.ComponentUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.heypr.buildersWand.managers.io.ConfigManager.loadWandConfigs;

public class WandManager {
    private final Map<String, Wand> wandConfigMap = new HashMap<>();

    public void registerWands() {
        ComponentUtil.debug("Starting wand registration process...");
        wandConfigMap.clear();
        List<Wand> loaded = loadWandConfigs();
        ComponentUtil.debug("Loaded " + loaded.size() + " wand configurations from config.");
        loaded.forEach(wand -> {
            wandConfigMap.put(wand.getId(), wand);
            ComponentUtil.debug("Successfully registered wand ID: " + wand.getId() + " (" + wand.getName() + ")");
        });
    }

    public Collection<Wand> registeredWands() {
        return wandConfigMap.values();
    }

    public static Wand getWandConfig(String wandId) {
        Wand wand = BuildersWand.getWandManager().wandConfigMap.get(wandId);
        if (wand == null) {
            ComponentUtil.debug("Warning: No registered wand found for ID: " + wandId);
        }
        return wand;
    }
}
