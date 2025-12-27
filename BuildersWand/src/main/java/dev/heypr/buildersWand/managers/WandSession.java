package dev.heypr.buildersWand.managers;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;

public class WandSession {
    public CompletableFuture<Void> currentCalculation;
    public Set<Block> previewBlocks = new HashSet<>();
    public Stack<List<BlockState>> undoHistory = new Stack<>();
    public Block lastTargetBlock;
    public BlockFace lastTargetFace;
    public BukkitRunnable particleTask;
    public long lastRightClickTime = 0L;
    public boolean placing = false;
    public boolean initialPlace = true;
}
