package dev.heypr.buildersWand.impl;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.BuildersWandAPI;
import dev.heypr.buildersWand.api.ModifiableWandConfig;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.api.WandItem;
import dev.heypr.buildersWand.managers.ConfigManager;
import dev.heypr.buildersWand.managers.WandManager;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class ApiImplementation extends BuildersWandAPI {

    private final BuildersWand plugin;

    public ApiImplementation(BuildersWand plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isWand(ItemStack item) {
        return WandManager.isWand(item);
    }

    @Override
    public WandItem getWandItem(ItemStack item) {
        if (!isWand(item)) {
            throw new IllegalArgumentException("The provided ItemStack is not a BuildersWand.");
        }
        return new WandItemImpl(item);
    }

    @Override
    public ModifiableWandConfig getModifiableConfig(int wandId) {
        if (plugin.getConfig().isConfigurationSection("wands." + wandId)) {
            return new ModifiableWandConfig(plugin.getConfig(), wandId);
        }
        return null;
    }

    @Override
    public Wand getWandById(int wandId) {
        return WandManager.getWandConfig(wandId);
    }

    @Override
    public ItemStack createWand(int wandId) {
        Wand wand = getWandById(wandId);
        return wand != null ? WandManager.createWandItem(wand) : null;
    }

    @Override
    public Collection<Wand> getAllRegisteredWands() {
        return BuildersWand.getWandManager().registeredWands();
    }

    @Override
    public void saveAndReload() {
        ConfigManager.reload();
    }
}
