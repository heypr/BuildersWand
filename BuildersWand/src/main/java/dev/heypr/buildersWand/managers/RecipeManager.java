package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.utility.Util;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import java.util.Iterator;
import java.util.Map;

public class RecipeManager {

    private final BuildersWand plugin;

    public RecipeManager(BuildersWand plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        for (Wand wand : BuildersWand.getWandManager().registeredWands()) {
            if (wand.isCraftingRecipeEnabled()) {
                NamespacedKey key = new NamespacedKey(plugin, "wand_recipe_" + wand.getId());
                ItemStack result = WandManager.createWandItem(wand);

                ShapedRecipe recipe = new ShapedRecipe(key, result);

                recipe.shape(wand.getRecipeShape().toArray(new String[0]));

                for (Map.Entry<Character, Material> entry : wand.getRecipeIngredients().entrySet()) {
                    recipe.setIngredient(entry.getKey(), entry.getValue());
                    Util.debug("Set ingredient '" + entry.getKey() + "' to " + entry.getValue());
                    Util.debug("Current recipe shape: " + String.join(", ", wand.getRecipeShape()));
                }

                Util.debug("Registering crafting recipe for wand ID: " + wand.getId());

                BuildersWand.getInstance().getServer().addRecipe(recipe, true);
                Util.debug("Successfully registered crafting recipe for wand ID: " + wand.getId());
            }
        }
    }

    public void unregisterRecipes() {
        Iterator<Recipe> iterator = BuildersWand.getInstance().getServer().recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof ShapedRecipe shapedRecipe && shapedRecipe.getKey().getKey().startsWith("wand_recipe")) {
                iterator.remove();
            }
        }
    }
}
