package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

import static dev.heypr.buildersWand.managers.ConfigManager.*;

public class WandManager {

    private final Map<Integer, Wand> wandConfigMap = new HashMap<>();

    public void registerWands() {
        wandConfigMap.clear();
        loadWandConfigs().forEach(wand -> wandConfigMap.put(wand.getId(), wand));
    }

    public Collection<Wand> registeredWands() {
        return wandConfigMap.values();
    }

    public static Wand getWandConfig(int wandId) {
        return BuildersWand.getWandManager().wandConfigMap.get(wandId);
    }

    public static boolean isWand(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(BuildersWand.PDC_KEY_ID, PersistentDataType.INTEGER);
    }

    public static Wand getWand(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (isWand(item)) {
            int wandId = meta.getPersistentDataContainer().get(BuildersWand.PDC_KEY_ID, PersistentDataType.INTEGER);
            return getWandConfig(wandId);
        }
        return null;
    }

    public static ItemStack createWandItem(Wand item) {
        if (item == null) return null;

        ItemStack wandItem = new ItemStack(item.getMaterial());
        ItemMeta meta = wandItem.getItemMeta();
        if (meta != null) {
            meta.displayName(item.getName());
            Component durabilityText = item.getDurabilityText().replaceText(TextReplacementConfig.builder().match("\\{durability\\}").replacement(String.valueOf(item.getDurabilityAmount())).build());
            Component sizeText = item.getMaxSizeText().replaceText(TextReplacementConfig.builder().match("\\{maxSize\\}").replacement(String.valueOf(item.getMaxSize())).build());
            List<Component> finalLore = new ArrayList<>();
            finalLore.add(durabilityText);
            finalLore.add(sizeText);
            for (TextComponent lore : item.getLore()) finalLore.add(lore.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            meta.lore(finalLore);
            meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_ID, PersistentDataType.INTEGER, item.getId());
            meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_DURABILITY, PersistentDataType.INTEGER, item.getDurabilityAmount());
            meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_UUID, PersistentDataType.STRING, UUID.randomUUID().toString());
            wandItem.setItemMeta(meta);
        }
        return wandItem;
    }

    public static void setWandDurability(ItemStack item, boolean infinite, int durability, int maxSize) {
        if (item == null || item.getItemMeta() == null || !isWand(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_DURABILITY, PersistentDataType.INTEGER, durability);
        Component durabilityText = getWand(item).getDurabilityText().replaceText(TextReplacementConfig.builder().match("\\{durability\\}").replacement(String.valueOf(durability)).build());
        Component sizeText = getWand(item).getMaxSizeText().replaceText(TextReplacementConfig.builder().match("\\{maxSize\\}").replacement(String.valueOf(maxSize)).build());
        List<Component> finalLore = new ArrayList<>();
        if (!infinite) finalLore.add(durabilityText);
        finalLore.add(sizeText);
        for (TextComponent lore : getWand(item).getLore()) finalLore.add(lore.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        meta.lore(finalLore);
        item.setItemMeta(meta);
        if (durability <= 0 && infinite) {
            item.setAmount(0);
        }
    }

    public static void handleInfiniteDurability(ItemStack item) {
        if (item == null || item.getItemMeta() == null || !isWand(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            setWandDurability(item, true, 2, getMaxSize(item));
        }
    }

    public static void incrementWandDurability(ItemStack item) {
        if (item == null || item.getItemMeta() == null || !isWand(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int currentDurability = getWandDurability(item);
            int maxSize = getMaxSize(item);
            setWandDurability(item, false, currentDurability + 1, maxSize);
        }
    }

    public static void decrementWandDurability(ItemStack item) {
        if (item == null || item.getItemMeta() == null || !isWand(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int currentDurability = getWandDurability(item);
            int maxSize = getMaxSize(item);
            setWandDurability(item, false, Math.max(0, currentDurability - 1), maxSize);
        }
    }

    public static boolean hasDurability(ItemStack item) {
        if (item == null || item.getItemMeta() == null || !isWand(item)) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(BuildersWand.PDC_KEY_DURABILITY, PersistentDataType.INTEGER);
    }

    public static boolean isWandDurabilityEnabled(ItemStack item) {
        if (item == null || item.getItemMeta() == null || !isWand(item)) return false;
        Wand wand = getWand(item);
        return wand != null && wand.isDurabilityEnabled();
    }

    public static int getWandDurability(ItemStack item) {
        if (item == null || item.getItemMeta() == null || !isWand(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().getOrDefault(BuildersWand.PDC_KEY_DURABILITY, PersistentDataType.INTEGER, 0);
    }

    public static int getMaxSize(ItemStack item) {
        if (item == null || item.getItemMeta() == null || !isWand(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().getOrDefault(BuildersWand.PDC_KEY_MAX_SIZE, PersistentDataType.INTEGER, getWand(item).getMaxSize());
    }

    public static List<Material> getBlockedMaterials(ItemStack item) {
        if (item == null || item.getItemMeta() == null || !isWand(item)) return Collections.emptyList();
        Wand wand = getWand(item);
        return wand != null ? wand.getBlockedMaterials() : Collections.emptyList();
    }

    public static ItemStack createWandItem(int wandId) {
        Wand wand = getWandConfig(wandId);
        if (wand == null) return null;

        ItemStack wandItem = new ItemStack(wand.getMaterial());
        ItemMeta meta = wandItem.getItemMeta();
        if (meta != null) {
            meta.displayName(wand.getName());
            meta.getPersistentDataContainer().set(BuildersWand.PDC_KEY_ID, PersistentDataType.INTEGER, wandId);
            wandItem.setItemMeta(meta);
        }
        return wandItem;
    }
}
