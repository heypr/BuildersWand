package dev.heypr.buildersWand.listeners;

import dev.heypr.buildersWand.managers.WandManager;
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
            if (WandManager.isWand(item) && !WandManager.getWand(item).isCraftable()) {
                event.getInventory().setResult(null);
                break;
            }
        }
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        ItemStack item = event.getResult();
        if (WandManager.isWand(item) && !WandManager.getWand(item).isCraftable()) {
            event.setResult(new ItemStack(Material.AIR));
            event.setCancelled(true);
        }
    }
}
