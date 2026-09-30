package dev.heypr.buildersWand.utility;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.managers.WandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WandItemUtil {

    public static boolean isWand(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        boolean hasKey = item.getItemMeta().getPersistentDataContainer().has(BuildersWand.PDC_KEY_ID, PersistentDataType.STRING);
        ComponentUtil.debug("Checking isWand: " + (hasKey ? "YES" : "NO") + " for item " + item.getType());
        return hasKey;
    }

    public static boolean isRegisteredWand(ItemStack item) {
        Wand wand = getWand(item);
        if (wand == null) {
            return false;
        }
        return BuildersWand.getWandManager().registeredWands().contains(wand);
    }

    public static Wand getWand(ItemStack item) {
        if (!isWand(item)) {
            ComponentUtil.debug("getWand failed: Item is not a wand.");
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        String wandId = meta.getPersistentDataContainer().get(BuildersWand.PDC_KEY_ID, PersistentDataType.STRING);
        if (wandId == null) {
            ComponentUtil.debug("getWand failed: PDC_KEY_ID is missing from item.");
            return null;
        }
        ComponentUtil.debug("getWand found ID: " + wandId);
        return WandManager.getWandConfig(wandId);
    }

    public static boolean isNonCraftableWand(ItemStack item) {
        Wand wand = getWand(item);
        return wand != null && !wand.isCraftable();
    }

    public static ItemStack createWandItem(Wand item) {
        if (item == null) {
            ComponentUtil.debug("createWandItem failed: Wand object is null.");
            return null;
        }
        ComponentUtil.debug("Creating ItemStack for wand: " + item.getId());
        ItemStack wandItem = new ItemStack(item.getMaterial());
        ItemMeta meta = wandItem.getItemMeta();
        if (meta != null) {
            meta.displayName(item.getName());
            meta.lore(buildLore(item, item.getDurabilityAmount(), item.getMaxSize(), item.isDurabilityEnabled()));
            meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_ID, PersistentDataType.STRING, item.getId());
            meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_DURABILITY, PersistentDataType.INTEGER, item.getDurabilityAmount());
            meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_UUID, PersistentDataType.STRING, UUID.randomUUID().toString());
            meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_MAX_SIZE, PersistentDataType.INTEGER, item.getMaxSize());
            wandItem.setItemMeta(meta);
            ComponentUtil.debug("Wand ItemStack creation complete.");
        }
        return wandItem;
    }

    public static void setWandDurability(ItemStack item, boolean infinite, int durability, int maxSize) {
        Wand wand = getWand(item);
        if (wand == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_DURABILITY, PersistentDataType.INTEGER, durability);
        meta.lore(buildLore(wand, durability, maxSize, wand.isDurabilityEnabled()));
        item.setItemMeta(meta);
        if (durability <= 0 && !infinite) {
            item.setAmount(0);
        }
    }

    public static void handleInfiniteDurability(ItemStack item) {
        if (!isWand(item)) {
            return;
        }
        setWandDurability(item, true, 2, getMaxSize(item));
    }

    public static void decrementWandDurability(ItemStack item) {
        if (!isWand(item)) {
            return;
        }
        int currentDurability = getWandDurability(item);
        int maxSize = getMaxSize(item);
        setWandDurability(item, false, Math.max(0, currentDurability - 1), maxSize);
    }

    public static boolean isWandDurabilityEnabled(ItemStack item) {
        Wand wand = getWand(item);
        return wand != null && wand.isDurabilityEnabled();
    }

    public static int getWandDurability(ItemStack item) {
        if (!isWand(item)) {
            return 0;
        }
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(BuildersWand.PDC_KEY_DURABILITY, PersistentDataType.INTEGER, 0);
    }

    public static int getMaxSize(ItemStack item) {
        if (!isWand(item)) {
            return 0;
        }
        Wand wand = getWand(item);
        if (wand == null) {
            return 0;
        }
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(BuildersWand.PDC_KEY_MAX_SIZE, PersistentDataType.INTEGER, wand.getMaxSize());
    }

    public static List<Component> buildLore(Wand wand, int durability, int maxSize, boolean includeDurabilityLine) {
        List<Component> finalLore = new ArrayList<>();
        if (includeDurabilityLine) {
            Component durabilityText = wand.getDurabilityText().replaceText(TextReplacementConfig.builder()
                    .match("\\{durability\\}").replacement(String.valueOf(durability)).build());
            finalLore.add(durabilityText);
        }
        Component sizeText = wand.getMaxSizeText().replaceText(TextReplacementConfig.builder()
                .match("\\{maxSize\\}").replacement(String.valueOf(maxSize)).build());
        finalLore.add(sizeText);
        for (TextComponent lore : wand.getLore()) {
            finalLore.add(lore.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        return finalLore;
    }
}
