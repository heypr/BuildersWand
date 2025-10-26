package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.Util;
import dev.heypr.buildersWand.listeners.WandListener;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class PlacementQueueManager {

    private final Queue<Block> blocksToPlace = new LinkedList<>();
    private final int size;
    private final BukkitRunnable task;

    public PlacementQueueManager(Player player, Set<Block> blocks, Material material, int maxPerTick) {
        this.blocksToPlace.addAll(blocks);
        this.size = blocksToPlace.size();

        this.task = new BukkitRunnable() {
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

                    block.setType(material, true);
                    placed++;
                }

                int remaining = blocksToPlace.size();
                if (remaining > 0 && remaining < 50) {
                    player.sendActionBar(Util.toPrefixedComponent("&7Placing blocks... &e" + remaining + " &7left"));
                }

                if (blocksToPlace.isEmpty()) {
                    if (size > 50) {
                        player.sendActionBar(Util.toPrefixedComponent("&aBlock placement complete!"));
                    }
                    WandListener.getInstance().unlockPlayer(player);
                    this.cancel();
                }
            }
        };
    }

    public void start() {
        task.runTaskTimer(BuildersWand.getInstance(), 1L, 1L);
    }

    private boolean isReplaceable(Material material) {
        return WandListener.REPLACEABLE.contains(material);
    }
}
