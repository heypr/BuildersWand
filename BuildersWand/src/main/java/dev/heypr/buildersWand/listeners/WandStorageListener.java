package dev.heypr.buildersWand.listeners;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.managers.WandStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

// TODO CANCEL DRAG AND ITEM MOVE EVENTS
public class WandStorageListener implements Listener {

    private static final int PAGE_SIZE = 45;
    private static final int PREV_BUTTON = 45;
    private static final int PAGE_BUTTON = 49;
    private static final int NEXT_BUTTON = 53;

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof WandStorage storage)) return;

        Player player = (Player) event.getWhoClicked();

        int rawSlot = event.getRawSlot();

        if (clickedInventory.equals(event.getView().getTopInventory()) && rawSlot >= PAGE_SIZE) {
            event.setCancelled(true);
        }

        switch (rawSlot) {
            case PREV_BUTTON -> {
                event.setCancelled(true);
                storage.open(player, storage.getCurrentPage() - 1);
            }
            case NEXT_BUTTON -> {
                event.setCancelled(true);
                storage.open(player, storage.getCurrentPage() + 1);
            }
            case PAGE_BUTTON -> event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof WandStorage storage)) return;
        int page = storage.getCurrentPage();
        for (int i = 0; i < PAGE_SIZE; i++) {
            ItemStack item = event.getView().getTopInventory().getItem(i);
            storage.setItem(page * PAGE_SIZE + i, item);
        }
        BuildersWand.getStorageManager().save(storage.getWand());
    }
}
