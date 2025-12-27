package dev.heypr.buildersWand.listeners;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.Util;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.api.events.WandPlaceEvent;
import dev.heypr.buildersWand.api.events.WandPreviewEvent;
import dev.heypr.buildersWand.managers.*;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.type.Flags;
import me.angeschossen.lands.api.land.LandWorld;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
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
import java.util.concurrent.CompletableFuture;

public class WandListener implements Listener {
    private static WandListener instance;
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
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        ItemStack item = event.getResult();
        if (WandManager.isWand(item) && !WandManager.getWand(item).isCraftable()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMoveEvent(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ItemStack wandItem = player.getInventory().getItemInMainHand();
        if (!WandManager.isWand(wandItem)) return;
        Wand wand = WandManager.getWand(wandItem);
        if (wand != null && wand.generatePreviewOnMove()) {
            generatePreview(player, wand);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack wandItem = player.getInventory().getItemInMainHand();
        if (!WandManager.isWand(wandItem)) return;
        Wand wand = WandManager.getWand(wandItem);
        if (wand == null) {
            Util.error("Wand configuration not found for item held by player " + player.getName());
            Util.error("Wand internal name: " + WandManager.getWandID(wandItem));
            if (ConfigManager.shouldDestroyInvalidWands()) {
                Util.error("Removing misconfigured wand from their inventory...");
                player.getInventory().removeItem(wandItem);
                player.sendMessage(Util.toPrefixedComponent(ConfigManager.getInvalidWandMessage()));
            }
            else {
                Util.error("Misconfigured wand not removed due to configuration option.");
            }
            return;
        }

        if (!event.getAction().isRightClick() || event.getHand() != EquipmentSlot.HAND) return;

        event.setCancelled(true);
        WandSession session = getSession(player);

        if (session.placing) {
            player.sendActionBar(Util.toPrefixedComponent("&4Wand is still placing blocks, please wait..."));
            return;
        }

        if (session.previewBlocks.isEmpty()) {
            generatePreview(player, wand);
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

        Set<Block> finalBlocksToPlace = new HashSet<>(session.previewBlocks);
        if (ConfigManager.shouldFireBlockPlaceEvent()) {
            WandPlaceEvent wandEvent = new WandPlaceEvent(player, wandItem, wand, session.previewBlocks);
            Bukkit.getPluginManager().callEvent(wandEvent);
            if (wandEvent.isCancelled()) return;
            finalBlocksToPlace = wandEvent.getBlocksToPlace();
        }

        if (wand.getUndoHistorySize() != 0) {
            List<BlockState> currentAction = new ArrayList<>();
            for (Block block : finalBlocksToPlace) {
                currentAction.add(block.getState());
            }
            session.undoHistory.push(currentAction);
        }

        if (session.undoHistory.size() > wand.getUndoHistorySize() && wand.getUndoHistorySize() != -1) {
            session.undoHistory.removeFirst();
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
                // todo: add configurable break sound
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

    @EventHandler(ignoreCancelled = true)
    public void onUndoAction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.getAction().isLeftClick() || event.getHand() != EquipmentSlot.HAND) return;
        if (!player.isSneaking()) return;

        ItemStack wandItem = player.getInventory().getItemInMainHand();
        if (!WandManager.isWand(wandItem)) return;
        Wand wand = WandManager.getWand(wandItem);
        if (wand == null) {
            Util.error("Wand configuration not found for item held by player " + player.getName());
            Util.error("Wand internal name: " + WandManager.getWandID(wandItem));
            Util.error("Removing misconfigured wand from their inventory...");
            player.getInventory().removeItem(wandItem);
            player.sendMessage(Util.toPrefixedComponent("&4The wand you had was misconfigured and has been removed. Please contact an administrator immediately."));
            return;
        }

        event.setCancelled(true);

        WandSession session = getSession(player);
        if (session.undoHistory.isEmpty()) {
            player.sendActionBar(Util.toPrefixedComponent("&cNothing to undo!"));
            return;
        }

        List<BlockState> lastAction = session.undoHistory.pop();

        if (wand.consumesItems()) {
            List<ItemStack> itemsToReturn = new ArrayList<>();

            lastAction.forEach(blockState -> {
                itemsToReturn.add(new ItemStack(blockState.getType()));
            });

            player.give(itemsToReturn);
        }

        player.sendActionBar(Util.toPrefixedComponent("&aAction undone! " + session.undoHistory.size() + " undoes remaining."));
    }

    public void generatePreview(Player player, Wand wand) {
        RayTraceResult rtr = player.rayTraceBlocks(wand.getMaxRayTraceDistance(), FluidCollisionMode.NEVER);
        if (rtr == null || rtr.getHitBlock() == null || rtr.getHitBlockFace() == null) return;

        WandSession session = getSession(player);

        if (session.placing) return;

        Block hitBlock = rtr.getHitBlock();
        BlockFace face = rtr.getHitBlockFace();
        if (hitBlock.equals(session.lastTargetBlock) && face == session.lastTargetFace && !session.previewBlocks.isEmpty()) return;
        if (wand.getBlockedMaterials().contains(hitBlock.getType())) return;

        if (session.currentCalculation != null && !session.currentCalculation.isDone()) {
            session.currentCalculation.cancel(true);
        }

        session.lastTargetBlock = hitBlock;
        session.lastTargetFace = face;

        session.currentCalculation = CompletableFuture.supplyAsync(() -> {
            if (wand.getWandType() == Wand.WandType.STATIC) {
                return BlockFinder.getStaticBlocks(hitBlock, face, wand.getStaticLength(), wand.getStaticWidth());
            }
            else {
                return BlockFinder.findConnectedBlocks(hitBlock, face, wand.getMaxSize(), wand.getBlockedMaterials());
            }
        }).thenAcceptAsync(sourceBlocks -> {
            Set<Block> validTargets = new HashSet<>();
            for (Block source : sourceBlocks) {
                Block target = source.getRelative(face);
                if (isValidLocation(player, target)) {
                    validTargets.add(target);
                }
            }

            Bukkit.getScheduler().runTask(BuildersWand.getInstance(), () -> {
                session.previewBlocks = validTargets;
                if (ConfigManager.shouldFireWandPreviewEvent()) {
                    WandPreviewEvent previewEvent = new WandPreviewEvent(player, wand, session.previewBlocks);
                    Bukkit.getPluginManager().callEvent(previewEvent);
                    session.previewBlocks = previewEvent.getPreviewBlocks();
                }

                if (session.particleTask == null || session.particleTask.isCancelled()) {
                    startParticleTask(player, wand, session);
                }
            });
        });
    }

    private boolean isValidLocation(Player player, Block target) {
        if (BuildersWand.isSkyblockEnabled() && !isInsideIsland(target.getLocation())) return false;
        if (BuildersWand.isLandsEnabled() && !isInsideLand(player, target.getLocation())) return false;
        if (BuildersWand.isWorldGuardEnabled() && !checkWG(player, target.getLocation())) return false;
        return BlockFinder.isReplaceable(target);
    }

    private void startParticleTask(Player player, Wand wand, WandSession session) {
        if (session.particleTask != null) session.particleTask.cancel();
        session.particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || session.previewBlocks.isEmpty()) {
                    this.cancel();
                    session.particleTask = null;
                    return;
                }
                ItemStack wandItem = player.getInventory().getItemInMainHand();
                if (!WandManager.isWand(wandItem)) {
                    this.cancel();
                    session.particleTask = null;
                    return;
                }
                World world = player.getWorld();
                Particle particle;
                try {
                    particle = Particle.valueOf(wand.getPreviewParticle());
                }
                catch (Exception e) {
                    particle = Particle.DUST;
                }
                Particle.DustOptions dustOptions = particle == Particle.DUST ? new Particle.DustOptions(Color.fromRGB(wand.getPreviewParticleOptionsRed(), wand.getPreviewParticleOptionsGreen(), wand.getPreviewParticleOptionsBlue()), (float) wand.getPreviewParticleOptionsSize()) : null;
                for (Block preview : session.previewBlocks) {
                    if (!preview.getWorld().equals(world)) continue;

                    Location loc = preview.getLocation().add(0.5, 0.5, 0.5);
                    if (dustOptions != null) {
                        world.spawnParticle(Particle.DUST, loc, wand.getPreviewParticleCount(), wand.getPreviewParticleOffsetX(), wand.getPreviewParticleOffsetY(), wand.getPreviewParticleOffsetZ(), wand.getPreviewParticleSpeed(), dustOptions);
                    }
                    else {
                        world.spawnParticle(particle, loc, wand.getPreviewParticleCount(), wand.getPreviewParticleOffsetX(), wand.getPreviewParticleOffsetY(), wand.getPreviewParticleOffsetZ(), wand.getPreviewParticleSpeed());
                    }
                }
            }
        };
        session.particleTask.runTaskTimer(BuildersWand.getInstance(), 0L, 5L);
    }

    private boolean checkWG(Player player, Location location) {
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        return query.testBuild(BukkitAdapter.adapt(location), localPlayer);
    }

    private boolean isInsideIsland(Location location) {
        Island island = SuperiorSkyblockAPI.getIslandAt(location);
        return island != null && island.isInsideRange(location);
    }

    private boolean isInsideLand(Player player, Location location) {
        LandsIntegration api = LandsIntegration.of(BuildersWand.getInstance());
        LandWorld world = api.getWorld(location.getWorld());
        return world != null && world.hasRoleFlag(player.getUniqueId(), location, Flags.BLOCK_PLACE);
    }

    public void unlockPlayer(Player player) {
        getSession(player).placing = false;
    }

    public int getItemCount(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) count += item.getAmount();
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
}
