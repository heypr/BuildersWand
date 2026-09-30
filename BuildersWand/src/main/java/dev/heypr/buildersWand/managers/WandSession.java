package dev.heypr.buildersWand.managers;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class WandSession {
    public record TargetBlock(Block block, BlockFace face) {}

    public CompletableFuture<Void> currentCalculation;
    public Set<Block> previewBlocks = new HashSet<>();
    public Stack<List<BlockState>> undoHistory = new Stack<>();
    public TargetBlock target;
    public BukkitRunnable particleTask;
    public long lastRightClickTime = 0L;
    public boolean placing = false;
    public boolean initialPlace = true;
}
