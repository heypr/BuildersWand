package dev.heypr.buildersWand.utility;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.managers.WandStorage;
import dev.heypr.buildersWand.managers.WandStorageManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InventoryUtil {

    public static int getItemCount(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        WandStorageManager manager = BuildersWand.getStorageManager();
        if (manager != null) {
            for (WandStorage storage : manager.getAllStorages()) {
                for (ItemStack item : storage.getItems()) {
                    if (item != null && item.getType() == material) {
                        count += item.getAmount();
                    }
                }
            }
        }
        return count;
    }

    public static void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) {
                continue;
            }
            int amt = item.getAmount();
            if (amt <= remaining) {
                remaining -= amt;
                item.setAmount(0);
            }
            else {
                item.setAmount(amt - remaining);
                break;
            }
        }
        if (remaining <= 0) return;
        WandStorageManager manager = BuildersWand.getStorageManager();
        if (manager != null) {
            for (WandStorage storage : manager.getAllStorages()) {
                if (remaining <= 0) break;
                remaining -= storage.removeItems(material, remaining);
            }
        }
    }

    public static void returnItems(Player player, List<ItemStack> items) {
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
        tryAddToStorage(player, toDrop);
    }

    private static void tryAddToStorage(Player player, List<ItemStack> items) {
        WandStorageManager manager = BuildersWand.getStorageManager();
        if (manager == null) {
            dropItems(player, items);
            return;
        }

        List<ItemStack> notStored = new ArrayList<>();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) continue;
            boolean stored = false;
            for (WandStorage storage : manager.getAllStorages()) {
                int maxIndex = storage.getAllContent().keySet().stream()
                        .max(Integer::compareTo)
                        .orElse(-1);

                storage.setItem(maxIndex + 1, item.clone());
                stored = true;
                break;
            }
            if (!stored) {
                notStored.add(item);
            }
        }
        if (!notStored.isEmpty()) {
            dropItems(player, notStored);
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
