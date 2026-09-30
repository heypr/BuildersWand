package dev.heypr.buildersWand.gui;

import dev.heypr.buildersWand.BuildersWand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class WandStorageGui {
    private static final int ROW_SIZE = 9;
    private static final int MAX_CONTENT_ROWS = 5;
    private static final int CONTROL_SCROLL_UP = 0;
    private static final int CONTROL_SCROLL_DOWN = 8;

    public static void open(Player player, Component title, int slotCount, Map<Integer, ItemStack> contents, Predicate<ItemStack> itemFilter, Runnable onClose) {
        WandStorageHolder holder = new WandStorageHolder(player, title, slotCount, contents, itemFilter, onClose);
        int contentRows = Math.clamp((int) Math.ceil(slotCount / (double) ROW_SIZE), 1, MAX_CONTENT_ROWS);
        int size = (contentRows + 1) * ROW_SIZE;
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);
        holder.setContentRows(contentRows);
        renderContent(holder);
        renderControlRow(holder);
        player.openInventory(inventory);
    }

    static void scroll(WandStorageHolder holder, int newOffset) {
        int clamped = Math.clamp(newOffset, 0, maxScrollOffset(holder));
        if (clamped == holder.getScrollOffset()) return;
        captureVisible(holder);
        holder.setScrollOffset(clamped);
        renderContent(holder);
        renderControlRow(holder);
    }

    private static int maxScrollOffset(WandStorageHolder holder) {
        int totalRows = Math.max(1, (int) Math.ceil(holder.getSlotCount() / (double) ROW_SIZE));
        return Math.max(0, totalRows - holder.getContentRows());
    }

    private static void renderContent(WandStorageHolder holder) {
        Inventory inventory = holder.getInventory();
        int contentSlots = holder.getContentRows() * ROW_SIZE;
        int baseIndex = holder.getScrollOffset() * ROW_SIZE;
        for (int relative = 0; relative < contentSlots; relative++) {
            int globalIndex = baseIndex + relative;
            if (globalIndex < holder.getSlotCount()) {
                inventory.setItem(relative, holder.getBacking().get(globalIndex));
            }
            else {
                inventory.setItem(relative, filler());
            }
        }
    }

    private static void renderControlRow(WandStorageHolder holder) {
        Inventory inventory = holder.getInventory();
        int controlStart = holder.getContentRows() * ROW_SIZE;
        for (int relative = 0; relative < ROW_SIZE; relative++) {
            inventory.setItem(controlStart + relative, filler());
        }
        boolean canScrollUp = holder.getScrollOffset() > 0;
        boolean canScrollDown = holder.getScrollOffset() < maxScrollOffset(holder);
        inventory.setItem(controlStart + CONTROL_SCROLL_UP, canScrollUp
                ? namedItem(Material.LIME_STAINED_GLASS_PANE, Component.text("Scroll Up", NamedTextColor.GREEN))
                : namedItem(Material.RED_STAINED_GLASS_PANE, Component.text("At the top", NamedTextColor.RED)));
        inventory.setItem(controlStart + CONTROL_SCROLL_DOWN, canScrollDown
                ? namedItem(Material.LIME_STAINED_GLASS_PANE, Component.text("Scroll Down", NamedTextColor.GREEN))
                : namedItem(Material.RED_STAINED_GLASS_PANE, Component.text("At the bottom", NamedTextColor.RED)));
    }

    static void captureVisible(WandStorageHolder holder) {
        Inventory inventory = holder.getInventory();
        int contentSlots = holder.getContentRows() * ROW_SIZE;
        int baseIndex = holder.getScrollOffset() * ROW_SIZE;
        List<ItemStack> rejected = new ArrayList<>();
        for (int relative = 0; relative < contentSlots; relative++) {
            int globalIndex = baseIndex + relative;
            if (globalIndex >= holder.getSlotCount()) continue;
            ItemStack item = inventory.getItem(relative);
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                holder.getBacking().remove(globalIndex);
            }
            else if (!holder.getItemFilter().test(item)) {
                rejected.add(item.clone());
                holder.getBacking().remove(globalIndex);
            }
            else {
                holder.getBacking().put(globalIndex, item.clone());
            }
        }

        if (!rejected.isEmpty()) {
            returnToPlayer(holder.getViewer(), rejected);
        }
    }

    static void scheduleSelfHeal(WandStorageHolder holder) {
        Bukkit.getScheduler().runTask(BuildersWand.getInstance(), () -> {
            Player player = holder.getViewer();
            if (!player.isOnline()) return;
            if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof WandStorageHolder open) || open != holder) {
                return;
            }
            renderControlRow(holder);
            Inventory inventory = holder.getInventory();
            int contentSlots = holder.getContentRows() * ROW_SIZE;
            int baseIndex = holder.getScrollOffset() * ROW_SIZE;
            List<ItemStack> rejected = new ArrayList<>();
            for (int relative = 0; relative < contentSlots; relative++) {
                int globalIndex = baseIndex + relative;
                if (globalIndex >= holder.getSlotCount()) {
                    inventory.setItem(relative, filler());
                    continue;
                }
                ItemStack item = inventory.getItem(relative);
                if (item != null && !item.getType().isAir() && !holder.getItemFilter().test(item)) {
                    rejected.add(item.clone());
                    inventory.setItem(relative, null);
                }
            }
            if (!rejected.isEmpty()) {
                returnToPlayer(player, rejected);
            }
        });
    }

    static ItemStack resolveIncomingItem(InventoryClickEvent event, Player player) {
        return switch (event.getClick()) {
            case NUMBER_KEY -> player.getInventory().getItem(event.getHotbarButton());
            case SWAP_OFFHAND -> player.getInventory().getItemInOffHand();
            case DOUBLE_CLICK -> null;
            default -> player.getItemOnCursor();
        };
    }

    private static void returnToPlayer(Player player, List<ItemStack> items) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(items.toArray(new ItemStack[0]));
        for (ItemStack overflow : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow);
        }
    }

    private static ItemStack filler() {
        return namedItem(Material.BLACK_STAINED_GLASS_PANE, Component.empty());
    }

    private static ItemStack namedItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        item.setItemMeta(meta);
        return item;
    }
}
