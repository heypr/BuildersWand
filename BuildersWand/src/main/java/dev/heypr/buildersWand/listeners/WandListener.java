package dev.heypr.buildersWand.listeners;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.api.events.WandPlaceEvent;
import dev.heypr.buildersWand.api.events.WandPreviewEvent;
import dev.heypr.buildersWand.hooks.BentoBoxHook;
import dev.heypr.buildersWand.hooks.LandsHook;
import dev.heypr.buildersWand.hooks.SuperiorSkyblockHook;
import dev.heypr.buildersWand.hooks.WorldGuardHook;
import dev.heypr.buildersWand.managers.*;
import dev.heypr.buildersWand.utility.Util;
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

        if (!player.hasPermission("builderswand.use." + wand.getId()) && !player.hasPermission("builderswand.use.*")) {
            player.sendActionBar(Util.toPrefixedComponent("&4You do not have permission to use this wand."));
            return;
        }

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
                session.previewBlocks.clear();
                session.lastTargetBlock = null;
                session.lastTargetFace = null;
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
                if (wand.isBreakSoundEnabled()) {
                    if (wand.getBreakSound() == null) {
                        Util.error("Break sound not configured for wand " + wand.getId() + ". Using default sound.");
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    }
                    else {
                        player.playSound(player.getLocation(), wand.getBreakSound(), 1.0f, 1.0f);
                    }
                    player.sendActionBar(wand.getBreakSoundMessage());
                }
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
        if (ConfigManager.shouldDestroyInvalidWands()) {
            Util.error("Removing misconfigured wand from their inventory...");
            player.getInventory().removeItem(wandItem);
            player.sendMessage(Util.toPrefixedComponent(ConfigManager.getInvalidWandMessage()));

        }
        else {
            Util.error("Misconfigured wand not removed due to configuration option.");
        }

        event.setCancelled(true);

        WandSession session = getSession(player);
        if (session.undoHistory.isEmpty()) {
            player.sendActionBar(Util.toPrefixedComponent("&cNothing to undo!"));
            return;
        }

        List<BlockState> lastAction = session.undoHistory.pop();
        List<ItemStack> itemsToReturn = new ArrayList<>();

        for (BlockState oldState : lastAction) {
            Block currentBlock = oldState.getBlock();

            if (wand.consumesItems() && !currentBlock.getType().isAir()) {
                itemsToReturn.add(new ItemStack(currentBlock.getType()));
            }
            oldState.update(true, false);
        }

        if (!itemsToReturn.isEmpty() && !player.getGameMode().isInvulnerable()) player.give(itemsToReturn.toArray(new ItemStack[0]));

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
        if (BuildersWand.isBentoBoxEnabled()) {
            if (!BentoBoxHook.canBuild(player, target.getLocation())) return false;
        }
        if (BuildersWand.isSuperiorSkyblockEnabled()) {
            if (!SuperiorSkyblockHook.canBuild(player, target.getLocation())) return false;
        }
        if (BuildersWand.isLandsEnabled()) {
            if (!LandsHook.canBuild(player, target.getLocation(), BuildersWand.getInstance())) return false;
        }
        if (BuildersWand.isWorldGuardEnabled()) {
            if (!WorldGuardHook.canBuild(player, target.getLocation())) return false;
        }
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
                    session.previewBlocks.clear();
                    session.lastTargetBlock = null;
                    session.lastTargetFace = null;
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
