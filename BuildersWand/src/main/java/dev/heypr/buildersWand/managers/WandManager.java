package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.utility.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

import static dev.heypr.buildersWand.managers.ConfigManager.loadWandConfigs;

public class WandManager {

    private final Map<String, Wand> wandConfigMap = new HashMap<>();

    public void registerWands() {
        Util.debug("Starting wand registration process...");
        wandConfigMap.clear();
        List<Wand> loaded = loadWandConfigs();
        Util.debug("Loaded " + loaded.size() + " wand configurations from config.");
        loaded.forEach(wand -> {
            wandConfigMap.put(wand.getId(), wand);
            Util.debug("Successfully registered wand ID: " + wand.getId() + " (" + wand.getName() + ")");
        });
    }

    public Collection<Wand> registeredWands() {
        return wandConfigMap.values();
    }

    public static Wand getWandConfig(String wandId) {
        Util.debug("Fetching wand config for ID: " + wandId);
        Wand wand = BuildersWand.getWandManager().wandConfigMap.get(wandId);
        if (wand == null) Util.debug("Warning: No registered wand found for ID: " + wandId);
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
        Util.debug("Checking isWand: " + (hasKey ? "YES" : "NO") + " for item " + item.getType());
        return hasKey;
    }

    public static Wand getWand(ItemStack item) {
        if (!isWand(item)) {
            Util.debug("getWand failed: Item is not a wand.");
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        String wandId = meta.getPersistentDataContainer().get(BuildersWand.PDC_KEY_ID, PersistentDataType.STRING);

        if (wandId == null) {
            Util.debug("getWand failed: PDC_KEY_ID is missing from item.");
            return null;
        }

        Util.debug("getWand found ID: " + wandId);
        return getWandConfig(wandId);
    }

    public static String getWandID(ItemStack item) {
        if (!isWand(item)) {
            Util.debug("getWandID failed: Item is not a wand.");
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        String wandId = meta.getPersistentDataContainer().get(BuildersWand.PDC_KEY_ID, PersistentDataType.STRING);

        Util.debug("getWandID found ID: " + wandId);
        return wandId;
    }

    public static ItemStack createWandItem(Wand item) {
        if (item == null) {
            Util.debug("createWandItem failed: Wand object is null.");
            return null;
        }

        Util.debug("Creating ItemStack for wand: " + item.getId());
        ItemStack wandItem = new ItemStack(item.getMaterial());
        ItemMeta meta = wandItem.getItemMeta();
        if (meta != null) {
            Util.debug("Setting display name and building lore...");
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

            Util.debug("Injecting PDC data (ID: " + item.getId() + ", Durability: " + item.getDurabilityAmount() + ")");
            meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_ID, PersistentDataType.STRING, item.getId());
            meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_DURABILITY, PersistentDataType.INTEGER, item.getDurabilityAmount());
            meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_UUID, PersistentDataType.STRING, UUID.randomUUID().toString());
            meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_MAX_SIZE, PersistentDataType.INTEGER, item.getMaxSize());

            wandItem.setItemMeta(meta);
            Util.debug("Wand ItemStack creation complete.");
        }
        return wandItem;
    }

    public static void setWandDurability(ItemStack item, boolean infinite, int durability, int maxSize) {
        Wand wand = getWand(item);
        if (wand == null) {
            Util.debug("setWandDurability aborted: Wand config not found.");
            return;
        }

        Util.debug("Updating durability: " + durability + " | MaxSize: " + maxSize + " | Infinite: " + infinite);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_DURABILITY, PersistentDataType.INTEGER, durability);

        Component durabilityText = wand.getDurabilityText().replaceText(TextReplacementConfig.builder().match("{durability}").replacement(String.valueOf(durability)).build());
        Component sizeText = wand.getMaxSizeText().replaceText(TextReplacementConfig.builder().match("{maxSize}").replacement(String.valueOf(maxSize)).build());

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
            Util.debug("Durability hit 0. Removing wand item.");
            item.setAmount(0);
        }
    }

    public static void handleInfiniteDurability(ItemStack item) {
        if (!isWand(item)) return;
        Util.debug("Applying infinite durability logic.");
        setWandDurability(item, true, 2, getMaxSize(item));
    }

    public static void incrementWandDurability(ItemStack item) {
        if (!isWand(item)) return;
        int currentDurability = getWandDurability(item);
        int maxSize = getMaxSize(item);
        Util.debug("Incrementing durability. Current: " + currentDurability);
        setWandDurability(item, false, currentDurability + 1, maxSize);
    }

    public static void decrementWandDurability(ItemStack item) {
        if (!isWand(item)) return;
        int currentDurability = getWandDurability(item);
        int maxSize = getMaxSize(item);
        Util.debug("Decrementing durability. Current: " + currentDurability);
        setWandDurability(item, false, Math.max(0, currentDurability - 1), maxSize);
    }

    public static boolean hasDurability(ItemStack item) {
        boolean hasDur = isWand(item) && item.getItemMeta().getPersistentDataContainer().has(BuildersWand.PDC_KEY_DURABILITY, PersistentDataType.INTEGER);
        Util.debug("hasDurability check: " + hasDur);
        return hasDur;
    }

    public static boolean isWandDurabilityEnabled(ItemStack item) {
        Wand wand = getWand(item);
        boolean enabled = wand != null && wand.isDurabilityEnabled();
        Util.debug("isWandDurabilityEnabled check: " + enabled);
        return enabled;
    }

    public static int getWandDurability(ItemStack item) {
        if (!isWand(item)) return 0;
        int dur = item.getItemMeta().getPersistentDataContainer().getOrDefault(BuildersWand.PDC_KEY_DURABILITY, PersistentDataType.INTEGER, 0);
        Util.debug("Retrieved durability: " + dur);
        return dur;
    }

    public static int getMaxSize(ItemStack item) {
        if (!isWand(item)) return 0;
        Wand wand = getWand(item);
        if (wand == null) return 0;
        int size = item.getItemMeta().getPersistentDataContainer().getOrDefault(BuildersWand.PDC_KEY_MAX_SIZE, PersistentDataType.INTEGER, wand.getMaxSize());
        Util.debug("Retrieved MaxSize: " + size);
        return size;
    }

    public static List<Material> getBlockedMaterials(ItemStack item) {
        Wand wand = getWand(item);
        if (wand == null) return Collections.emptyList();
        Util.debug("Retrieving blocked materials for wand: " + wand.getId());
        return wand.getBlockedMaterials();
    }

    public static ItemStack createWandItem(String wandId) {
        Util.debug("createWandItem requested for ID: " + wandId);
        Wand wand = getWandConfig(wandId);
        return createWandItem(wand);
    }
}
