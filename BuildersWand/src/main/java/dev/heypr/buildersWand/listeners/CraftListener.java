package dev.heypr.buildersWand.listeners;

import dev.heypr.buildersWand.utility.WandItemUtil;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public class CraftListener implements Listener {

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (WandItemUtil.isNonCraftableWand(item)) {
                event.getInventory().setResult(null);
                break;
            }
        }
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        ItemStack item = event.getResult();
        if (WandItemUtil.isNonCraftableWand(item)) {
            event.setResult(new ItemStack(Material.AIR));
            event.setCancelled(true);
        }
    }
}
