package dev.heypr.buildersWand.api;

import org.bukkit.inventory.ItemStack;
import java.util.Collection;

/**
 * The main API class for the BuildersWand plugin.
 * This class is the entry point for all interactions with the plugin.
 *
 * NOTE: This is an interface. The actual implementation is provided by the BuildersWand plugin.
 * You can get an instance of the implementation by calling BuildersWand.getApi().
 */
public abstract class BuildersWandAPI {

    private static BuildersWandAPI instance;

    /**
     * Gets the currently active instance of the BuildersWandAPI.
     * @return The API instance.
     * @throws IllegalStateException if the BuildersWand plugin is not enabled.
     */
    public static BuildersWandAPI getInstance() {
        if (instance == null) {
            throw new IllegalStateException("The BuildersWandAPI is not yet available. Is the BuildersWand plugin enabled?");
        }
        return instance;
    }

    /**
     * INTERNAL USE ONLY.
     * This method is called by the BuildersWand plugin to set the API instance.
     * @param apiInstance The instance of the API implementation.
     */
    public static void setInstance(BuildersWandAPI apiInstance) {
        instance = apiInstance;
    }

    public abstract boolean isWand(ItemStack item);

    public abstract WandItem getWandItem(ItemStack item);

    public abstract ModifiableWandConfig getModifiableConfig(int wandId);

    public abstract Wand getWandById(int wandId);

    public abstract ItemStack createWand(int wandId);

    public abstract Collection<Wand> getAllRegisteredWands();

    /**
     * Saves all pending configuration changes made via ModifiableWandConfig
     * and reloads all recipes and wand configurations on the server.
     */
    public abstract void saveAndReload();
}
