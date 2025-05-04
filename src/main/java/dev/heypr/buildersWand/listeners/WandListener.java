package dev.heypr.buildersWand.listeners;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.Wand;
import dev.heypr.buildersWand.managers.ConfigManager;
import dev.heypr.buildersWand.managers.PlacementQueueManager;
import dev.heypr.buildersWand.managers.WandManager;
import dev.heypr.buildersWand.managers.WandSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.*;

public class WandListener implements Listener {
    private static WandListener instance;
    private final Set<Block> extrudedBlocks = new HashSet<>();
    private final Map<UUID, WandSession> sessions = new HashMap<>();
    private static final Component PREFIX = Component.text("[BuildersWand] ").color(NamedTextColor.AQUA);
    NamespacedKey itemKey = new NamespacedKey(BuildersWand.getInstance(), "builderswand");

    public WandListener() {
        instance = this;
    }

    public static WandListener getInstance() {
        return instance;
    }

    private WandSession getSession(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), k -> new WandSession());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getItemInHand().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMoveEvent(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        ItemStack wandItem = player.getInventory().getItemInMainHand();
        if (!WandManager.isWand(wandItem)) {
            sessions.remove(player.getUniqueId());
            return;
        }

        Wand wand = WandManager.getWand(wandItem);

        if (wand.generatePreviewOnMove()) generatePreview(player, wand);
    }

    @EventHandler
    public void onPlayerRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack wandItem = player.getInventory().getItemInMainHand();
        if (!WandManager.isWand(wandItem)) return;

        Wand wand = WandManager.getWand(wandItem);
        if (!event.getAction().isRightClick() || event.getHand() != EquipmentSlot.HAND) return;

        event.setCancelled(true);
        WandSession session = getSession(player);

        if (session.placing) {
            player.sendActionBar(PREFIX.append(Component.text("Wand is still placing blocks, please wait...").color(NamedTextColor.RED)));
            return;
        }

        if (session.previewBlocks.isEmpty()) {
            generatePreview(player, wand);
            if (session.previewBlocks.isEmpty()) {
                player.sendActionBar(PREFIX.append(Component.text("No blocks available for preview.").color(NamedTextColor.RED)));
            }
            else {
                player.sendActionBar(PREFIX.append(Component.text("Preview generated. Right-click again to place.").color(NamedTextColor.YELLOW)));
            }
            return;
        }

        long now = System.currentTimeMillis();
        long last = session.lastRightClickTime;
        float cooldown = wand.getCooldown() * 1000L;

        if (now - last < cooldown) {
            player.sendActionBar(PREFIX.append(Component.text("Please wait " + (int)((cooldown - (now - last)) / 1000) + " seconds before using the wand again.").color(NamedTextColor.RED)));
            return;
        }

        if (!player.getGameMode().equals(GameMode.CREATIVE) && wand.consumesItems()) {
            int available = getItemCount(player, session.lastTargetBlock.getType());
            int needed = session.previewBlocks.size();

            if (available < needed) {
                player.sendMessage(PREFIX.append(Component.text("You need " + (needed - available) + " more " + session.lastTargetBlock.getType().name() + " blocks.").color(NamedTextColor.RED)));
                return;
            }
            removeItems(player, session.lastTargetBlock.getType(), needed);
        }

        session.lastRightClickTime = now;

        if (ConfigManager.isPlacementQueueEnabled()) {
            new PlacementQueueManager(player, session.previewBlocks, session.lastTargetBlock.getType(), ConfigManager.getMaxBlocksPerTick()).start();
        }
        else {
            for (Block block : session.previewBlocks) {
                block.setType(session.lastTargetBlock.getType(), true);
            }
        }

        BlockPlaceEvent bpe = new BlockPlaceEvent(session.lastTargetBlock, session.lastTargetBlock.getState(), session.lastTargetBlock, new ItemStack(session.lastTargetBlock.getType()), player, true, EquipmentSlot.HAND);
        Bukkit.getServer().getPluginManager().callEvent(bpe);

        session.previewBlocks.clear();
        session.lastTargetBlock = null;
        session.lastTargetFace = null;

        if (WandManager.isWandDurabilityEnabled(wandItem)) {
            if (WandManager.getWandDurability(wandItem) <= 1) {
                player.getInventory().removeItem(wandItem);
            }
            else {
                WandManager.decrementWandDurability(wandItem);
            }
        }
        else {
            WandManager.handleInfiniteDurability(wandItem);
        }

        if (!wand.generatePreviewOnMove()) {
            session.initialPlace = true;
        }
    }

    public void generatePreview(Player player, Wand wand) {
        RayTraceResult rtr = player.rayTraceBlocks(wand.getMaxRayTraceDistance(), FluidCollisionMode.NEVER);
        if (rtr == null || rtr.getHitBlock() == null || rtr.getHitBlockFace() == null) return;

        WandSession session = getSession(player);

        if (session.placing) return;


        Block hitBlock = rtr.getHitBlock();
        BlockFace face = rtr.getHitBlockFace();

        if (hitBlock.equals(session.lastTargetBlock) && face == session.lastTargetFace) return;

        session.previewBlocks.clear();
        session.lastTargetBlock = hitBlock;
        session.lastTargetFace = face;

        extrudedBlocks.clear();

        getExtrudedPlane(hitBlock, face, hitBlock.getType(), wand.getMaxSize());

        ItemStack item = new ItemStack(hitBlock.getType());
        item.editMeta(meta -> meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1));

        for (Block b : extrudedBlocks) {
            Block target = b.getRelative(face);
            if (BuildersWand.isSkyblockEnabled() && !isInsideIsland(target.getLocation())) continue;
            BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, target.getState(), hitBlock, item, player, true, EquipmentSlot.HAND);
            Bukkit.getServer().getPluginManager().callEvent(blockPlaceEvent);

            if ((target.getType().isAir() || isReplaceable(target.getType()))
                    && target.getType() == Material.AIR) {
                session.previewBlocks.add(target);
            }
        }

        long now = System.currentTimeMillis();
        long last = session.lastRightClickTime;
        float cooldown = wand.getCooldown() * 1000L;

        if (now - last < cooldown) {
            player.sendActionBar(PREFIX.append(Component.text("Please wait " + (int)((cooldown - (now - last)) / 1000) + " seconds before using the wand again.").color(NamedTextColor.RED)));
            return;
        }

        if (session.particleTask != null) {
            session.particleTask.cancel();
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                if (session.previewBlocks.isEmpty()) {
                    this.cancel();
                    return;
                }

                ItemStack wandItem = player.getInventory().getItemInMainHand();
                if (!WandManager.isWand(wandItem)) this.cancel();

                World world = hitBlock.getWorld();
                List<Block> locations = new ArrayList<>(session.previewBlocks);

                for (Block preview : locations) {
                    Location loc = preview.getLocation();
                    world.spawnParticle(
                            Particle.DUST,
                            loc.add(0.5, 0.5, 0.5),
                            1,
                            new Particle.DustOptions(Color.WHITE, 1)
                    );
                }
            }
        };
        task.runTaskTimer(BuildersWand.getInstance(), 0L, 5L);
        session.particleTask = task;
    }

    private boolean isInsideIsland(Location location) {
        Island island = SuperiorSkyblockAPI.getIslandAt(location);
        return island != null && island.isInsideRange(location);
    }

    public void getExtrudedPlane(Block origin, BlockFace face, Material type, int max) {
        Deque<Block> stack = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();
        stack.push(origin);
        visited.add(origin);

        while (!stack.isEmpty() && visited.size() < max) {
            Block current = stack.pop();
            extrudedBlocks.add(current);
            for (BlockFace dir : getPlaneDirections(face)) {
                Block neighbor = current.getRelative(dir);
                if (!visited.contains(neighbor) && neighbor.getType().equals(type)) {
                    stack.push(neighbor);
                    visited.add(neighbor);
                }
            }
        }
    }

    public void unlockPlayer(Player player) {
        getSession(player).placing = false;
    }

    public int getItemCount(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) continue;

            int amt = item.getAmount();
            if (amt <= remaining) {
                remaining -= amt;
                item.setAmount(0);
            }
            else {
                item.setAmount(amt - remaining);
                break;
            }
        }
    }

    public BlockFace[] getPlaneDirections(BlockFace clickedFace) {
        return switch (clickedFace) {
            case UP, DOWN -> new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
            case NORTH, SOUTH -> new BlockFace[]{BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
            case EAST, WEST -> new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN};
            default -> new BlockFace[0];
        };
    }

    public static final Set<Material> REPLACEABLE = EnumSet.of(
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR,
            Material.SHORT_GRASS,
            Material.TALL_GRASS,
            Material.FERN,
            Material.LARGE_FERN,
            Material.DEAD_BUSH,
            Material.DANDELION,
            Material.POPPY,
            Material.BLUE_ORCHID,
            Material.ALLIUM,
            Material.AZURE_BLUET,
            Material.RED_TULIP,
            Material.ORANGE_TULIP,
            Material.WHITE_TULIP,
            Material.PINK_TULIP,
            Material.OXEYE_DAISY,
            Material.CORNFLOWER,
            Material.LILY_OF_THE_VALLEY,
            Material.SUNFLOWER,
            Material.SWEET_BERRY_BUSH,
            Material.SNOW,
            Material.SEAGRASS,
            Material.TALL_SEAGRASS,
            Material.MOSS_CARPET
    );

    private boolean isReplaceable(Material material) {
        return REPLACEABLE.contains(material);
    }
}
