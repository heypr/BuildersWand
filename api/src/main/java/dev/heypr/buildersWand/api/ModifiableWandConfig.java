package dev.heypr.buildersWand.api;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifiableWandConfig {

    private final FileConfiguration config;
    private final String recipeBasePath;
    private final String basePath;

    public ModifiableWandConfig(FileConfiguration config, int wandId) {
        this.config = config;
        this.basePath = "wands." + wandId + ".";
        this.recipeBasePath = basePath + "craftingRecipe.";
    }

    public ModifiableWandConfig setName(String name) {
        config.set(basePath + "name", name);
        return this;
    }

    public ModifiableWandConfig setMaterial(Material material) {
        config.set(basePath + "material", material.name());
        return this;
    }

    public ModifiableWandConfig setLore(List<String> lore) {
        config.set(basePath + "lore", lore);
        return this;
    }

    public ModifiableWandConfig setMaxSize(int maxSize) {
        config.set(basePath + "maxSize", maxSize);
        return this;
    }

    public ModifiableWandConfig setDurabilityAmount(int amount) {
        config.set(basePath + "durability.amount", amount);
        return this;
    }

    public ModifiableWandConfig setCooldown(float cooldownInSeconds) {
        config.set(basePath + "cooldown", cooldownInSeconds);
        return this;
    }

    public ModifiableWandConfig setCraftingRecipe(List<String> shape, Map<Character, Material> ingredients) {
        if (shape == null || shape.isEmpty() || shape.size() > 3 || shape.stream().anyMatch(s -> s.length() > 3)) {
            throw new IllegalArgumentException("Recipe shape must be 1-3 rows of 1-3 characters each.");
        }
        if (ingredients == null || ingredients.isEmpty()) {
            throw new IllegalArgumentException("Recipe must have at least one ingredient.");
        }
        config.set(recipeBasePath + "enabled", true);
        config.set(recipeBasePath + "shape", shape);
        Map<String, String> ingredientStrings = new HashMap<>();
        for (Map.Entry<Character, Material> entry : ingredients.entrySet()) {
            ingredientStrings.put(entry.getKey().toString(), entry.getValue().name());
        }
        config.set(recipeBasePath + "ingredients", ingredientStrings);
        return this;
    }

    public ModifiableWandConfig disableCrafting() {
        config.set(recipeBasePath + "enabled", false);
        return this;
    }
}