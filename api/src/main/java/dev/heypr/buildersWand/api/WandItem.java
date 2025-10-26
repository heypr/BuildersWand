package dev.heypr.buildersWand.api;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * A wrapper for a wand ItemStack that allows for safe modification of its properties.
 * Use BuildersWandAPI.getInstance().getWandItem(item) to get an instance.
 */
public interface WandItem {

    /**
     * @return The underlying ItemStack with all modifications applied.
     */
    ItemStack getItemStack();

    /**
     * @return The base configuration of this wand.
     */
    Wand getWandConfig();

    /**
     * @return The current durability of the wand.
     */
    int getDurability();

    /**
     * Sets the wand's durability and updates its lore.
     *
     * @param durability The new durability value.
     * @return This WandItem instance for method chaining.
     */
    WandItem setDurability(int durability);

    /**
     * @return The maximum block placement size of the wand.
     */
    int getMaxSize();

    /**
     * Sets the wand's display name.
     *
     * @param displayName The new display name as a Component.
     * @return This WandItem instance for method chaining.
     */
    WandItem setDisplayName(Component displayName);

    /**
     * Replaces the wand's entire lore with a new list of components.
     * @param newLore The list of components to set as the new lore.
     * @return This WandItem instance for method chaining.
     */
    WandItem setLore(List<Component> newLore);

    /**
     * Safely refreshes the wand's lore using the base templates from config.yml.
     * @return This WandItem instance for method chaining.
     */
    WandItem updateLore();
}
