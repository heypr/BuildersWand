package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.BuildersWand;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class WandStorage implements InventoryHolder {

    private final Inventory inventory;
    private final Player player;
    private final ConcurrentHashMap<Integer, ItemStack> contentMap = new ConcurrentHashMap<>();
    private final Collection<ItemStack> contentList = this.contentMap.values();

    public WandStorage(BuildersWand plugin, Player player) {
        this.inventory = plugin.getServer().createInventory(this, 54);
        this.player = player;
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return this.inventory;
    }

    public Player getPlayer() {
        return this.player;
    }

    public ItemStack getItem(int slot) {
        return this.contentMap.get(slot);
    }

    public Collection<ItemStack> getItems() {
        return this.contentList;
    }

    public void setItem(int slot, ItemStack item) {
        this.contentMap.put(slot, item);
    }

    public void removeItem(int slot) {
        ItemStack item = this.contentMap.remove(slot);
        if (item != null) {
            this.contentList.remove(item);
        }
    }

    public void removeItem(ItemStack item) {
        this.contentMap.forEach((slot, itemStack) -> {
            if (item.equals(itemStack)) {
                this.contentMap.remove(slot);
                this.contentList.remove(item);
            }
        });
    }

    public boolean hasExactItem(ItemStack item) {
        return this.contentList.contains(item);
    }

    public boolean hasItem(ItemStack item) {
        return this.contentList.contains(item);
    }

    public boolean hasMaterial(Material material) {
        for (ItemStack item : this.contentList) {
            if (item.getType() == material) {
                return true;
            }
        }
        return false;
    }

    public int itemCount() {
        return this.contentList.size();
    }
}
