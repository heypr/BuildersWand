package dev.heypr.buildersWand.managers;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class WandSession {
    public Set<Block> previewBlocks = new HashSet<>();
    public Block lastTargetBlock;
    public BlockFace lastTargetFace;
    public BukkitRunnable particleTask;
    public long lastRightClickTime = 0L;
    public boolean placing = false;
}
