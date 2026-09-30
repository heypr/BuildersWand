package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.listeners.WandUseListener;
import dev.heypr.buildersWand.managers.io.MessageManager;
import dev.heypr.buildersWand.utility.BlockFinderUtil;
import dev.heypr.buildersWand.utility.BlockPlacementUtil;
import dev.heypr.buildersWand.utility.InventoryUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class PlacementQueueManager {
    private final Queue<Block> blocksToPlace = new LinkedList<>();
    private final int size;
    private final BukkitRunnable task;

    public PlacementQueueManager(Player player, Set<Block> blocks, Material material, int maxPerTick, Block targetBlock, boolean fireEvents, Wand wand, ItemStack wandItem) {
        this.blocksToPlace.addAll(blocks);
        this.size = blocksToPlace.size();
        this.task = new BukkitRunnable() {
            private int skipped = 0;

            @Override
            public void run() {
                if (player == null || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                int placed = 0;
                while (!blocksToPlace.isEmpty() && placed < maxPerTick) {
                    Block block = blocksToPlace.poll();
                    if (block == null) continue;
                    if (!block.getType().isAir() && !isReplaceable(block.getType())) continue;
                    if (BlockPlacementUtil.place(block, material, targetBlock, player, fireEvents)) {
                        placed++;
                    }
                    else {
                        skipped++;
                    }
                }
                int remaining = blocksToPlace.size();
                if (remaining > 0 && remaining < 50) {
                    MessageManager.sendMessage(player, MessageManager.Messages.PLACING_BLOCKS, "remaining", String.valueOf(remaining));
                }
                if (blocksToPlace.isEmpty()) {
                    if (size > 50) {
                        MessageManager.sendMessage(player, MessageManager.Messages.PLACEMENT_COMPLETE);
                    }
                    if (skipped > 0 && wand.consumesItems()) {
                        InventoryUtil.returnItems(player, material, skipped, wand, wandItem);
                    }
                    WandUseListener.getInstance().unlockPlayer(player);
                    this.cancel();
                }
            }
        };
    }

    public void start() {
        task.runTaskTimer(BuildersWand.getInstance(), 1L, 1L);
    }

    private boolean isReplaceable(Material material) {
        return BlockFinderUtil.REPLACEABLE.contains(material);
    }
}
