package dev.heypr.buildersWand.utility;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryUtil {

    public static int getItemCount(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        // todo: add wandstorage inventory checking here.
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
        // todo: mark if item is being taken from the player's inventory or the wand storage and place accordingly.
        // todo: put overflowing items into wand storage.
    }
}
