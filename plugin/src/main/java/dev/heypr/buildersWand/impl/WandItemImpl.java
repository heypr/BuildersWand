package dev.heypr.buildersWand.impl;

import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.api.WandItem;
import dev.heypr.buildersWand.managers.WandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WandItemImpl implements WandItem {

    private final ItemStack wandStack;
    private final Wand wandConfig;

    public WandItemImpl(ItemStack wandStack) {
        this.wandStack = Objects.requireNonNull(wandStack, "ItemStack cannot be null");
        this.wandConfig = WandManager.getWand(wandStack);
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
        return WandManager.getWandDurability(wandStack);
    }

    @Override
    public WandItem setDurability(int durability) {
        WandManager.setWandDurability(wandStack, false, durability, getMaxSize());
        return this;
    }

    @Override
    public int getMaxSize() {
        return WandManager.getMaxSize(wandStack);
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
        if (meta == null) return this;

        List<Component> finalLore = new ArrayList<>();
        int currentDurability = getDurability();
        int currentMaxSize = getMaxSize();

        if (wandConfig.isDurabilityEnabled()) {
            Component durabilityText = wandConfig.getDurabilityText()
                    .replaceText(TextReplacementConfig.builder()
                            .match("\\{durability\\}")
                            .replacement(String.valueOf(currentDurability)).build());
            finalLore.add(durabilityText);
        }

        Component sizeText = wandConfig.getMaxSizeText()
                .replaceText(TextReplacementConfig.builder()
                        .match("\\{maxSize\\}")
                        .replacement(String.valueOf(currentMaxSize)).build());
        finalLore.add(sizeText);

        finalLore.addAll(wandConfig.getLore());
        meta.lore(finalLore);
        wandStack.setItemMeta(meta);
        return this;
    }
}