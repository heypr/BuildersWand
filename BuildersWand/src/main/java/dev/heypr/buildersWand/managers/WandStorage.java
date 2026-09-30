package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.gui.WandStorageGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WandStorage {
    private static final int SLOTS_PER_LINE = 7;
    private final String wandTypeId;
    private final String wandItemUUID;
    private final Map<Integer, ItemStack> content = new ConcurrentHashMap<>();

    public WandStorage(String wandTypeId, String wandItemUUID) {
        this.wandTypeId = wandTypeId;
        this.wandItemUUID = wandItemUUID;
    }

    public String getWandTypeId() {
        return wandTypeId;
    }

    public String getWandItemUUID() {
        return wandItemUUID;
    }

    public Map<Integer, ItemStack> getContent() {
        return Collections.unmodifiableMap(content);
    }

    public Map<Integer, ItemStack> getContentCopy() {
        Map<Integer, ItemStack> copy = new HashMap<>();
        content.forEach((slot, item) -> {
            if (item != null) {
                copy.put(slot, item.clone());
            }
        });
        return copy;
    }

    public ItemStack getItem(int index) {
        ItemStack item = content.get(index);
        return item != null ? item.clone() : null;
    }

    public void setItem(int index, ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() == 0) {
            content.remove(index);
        }
        else {
            content.put(index, item.clone());
        }
    }

    public boolean hasMaterial(Material material) {
        return content.values().stream().anyMatch(item -> item != null && item.getType() == material);
    }

    public int getCount(Material material) {
        return content.values().stream().filter(item -> item != null && item.getType() == material).mapToInt(ItemStack::getAmount).sum();
    }

    public void removeItems(Material material, int amount) {
        int remaining = amount;
        List<Integer> sortedSlots = new ArrayList<>(content.keySet());
        Collections.sort(sortedSlots);
        for (int slot : sortedSlots) {
            if (remaining <= 0) break;
            ItemStack item = content.get(slot);
            if (item == null || item.getType() != material) continue;
            int quantity = item.getAmount();
            if (quantity <= remaining) {
                remaining -= quantity;
                content.remove(slot);
            }
            else {
                item.setAmount(quantity - remaining);
                remaining = 0;
            }
        }
    }

    public int addItems(ItemStack... itemsToAdd) {
        int overflow = 0;
        for (ItemStack itemToAdd : itemsToAdd) {
            if (itemToAdd == null || itemToAdd.getType().isAir() || itemToAdd.getAmount() <= 0) continue;
            int remaining = itemToAdd.getAmount();
            Material material = itemToAdd.getType();
            for (Map.Entry<Integer, ItemStack> entry : content.entrySet()) {
                if (remaining <= 0) break;
                ItemStack existing = entry.getValue();
                if (existing == null || existing.getType() != material) continue;
                if (!itemsEqualForStacking(existing, itemToAdd)) continue;
                int canAdd = existing.getMaxStackSize() - existing.getAmount();
                if (canAdd <= 0) continue;
                int toAdd = Math.min(canAdd, remaining);
                existing.setAmount(existing.getAmount() + toAdd);
                remaining -= toAdd;
            }
            if (remaining > 0) {
                int nextSlot = findNextEmptySlot();
                if (nextSlot >= 0) {
                    ItemStack newStack = itemToAdd.clone();
                    newStack.setAmount(Math.min(remaining, newStack.getMaxStackSize()));
                    content.put(nextSlot, newStack);
                    remaining -= newStack.getAmount();
                }
            }
            overflow += remaining;
        }
        return overflow;
    }

    public void open(Player player) {
        Wand wand = WandManager.getWandConfig(wandTypeId);
        if (wand == null) {
            return;
        }
        Component title = wand.getName().append(Component.text(" Storage"));
        openInternal(player, title, wand.getStorageMaxLines() * SLOTS_PER_LINE);
    }

    public void openForAdmin(Player admin) {
        Wand wand = WandManager.getWandConfig(wandTypeId);
        if (wand != null) {
            Component title = wand.getName().append(Component.text(" Storage"));
            openInternal(admin, title, wand.getStorageMaxLines() * SLOTS_PER_LINE);
            return;
        }
        int usedLines = content.isEmpty() ? 1 : (int) Math.ceil((content.keySet().stream().max(Integer::compareTo).orElse(0) + 1) / (double) SLOTS_PER_LINE);
        Component title = Component.text("Unknown wand type '" + wandTypeId + "' Storage");
        openInternal(admin, title, Math.max(1, usedLines) * SLOTS_PER_LINE);
    }

    private void openInternal(Player player, Component title, int storageSize) {
        Map<Integer, ItemStack> mutableContents = getContentCopy();
        WandStorageGui.open(player, title, storageSize, mutableContents, WandStorage::isAllowed, () -> {
            content.clear();
            mutableContents.forEach((slot, item) -> {
                if (item != null && !item.getType().isAir() && item.getAmount() > 0) {
                    content.put(slot, item.clone());
                }
            });
            WandStorageManager manager = BuildersWand.getStorageManager();
            if (manager != null) {
                manager.saveNow(wandItemUUID);
            }
        });
    }

    private int findNextEmptySlot() {
        int maxSlot = content.keySet().stream().max(Integer::compareTo).orElse(-1);
        for (int i = 0; i <= maxSlot + 1; i++) {
            if (!content.containsKey(i)) {
                return i;
            }
        }
        return maxSlot + 1;
    }

    private static boolean itemsEqualForStacking(ItemStack a, ItemStack b) {
        return a.getType() == b.getType();
    }

    static boolean isAllowed(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (!item.getType().isBlock() || !item.getType().isItem()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;
        if (meta.hasDisplayName()) return false;
        if (meta.hasLore()) return false;
        if (meta.hasEnchants()) return false;
        if (meta.hasCustomModelData()) return false;
        return meta.getPersistentDataContainer().isEmpty();
    }
}
