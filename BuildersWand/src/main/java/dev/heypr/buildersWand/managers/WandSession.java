package dev.heypr.buildersWand.managers;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class WandSession {
    public CompletableFuture<Void> currentCalculation;
    public Set<Block> previewBlocks = new HashSet<>();
    public Deque<List<BlockState>> undoHistory = new ArrayDeque<>();
    public Block lastTargetBlock;
    public BlockFace lastTargetFace;
    public BukkitRunnable particleTask;
    public long lastRightClickTime = 0L;
    public boolean placing = false;
    public boolean initialPlace = true;
}
