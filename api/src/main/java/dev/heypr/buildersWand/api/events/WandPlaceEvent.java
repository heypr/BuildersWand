package dev.heypr.buildersWand.api.events;

import dev.heypr.buildersWand.api.Wand;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Called just before a BuildersWand places blocks.
 * This event is cancellable and allows modification of the blocks to be placed.
 */
public class WandPlaceEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final ItemStack wandItem;
    private final Wand wand;
    private Set<Block> blocksToPlace;
    private boolean isCancelled;

    public WandPlaceEvent(Player player, ItemStack wandItem, Wand wand, Set<Block> blocksToPlace) {
        super(player);
        this.wandItem = wandItem;
        this.wand = wand;
        this.blocksToPlace = blocksToPlace;
        this.isCancelled = false;
    }

    /**
     * @return The ItemStack of the wand being used.
     */
    public ItemStack getWandItem() {
        return wandItem;
    }

    /**
     * @return The configuration object for the wand being used.
     */
    public Wand getWand() {
        return wand;
    }

    /**
     * Gets the set of blocks that are about to be placed.
     * This set is mutable, allowing modification (add/remove blocks).
     *
     * @return A mutable set of blocks to be placed.
     */
    public Set<Block> getBlocksToPlace() {
        return blocksToPlace;
    }

    /**
     * Sets the blocks that the wand will place.
     *
     * @param blocksToPlace The new set of blocks to be placed.
     */
    public void setBlocksToPlace(Set<Block> blocksToPlace) {
        this.blocksToPlace = blocksToPlace;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
