package dev.heypr.buildersWand.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class WandStorageGuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof WandStorageHolder holder)) return;

        Inventory top = event.getView().getTopInventory();
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        if (clicked.equals(top)) {
            int rawSlot = event.getRawSlot();
            int controlStart = holder.getContentRows() * WandStorageGui.ROW_SIZE;

            if (rawSlot >= controlStart) {
                event.setCancelled(true);
                int relative = rawSlot - controlStart;
                if (relative == 0) {
                    WandStorageGui.scroll(holder, holder.getScrollOffset() - 1);
                }
                else if (relative == 8) {
                    WandStorageGui.scroll(holder, holder.getScrollOffset() + 1);
                }
                return;
            }

            int globalIndex = holder.getScrollOffset() * WandStorageGui.ROW_SIZE + rawSlot;
            if (globalIndex >= holder.getSlotCount()) {
                event.setCancelled(true);
            }
            else {
                ItemStack incoming = WandStorageGui.resolveIncomingItem(event, (Player) event.getWhoClicked());
                if (incoming != null && !incoming.getType().isAir() && !holder.getItemFilter().test(incoming)) {
                    event.setCancelled(true);
                }
            }
        }
        else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            ItemStack moving = event.getCurrentItem();
            if (moving != null && !moving.getType().isAir() && !holder.getItemFilter().test(moving)) {
                event.setCancelled(true);
            }
        }

        WandStorageGui.scheduleSelfHeal(holder);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof WandStorageHolder holder)) return;

        Inventory top = event.getView().getTopInventory();
        int topSize = top.getSize();
        int controlStart = holder.getContentRows() * WandStorageGui.ROW_SIZE;

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize) continue;
            if (rawSlot >= controlStart) {
                event.setCancelled(true);
                break;
            }
            int globalIndex = holder.getScrollOffset() * WandStorageGui.ROW_SIZE + rawSlot;
            if (globalIndex >= holder.getSlotCount()) {
                event.setCancelled(true);
                break;
            }
            ItemStack resulting = event.getNewItems().get(rawSlot);
            if (resulting != null && !resulting.getType().isAir() && !holder.getItemFilter().test(resulting)) {
                event.setCancelled(true);
                break;
            }
        }

        WandStorageGui.scheduleSelfHeal(holder);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof WandStorageHolder holder)) return;

        WandStorageGui.captureVisible(holder);
        holder.getOnClose().run();
    }
}
