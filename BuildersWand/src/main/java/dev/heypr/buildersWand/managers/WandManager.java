package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.utility.ComponentUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

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
        ComponentUtil.debug("Fetching wand config for ID: " + wandId);
        Wand wand = BuildersWand.getWandManager().wandConfigMap.get(wandId);
        if (wand == null) {
            ComponentUtil.debug("Warning: No registered wand found for ID: " + wandId);
        }
        return wand;
    }

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
        return getWandConfig(wandId);
    }

    public static String getWandID(ItemStack item) {
        if (!isWand(item)) {
            ComponentUtil.debug("getWandID failed: Item is not a wand.");
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(BuildersWand.PDC_KEY_ID, PersistentDataType.STRING);
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
            Component durabilityText = item.getDurabilityText().replaceText(TextReplacementConfig.builder().match("\\{durability\\}").replacement(String.valueOf(item.getDurabilityAmount())).build());
            Component sizeText = item.getMaxSizeText().replaceText(TextReplacementConfig.builder().match("\\{maxSize\\}").replacement(String.valueOf(item.getMaxSize())).build());
            List<Component> finalLore = new ArrayList<>();
            finalLore.add(durabilityText);
            finalLore.add(sizeText);
            for (TextComponent lore : item.getLore()) {
                finalLore.add(lore.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }
            meta.lore(finalLore);
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
        Component durabilityText = wand.getDurabilityText().replaceText(TextReplacementConfig.builder().match("\\{durability\\}").replacement(String.valueOf(durability)).build());
        Component sizeText = wand.getMaxSizeText().replaceText(TextReplacementConfig.builder().match("\\{maxSize\\}").replacement(String.valueOf(maxSize)).build());
        List<Component> finalLore = new ArrayList<>();
        if (!infinite) {
            finalLore.add(durabilityText);
        }
        finalLore.add(sizeText);
        for (TextComponent lore : wand.getLore()) {
            finalLore.add(lore.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        meta.lore(finalLore);
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

    public static void incrementWandDurability(ItemStack item) {
        if (!isWand(item)) {
            return;
        }
        int currentDurability = getWandDurability(item);
        int maxSize = getMaxSize(item);
        setWandDurability(item, false, currentDurability + 1, maxSize);
    }

    public static void decrementWandDurability(ItemStack item) {
        if (!isWand(item)) {
            return;
        }
        int currentDurability = getWandDurability(item);
        int maxSize = getMaxSize(item);
        setWandDurability(item, false, Math.max(0, currentDurability - 1), maxSize);
    }

    public static boolean hasDurability(ItemStack item) {
        return isWand(item) && item.getItemMeta().getPersistentDataContainer().has(BuildersWand.PDC_KEY_DURABILITY, PersistentDataType.INTEGER);
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

    public static List<Material> getBlockedMaterials(ItemStack item) {
        Wand wand = getWand(item);
        if (wand == null) {
            return Collections.emptyList();
        }
        return wand.getBlockedMaterials();
    }

    public static ItemStack createWandItem(String wandId) {
        Wand wand = getWandConfig(wandId);
        return createWandItem(wand);
    }
}
