package dev.heypr.buildersWand.utility;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.managers.WandStorage;
import dev.heypr.buildersWand.managers.WandStorageManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InventoryUtil {

    public static int getItemCount(Player player, Material material, Wand wand, ItemStack wandItem) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        WandStorageManager manager = BuildersWand.getStorageManager();
        if (manager != null) {
            WandStorage storage = manager.getStorage(wand, wandItem);
            if (storage != null) {
                count += storage.getCount(material);
            }
        }
        return count;
    }

    public static void removeItems(Player player, Block block, int amount, Wand wand, ItemStack wandItem) {
        int remaining = amount;

        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() != block.getType()) continue;
            int qty = item.getAmount();
            if (qty <= remaining) {
                remaining -= qty;
                player.getInventory().setItem(i, null);
            }
            else {
                item.setAmount(qty - remaining);
                player.getInventory().setItem(i, item);
                remaining = 0;
            }
        }

        if (remaining <= 0) return;

        WandStorageManager manager = BuildersWand.getStorageManager();
        if (manager != null) {
            WandStorage storage = manager.getStorage(wand, wandItem);
            if (storage != null && storage.hasMaterial(block.getType())) {
                storage.removeItems(block.getType(), remaining);
                manager.save(wandItem.getItemMeta().getPersistentDataContainer()
                        .get(BuildersWand.PDC_KEY_UUID, org.bukkit.persistence.PersistentDataType.STRING));
            }
        }
    }

    public static void returnItems(Player player, Material material, int amount, Wand wand, ItemStack wandItem) {
        if (amount <= 0) return;
        List<ItemStack> items = new ArrayList<>();
        int remaining = amount;
        int maxStack = material.getMaxStackSize();
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            items.add(new ItemStack(material, stackAmount));
            remaining -= stackAmount;
        }
        returnItems(player, items, wand, wandItem);
    }

    public static void returnItems(Player player, List<ItemStack> items, Wand wand, ItemStack wandItem) {
        if (items == null || items.isEmpty() || player.getGameMode().isInvulnerable()) {
            return;
        }
        List<ItemStack> toDrop = new ArrayList<>();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) continue;
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                toDrop.addAll(leftover.values());
            }
        }
        if (toDrop.isEmpty()) return;
        tryAddToStorage(player, toDrop, wand, wandItem);
    }

    private static void tryAddToStorage(Player player, List<ItemStack> items, Wand wand, ItemStack wandItem) {
        WandStorageManager manager = BuildersWand.getStorageManager();
        if (manager == null) {
            dropItems(player, items);
            return;
        }
        WandStorage storage = manager.getStorage(wand, wandItem);
        if (storage == null) {
            dropItems(player, items);
            return;
        }

        List<ItemStack> stillLeftover = new ArrayList<>();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) continue;
            int overflow = storage.addItems(item);
            if (overflow > 0) {
                ItemStack overflowStack = item.clone();
                overflowStack.setAmount(overflow);
                stillLeftover.add(overflowStack);
            }
        }

        if (!stillLeftover.isEmpty()) {
            List<ItemStack> toDrop = new ArrayList<>();
            for (ItemStack item : stillLeftover) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                toDrop.addAll(leftover.values());
            }
            if (!toDrop.isEmpty()) {
                dropItems(player, toDrop);
            }
        }

        String wandUUID = wandItem.getItemMeta().getPersistentDataContainer().get(BuildersWand.PDC_KEY_UUID, PersistentDataType.STRING);
        if (wandUUID != null) {
            manager.save(wandUUID);
        }
    }

    private static void dropItems(Player player, List<ItemStack> items) {
        Location location = player.getLocation();
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir() && item.getAmount() > 0) {
                player.getWorld().dropItemNaturally(location, item);
            }
        }
    }
}
