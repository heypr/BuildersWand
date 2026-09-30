package dev.heypr.buildersWand.listeners;

import dev.heypr.buildersWand.utility.WandItemUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;

public class FurnaceListener implements Listener {

    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        ItemStack item = event.getFuel();
        if (WandItemUtil.isNonCraftableWand(item)) {
            event.setConsumeFuel(false);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        ItemStack item = event.getResult();
        if (WandItemUtil.isNonCraftableWand(item)) {
            event.setCancelled(true);
        }
    }
}
