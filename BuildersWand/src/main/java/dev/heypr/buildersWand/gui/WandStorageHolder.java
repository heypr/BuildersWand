package dev.heypr.buildersWand.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Predicate;

final class WandStorageHolder implements InventoryHolder {

    private final Player viewer;
    private final Component title;
    private final int slotCount;
    private final Map<Integer, ItemStack> backing;
    private final Predicate<ItemStack> itemFilter;
    private final Runnable onClose;

    private Inventory inventory;
    private int contentRows;
    private int scrollOffset;

    WandStorageHolder(Player viewer, Component title, int slotCount, Map<Integer, ItemStack> backing,
                       Predicate<ItemStack> itemFilter, Runnable onClose) {
        this.viewer = viewer;
        this.title = title;
        this.slotCount = slotCount;
        this.backing = backing;
        this.itemFilter = itemFilter;
        this.onClose = onClose;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    Player getViewer() {
        return viewer;
    }

    Component getTitle() {
        return title;
    }

    int getSlotCount() {
        return slotCount;
    }

    Map<Integer, ItemStack> getBacking() {
        return backing;
    }

    Predicate<ItemStack> getItemFilter() {
        return itemFilter;
    }

    Runnable getOnClose() {
        return onClose;
    }

    int getContentRows() {
        return contentRows;
    }

    void setContentRows(int contentRows) {
        this.contentRows = contentRows;
    }

    int getScrollOffset() {
        return scrollOffset;
    }

    void setScrollOffset(int scrollOffset) {
        this.scrollOffset = scrollOffset;
    }
}
