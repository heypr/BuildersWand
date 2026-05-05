package dev.heypr.buildersWand.listeners;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.managers.WandStorage;
import dev.heypr.buildersWand.managers.WandStorageManager;
import dev.heypr.buildersWand.utility.ComponentUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class WandStorageListener implements Listener {

    private static final int PAGE_SIZE = 45;
    private static final int PREV_BUTTON = 45;
    private static final int NEXT_BUTTON = 53;

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof WandStorage storage)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();

        switch (slot) {
            case PREV_BUTTON -> {
                event.setCancelled(true);
                storage.open(player, storage.getCurrentPage() - 1);
            }
            case NEXT_BUTTON -> {
                event.setCancelled(true);
                storage.open(player, storage.getCurrentPage() + 1);
            }
            default -> {
                if (slot >= 0 && slot < PAGE_SIZE) {
                    handleItemWithdraw(storage, player, event, slot);
                }
            }
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
        persistStorage();
    }

    private void handleItemWithdraw(WandStorage storage, Player player, InventoryClickEvent event, int slot) {
        ItemStack clicked = event.getView().getTopInventory().getItem(slot);
        if (clicked == null || clicked.getType().isAir()) return;
        int index = storage.getCurrentPage() * PAGE_SIZE + slot;
        if (event.isShiftClick() || event.isLeftClick()) {
            handleFullStackWithdraw(storage, player, clicked, index);
        }
        else if (event.isRightClick()) {
            handleSingleWithdraw(storage, player, clicked, index);
        }
    }

    private void handleFullStackWithdraw(WandStorage storage, Player player, ItemStack clicked, int index) {
        ItemStack toGive = clicked.clone();
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);
        if (leftover.isEmpty()) {
            storage.removeItem(index);
        }
        else {
            int given = toGive.getAmount() - leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            if (given > 0) {
                clicked.setAmount(clicked.getAmount() - given);
                storage.setItem(index, clicked);
            }
        }
    }

    private void handleSingleWithdraw(WandStorage storage, Player player, ItemStack clicked, int index) {
        ItemStack single = clicked.clone();
        single.setAmount(1);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(single);
        if (leftover.isEmpty()) {
            clicked.setAmount(clicked.getAmount() - 1);
            storage.setItem(index, clicked.getAmount() > 0 ? clicked : null);
        }
    }

    private void persistStorage() {
        if (BuildersWand.getStorageManager() instanceof WandStorageManager manager) {
            try {
                manager.save();
            }
            catch (Exception e) {
                ComponentUtil.error("Failed to save wand storage: " + e.getMessage());
            }
        }
    }
}
