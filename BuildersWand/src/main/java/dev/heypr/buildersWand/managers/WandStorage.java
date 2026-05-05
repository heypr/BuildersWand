package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.api.Wand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WandStorage implements InventoryHolder {

    private static final int PAGE_SIZE = 45;
    private static final int INVENTORY_SIZE = 54;

    private final Wand wand;
    private final ConcurrentHashMap<Integer, ItemStack> contentMap = new ConcurrentHashMap<>();
    private int currentPage = 0;
    private Inventory inventory;

    public WandStorage(Wand wand) {
        this.wand = wand;
        this.inventory = buildInventoryForPage(0);
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }

    public Wand getWand() {
        return wand;
    }

    public ItemStack getItem(int index) {
        return contentMap.get(index);
    }

    public Collection<ItemStack> getItems() {
        return contentMap.values();
    }

    public Map<Integer, ItemStack> getAllContent() {
        return Collections.unmodifiableMap(contentMap);
    }

    public void setItem(int index, ItemStack item) {
        if (item == null) {
            contentMap.remove(index);
        }
        else {
            contentMap.put(index, item);
        }
        updateInventorySlot(index, item);
    }

    public void removeItem(int index) {
        contentMap.remove(index);
        updateInventorySlot(index, null);
    }

    public boolean hasItem(ItemStack item) {
        return contentMap.containsValue(item);
    }

    public boolean hasMaterial(Material material) {
        return contentMap.values().parallelStream().anyMatch(item -> item != null && item.getType() == material);
    }

    public int itemCount() {
        return contentMap.values().parallelStream().mapToInt(item -> item != null ? item.getAmount() : 0).sum();
    }

    public int getTotalPages() {
        return contentMap.keySet().stream()
                .max(Integer::compareTo)
                .map(maxIndex -> (maxIndex + PAGE_SIZE) / PAGE_SIZE)
                .orElse(1);
    }

    public void open(Player player, int page) {
        int totalPages = getTotalPages();
        currentPage = Math.clamp(page, 0, totalPages - 1);
        inventory = buildInventoryForPage(currentPage);
        player.openInventory(inventory);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int removeItems(Material material, int amount) {
        int removed = 0;
        List<Integer> toDelete = new ArrayList<>();
        for (var entry : contentMap.entrySet()) {
            if (removed >= amount) break;
            if (entry.getValue() instanceof ItemStack stack && stack.getType() == material) {
                int take = Math.min(stack.getAmount(), amount - removed);
                stack.setAmount(stack.getAmount() - take);
                removed += take;
                if (stack.getAmount() <= 0) {
                    toDelete.add(entry.getKey());
                }
            }
        }
        toDelete.forEach(contentMap::remove);
        return removed;
    }

    private void updateInventorySlot(int index, ItemStack item) {
        if (isIndexVisible(index)) {
            inventory.setItem(index % PAGE_SIZE, item);
        }
    }

    private boolean isIndexVisible(int index) {
        return (index / PAGE_SIZE) == currentPage;
    }

    private Inventory buildInventoryForPage(int page) {
        Inventory inv = Bukkit.createInventory(this, INVENTORY_SIZE, String.format("Wand Storage - %s (%d)", wand.getId(), page + 1));
        int startIndex = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            inv.setItem(i, contentMap.get(startIndex + i));
        }
        inv.setItem(45, createControl(Material.ARROW, "Previous Page"));
        inv.setItem(49, createControl(Material.PAPER, String.format("Page: %d", page + 1)));
        inv.setItem(53, createControl(Material.ARROW, "Next Page"));
        return inv;
    }

    private ItemStack createControl(Material material, String name) {
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof ItemMeta meta) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
