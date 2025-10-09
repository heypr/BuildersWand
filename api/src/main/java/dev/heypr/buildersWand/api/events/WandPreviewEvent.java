package dev.heypr.buildersWand.api.events;

import dev.heypr.buildersWand.api.Wand;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Called when a wand is about to display a block placement preview.
 * This event allows for modification of the previewed blocks or cancellation of the preview display.
 */
public class WandPreviewEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Wand wand;
    private Set<Block> previewBlocks;
    private boolean isCancelled;

    public WandPreviewEvent(Player player, Wand wand, Set<Block> previewBlocks) {
        super(player);
        this.wand = wand;
        this.previewBlocks = previewBlocks;
        this.isCancelled = false;
    }

    /**
     * @return The configuration of the wand being used.
     */
    public Wand getWand() {
        return wand;
    }

    /**
     * Gets the set of blocks that are about to be shown in the preview.
     * This set is mutable, allowing for modification (add/remove blocks).
     *
     * @return A mutable set of blocks to be previewed.
     */
    public Set<Block> getPreviewBlocks() {
        return previewBlocks;
    }

    /**
     * Sets the blocks that will be shown in the preview.
     *
     * @param previewBlocks The new set of blocks to be previewed.
     */
    public void setPreviewBlocks(Set<Block> previewBlocks) {
        this.previewBlocks = previewBlocks;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}
