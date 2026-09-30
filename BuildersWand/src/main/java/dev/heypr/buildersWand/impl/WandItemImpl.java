package dev.heypr.buildersWand.impl;

import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.api.WandItem;
import dev.heypr.buildersWand.utility.WandItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class WandItemImpl implements WandItem {
    private final ItemStack wandStack;
    private final Wand wandConfig;

    public WandItemImpl(ItemStack wandStack) {
        this.wandStack = wandStack;
        this.wandConfig = WandItemUtil.getWand(wandStack);
    }

    @Override
    public ItemStack getItemStack() {
        return wandStack;
    }

    @Override
    public Wand getWandConfig() {
        return wandConfig;
    }

    @Override
    public int getDurability() {
        return WandItemUtil.getWandDurability(wandStack);
    }

    @Override
    public WandItem setDurability(int durability) {
        WandItemUtil.setWandDurability(wandStack, false, durability, getMaxSize());
        return this;
    }

    @Override
    public int getMaxSize() {
        return WandItemUtil.getMaxSize(wandStack);
    }

    @Override
    public WandItem setDisplayName(Component displayName) {
        wandStack.editMeta(meta -> meta.displayName(displayName));
        return this;
    }

    @Override
    public WandItem setLore(List<Component> newLore) {
        wandStack.editMeta(meta -> meta.lore(newLore));
        return this;
    }

    @Override
    public WandItem updateLore() {
        ItemMeta meta = wandStack.getItemMeta();
        if (meta == null) {
            return this;
        }
        meta.lore(WandItemUtil.buildLore(wandConfig, getDurability(), getMaxSize(), wandConfig.isDurabilityEnabled()));
        wandStack.setItemMeta(meta);
        return this;
    }
}
