package dev.heypr.buildersWand.listeners;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.Util;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.api.events.WandPlaceEvent;
import dev.heypr.buildersWand.api.events.WandPreviewEvent;
import dev.heypr.buildersWand.managers.ConfigManager;
import dev.heypr.buildersWand.managers.PlacementQueueManager;
import dev.heypr.buildersWand.managers.WandManager;
import dev.heypr.buildersWand.managers.WandSession;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.type.Flags;
import me.angeschossen.lands.api.land.LandWorld;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.*;

public class WandListener implements Listener {
    private static WandListener instance;
    private final Set<Block> extrudedBlocks = new HashSet<>();
    private final Map<UUID, WandSession> sessions = new HashMap<>();

    public WandListener() {
        instance = this;
    }

    public static WandListener getInstance() {
        return instance;
    }

    private WandSession getSession(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), k -> new WandSession());
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (WandManager.isWand(item) && !WandManager.getWand(item).isCraftable()) {
                event.getInventory().setResult(null);
                break;
            }
        }
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        ItemStack item = event.getResult();
        if (WandManager.isWand(item) && !WandManager.getWand(item).isCraftable()) {
            event.setResult(new ItemStack(Material.AIR));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        ItemStack item = event.getFuel();
        if (WandManager.isWand(item) && !WandManager.getWand(item).isCraftable()) {
            event.setConsumeFuel(false);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFuranceSmelt(FurnaceSmeltEvent event) {
        ItemStack item = event.getResult();
        if (WandManager.isWand(item) && !WandManager.getWand(item).isCraftable()) {
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

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack wandItem = player.getInventory().getItemInMainHand();
        if (!WandManager.isWand(wandItem)) return;

        Wand wand = WandManager.getWand(wandItem);
        if (!event.getAction().isRightClick() || event.getHand() != EquipmentSlot.HAND) return;

        event.setCancelled(true);
        WandSession session = getSession(player);

        if (session.placing) {
            player.sendActionBar(Util.toPrefixedComponent("&4Wand is still placing blocks, please wait..."));
            return;
        }

        if (session.previewBlocks.isEmpty()) {
            generatePreview(player, wand);
            if (session.previewBlocks.isEmpty()) {
                player.sendActionBar(Util.toPrefixedComponent("&4No blocks available for preview."));
            }
            else {
                player.sendActionBar(Util.toPrefixedComponent("&ePreview generated. Right click again to place."));
            }
            return;
        }

        long now = System.currentTimeMillis();
        long last = session.lastRightClickTime;
        float cooldown = wand.getCooldown() * 1000L;

        if (now - last < cooldown) {
            player.sendActionBar(Util.toPrefixedComponent("&4Please wait " + (int) ((cooldown - (now - last)) / 1000) + " seconds before using the wand again."));
            return;
        }

        int needed = session.previewBlocks.size();
        if (!player.getGameMode().equals(GameMode.CREATIVE) && wand.consumesItems()) {
            int available = getItemCount(player, session.lastTargetBlock.getType());
            if (available < needed) {
                player.sendMessage(Util.toPrefixedComponent("&4You need " + (needed - available) + " more " + session.lastTargetBlock.getType().name() + " blocks."));
                return;
            }
        }

        session.lastRightClickTime = now;

        BlockPlaceEvent bpe = new BlockPlaceEvent(session.lastTargetBlock, session.lastTargetBlock.getState(), session.lastTargetBlock, new ItemStack(session.lastTargetBlock.getType()), player, true, EquipmentSlot.HAND);
        Bukkit.getServer().getPluginManager().callEvent(bpe);

        if (bpe.isCancelled()) {
            player.sendActionBar(Util.toPrefixedComponent("&4Disallowed."));
            return;
        }

        Set<Block> finalBlocksToPlace = session.previewBlocks;

        if (ConfigManager.shouldFireBlockPlaceEvent()) {
            WandPlaceEvent wandEvent = new WandPlaceEvent(player, wandItem, wand, session.previewBlocks);
            Bukkit.getPluginManager().callEvent(wandEvent);

            if (wandEvent.isCancelled()) {
                return;
            }

            finalBlocksToPlace = wandEvent.getBlocksToPlace();
        }

        if (!player.getGameMode().equals(GameMode.CREATIVE) && wand.consumesItems()) {
            removeItems(player, session.lastTargetBlock.getType(), needed);
        }

        if (ConfigManager.isPlacementQueueEnabled()) {
            new PlacementQueueManager(player, finalBlocksToPlace, session.lastTargetBlock.getType(), ConfigManager.getMaxBlocksPerTick()).start();
        }
        else {
            for (Block block : finalBlocksToPlace) {
                block.setType(session.lastTargetBlock.getType(), true);
            }
        }

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
        if (wand.getBlockedMaterials().contains(hitBlock.getType())) return;

        session.previewBlocks.clear();
        session.lastTargetBlock = hitBlock;
        session.lastTargetFace = face;

        extrudedBlocks.clear();

        getExtrudedPlane(hitBlock, face, hitBlock.getType(), wand.getMaxSize());

        for (Block b : extrudedBlocks) {
            Block target = b.getRelative(face);
            if (BuildersWand.isSkyblockEnabled() && !isInsideIsland(target.getLocation())) continue;
            if (BuildersWand.isLandsEnabled() && !isInsideLand(player, target.getLocation())) continue;
            if (BuildersWand.isWorldGuardEnabled() && !checkWG(player, target.getLocation())) continue;

            if (target.getType().isAir() && isReplaceable(target.getType())) {
                session.previewBlocks.add(target);
            }
        }

        if (ConfigManager.shouldFireWandPreviewEvent()) {
            WandPreviewEvent previewEvent = new WandPreviewEvent(player, wand, session.previewBlocks);
            Bukkit.getPluginManager().callEvent(previewEvent);

            session.previewBlocks = previewEvent.getPreviewBlocks();

            if (previewEvent.isCancelled()) {
                return;
            }
        }

        long now = System.currentTimeMillis();
        long last = session.lastRightClickTime;
        float cooldown = wand.getCooldown() * 1000L;

        if (now - last < cooldown) {
            player.sendActionBar(Util.toPrefixedComponent("&4Please wait " + (int)((cooldown - (now - last)) / 1000) + " seconds before using the wand again."));
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
                    world.spawnParticle(Particle.DUST, loc.add(0.5, 0.5, 0.5), 1, new Particle.DustOptions(Color.WHITE, 1));
                }
            }
        };
        task.runTaskTimer(BuildersWand.getInstance(), 0L, 5L);
        session.particleTask = task;
    }

    private boolean checkWG(Player player, Location location) {
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        com.sk89q.worldedit.util.Location wgLocation = BukkitAdapter.adapt(location);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        return query.testBuild(wgLocation, localPlayer);
    }

    private boolean isInsideIsland(Location location) {
        Island island = SuperiorSkyblockAPI.getIslandAt(location);
        return island != null && island.isInsideRange(location);
    }

    private boolean isInsideLand(Player player, Location location) {
        LandsIntegration api = LandsIntegration.of(BuildersWand.getInstance());
        LandWorld world = api.getWorld(location.getWorld());
        if (world == null) return false;
        return world.hasRoleFlag(player.getUniqueId(), location, Flags.BLOCK_PLACE);
    }

    public void getExtrudedPlane(Block origin, BlockFace face, Material type, int max) {
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && visited.size() < max) {
            Block current = queue.poll();
            extrudedBlocks.add(current);
            for (BlockFace dir : getPlaneDirections(face)) {
                Block neighbor = current.getRelative(dir);
                if (!visited.contains(neighbor) && neighbor.getType().equals(type)) {
                    queue.add(neighbor);
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
