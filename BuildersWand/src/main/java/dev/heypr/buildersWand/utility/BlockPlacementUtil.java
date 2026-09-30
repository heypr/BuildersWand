package dev.heypr.buildersWand.utility;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class BlockPlacementUtil {

    public static boolean place(Block block, Material material, Block against, Player player, boolean fireEvent) {
        if (!fireEvent) {
            block.setType(material, true);
            return true;
        }
        BlockPlaceEvent event = new BlockPlaceEvent(block, block.getState(), against, new ItemStack(material), player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        block.setType(material, true);
        return true;
    }
}
