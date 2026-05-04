package dev.heypr.buildersWand.listeners;

import dev.heypr.buildersWand.managers.WandManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;

public class FurnaceListener implements Listener {

    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        ItemStack item = event.getFuel();
        if (WandManager.isWand(item) && !WandManager.getWand(item).isCraftable()) {
            event.setConsumeFuel(false);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        ItemStack item = event.getResult();
        if (WandManager.isWand(item) && !WandManager.getWand(item).isCraftable()) {
            event.setCancelled(true);
        }
    }
}
