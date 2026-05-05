package dev.heypr.buildersWand.listeners;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.api.events.WandPlaceEvent;
import dev.heypr.buildersWand.api.events.WandPreviewEvent;
import dev.heypr.buildersWand.hooks.BentoBoxHook;
import dev.heypr.buildersWand.hooks.LandsHook;
import dev.heypr.buildersWand.hooks.SuperiorSkyblockHook;
import dev.heypr.buildersWand.hooks.WorldGuardHook;
import dev.heypr.buildersWand.managers.PlacementQueueManager;
import dev.heypr.buildersWand.managers.WandManager;
import dev.heypr.buildersWand.managers.WandSession;
import dev.heypr.buildersWand.managers.io.ConfigManager;
import dev.heypr.buildersWand.managers.io.MessageManager;
import dev.heypr.buildersWand.utility.BlockFinderUtil;
import dev.heypr.buildersWand.utility.ComponentUtil;
import dev.heypr.buildersWand.utility.InventoryUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class WandUseListener implements Listener {
    private static WandUseListener instance;
    private final Map<UUID, WandSession> sessions = new HashMap<>();

    public WandUseListener() {
        instance = this;
    }

    public static WandUseListener getInstance() {
        return instance;
    }

    private WandSession getSession(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), k -> new WandSession());
    }

    @EventHandler
    public void onMoveEvent(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ItemStack wandItem = player.getInventory().getItemInMainHand();
        if (!WandManager.isWand(wandItem)) {
            return;
        }
        Wand wand = WandManager.getWand(wandItem);
        if (wand != null && wand.generatePreviewOnMove()) {
            generatePreview(player, wand);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack wandItem = player.getInventory().getItemInMainHand();
        if (!WandManager.isWand(wandItem)) {
            return;
        }
        Wand wand = WandManager.getWand(wandItem);
        if (wand == null) {
            handleMisconfiguredWand(player, wandItem);
            return;
        }
        if (!event.getAction().isRightClick() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        event.setCancelled(true);
        if (!player.hasPermission("builderswand.use." + wand.getId()) && !player.hasPermission("builderswand.use.*")) {
            MessageManager.sendMessage(player, MessageManager.Messages.NO_PERMISSION, "wand_id", wand.getId());
            return;
        }
        WandSession session = getSession(player);
        if (session.placing) {
            MessageManager.sendActionBar(player, MessageManager.Messages.STILL_PLACING);
            return;
        }
        if (session.previewBlocks.isEmpty()) {
            generatePreview(player, wand);
            return;
        }
        handlePlacement(player, wand, session);
    }

    private void handleMisconfiguredWand(Player player, ItemStack wandItem) {
        if (ConfigManager.shouldDestroyInvalidWands()) {
            ComponentUtil.error("Removing misconfigured wand from their inventory...");
            player.getInventory().removeItem(wandItem);
            MessageManager.sendMessage(player, MessageManager.Messages.MISCONFIGURED);
        }
        else {
            ComponentUtil.error("Misconfigured wand not removed due to configuration option.");
        }
    }

    private void handlePlacement(Player player, Wand wand, WandSession session) {
        long now = System.currentTimeMillis();
        long last = session.lastRightClickTime;
        long cooldown = (long) (wand.getCooldown() * 1000L);
        if (now - last < cooldown) {
            MessageManager.sendActionBar(player, MessageManager.Messages.COOLDOWN_ACTIVE, "seconds", String.valueOf((int) ((cooldown - (now - last)) / 1000)));
            return;
        }
        int needed = session.previewBlocks.size();
        if (!player.getGameMode().equals(GameMode.CREATIVE) && wand.consumesItems()) {
            int available = InventoryUtil.getItemCount(player, session.lastTargetBlock.getType());
            if (available < needed) {
                MessageManager.sendMessage(player, MessageManager.Messages.INSUFFICIENT_BLOCKS, "needed", needed - available, "material", session.lastTargetBlock.getType().name());
                session.previewBlocks.clear();
                session.lastTargetBlock = null;
                session.lastTargetFace = null;
                return;
            }
        }
        session.lastRightClickTime = now;
        BlockPlaceEvent bpe = new BlockPlaceEvent(
                session.lastTargetBlock,
                session.lastTargetBlock.getState(),
                session.lastTargetBlock,
                new ItemStack(session.lastTargetBlock.getType()),
                player,
                true,
                EquipmentSlot.HAND
        );
        Bukkit.getServer().getPluginManager().callEvent(bpe);
        if (bpe.isCancelled()) {
            MessageManager.sendMessage(player, MessageManager.Messages.PLACEMENT_DISALLOWED);
            return;
        }
        Set<Block> finalBlocksToPlace = new HashSet<>(session.previewBlocks);
        if (ConfigManager.shouldFireBlockPlaceEvent()) {
            WandPlaceEvent wandEvent = new WandPlaceEvent(player, player.getInventory().getItemInMainHand(), wand, session.previewBlocks);
            Bukkit.getPluginManager().callEvent(wandEvent);
            if (wandEvent.isCancelled()) return;
            finalBlocksToPlace = wandEvent.getBlocksToPlace();
        }
        storeUndoHistory(wand, session, finalBlocksToPlace);
        if (!player.getGameMode().equals(GameMode.CREATIVE) && wand.consumesItems()) {
            InventoryUtil.removeItems(player, session.lastTargetBlock.getType(), needed);
        }
        placeBlocks(player, session, finalBlocksToPlace);
        handleDurability(player, wand, player.getInventory().getItemInMainHand());
        if (!wand.generatePreviewOnMove()) {
            session.initialPlace = true;
        }
    }

    private void storeUndoHistory(Wand wand, WandSession session, Set<Block> blocks) {
        if (wand.getUndoHistorySize() == 0) return;
        List<BlockState> currentAction = new ArrayList<>();
        for (Block block : blocks) {
            currentAction.add(block.getState());
        }
        session.undoHistory.push(currentAction);
        if (session.undoHistory.size() > wand.getUndoHistorySize() && wand.getUndoHistorySize() != -1) {
            session.undoHistory.removeFirst();
        }
    }

    private void placeBlocks(Player player, WandSession session, Set<Block> blocks) {
        if (ConfigManager.isPlacementQueueEnabled()) {
            new PlacementQueueManager(player, blocks, session.lastTargetBlock.getType(), ConfigManager.getMaxBlocksPerTick()).start();
        }
        else {
            for (Block block : blocks) {
                block.setType(session.lastTargetBlock.getType(), true);
            }
        }
        session.previewBlocks.clear();
        session.lastTargetBlock = null;
        session.lastTargetFace = null;
    }

    private void handleDurability(Player player, Wand wand, ItemStack wandItem) {
        if (!WandManager.isWandDurabilityEnabled(wandItem)) {
            WandManager.handleInfiniteDurability(wandItem);
            return;
        }
        if (WandManager.getWandDurability(wandItem) <= 1) {
            player.getInventory().removeItem(wandItem);
            if (wand.isBreakSoundEnabled()) {
                playBreakSound(player, wand);
            }
        }
        else {
            WandManager.decrementWandDurability(wandItem);
        }
    }

    private void playBreakSound(Player player, Wand wand) {
        Sound sound = wand.getBreakSound();
        if (sound == null) {
            ComponentUtil.error("Break sound not configured for wand " + wand.getId() + ". Using default sound.");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        }
        else {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
        player.sendActionBar(wand.getBreakSoundMessage());
    }

    @EventHandler(ignoreCancelled = true)
    public void onUndoAction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.getAction().isLeftClick() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!player.isSneaking()) {
            return;
        }
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (!WandManager.isWand(itemInHand)) {
            return;
        }
        if (!WandManager.isRegisteredWand(itemInHand)) {
            if (ConfigManager.shouldDestroyInvalidWands()) {
                ComponentUtil.error("Removing misconfigured wand from their inventory...");
                player.getInventory().removeItem(itemInHand);
                MessageManager.sendMessage(player, MessageManager.Messages.MISCONFIGURED);
            }
            else {
                ComponentUtil.error("Misconfigured wand not removed due to configuration option.");
            }
        }
        Wand wand = WandManager.getWand(itemInHand);
        if (wand == null) {
            return;
        }
        if (!wand.canBreakBlocksWhileCrouched()) {
            event.setCancelled(true);
        }
        WandSession session = getSession(player);
        if (session.undoHistory.isEmpty()) {
            MessageManager.sendActionBar(player, MessageManager.Messages.NOTHING_TO_UNDO);
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
        if (!itemsToReturn.isEmpty()) {
            InventoryUtil.returnItems(player, itemsToReturn);
        }
        MessageManager.sendActionBar(player, MessageManager.Messages.ACTION_UNDONE, "remaining", session.undoHistory.size());
    }

    public void generatePreview(Player player, Wand wand) {
        RayTraceResult rtr = player.rayTraceBlocks(wand.getMaxRayTraceDistance(), FluidCollisionMode.NEVER);
        if (rtr == null || rtr.getHitBlock() == null || rtr.getHitBlockFace() == null) {
            return;
        }
        WandSession session = getSession(player);
        if (session.placing) {
            return;
        }
        Block hitBlock = rtr.getHitBlock();
        BlockFace face = rtr.getHitBlockFace();
        if (hitBlock.equals(session.lastTargetBlock) && face == session.lastTargetFace && !session.previewBlocks.isEmpty()) {
            return;
        }
        if (wand.getBlockedMaterials().contains(hitBlock.getType())) {
            return;
        }
        if (session.currentCalculation != null && !session.currentCalculation.isDone()) {
            session.currentCalculation.cancel(true);
        }
        session.lastTargetBlock = hitBlock;
        session.lastTargetFace = face;
        session.currentCalculation = CompletableFuture.supplyAsync(() -> {
            if (wand.getWandType() == Wand.WandType.STATIC) {
                return BlockFinderUtil.getStaticBlocks(hitBlock, face, wand.getStaticLength(), wand.getStaticWidth());
            }
            else {
                return BlockFinderUtil.findConnectedBlocks(hitBlock, face, wand.getMaxSize(), wand.getBlockedMaterials());
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
            if (!BentoBoxHook.canBuild(player, target.getLocation())) {
                return false;
            }
        }
        if (BuildersWand.isSuperiorSkyblockEnabled()) {
            if (!SuperiorSkyblockHook.canBuild(player, target.getLocation())) {
                return false;
            }
        }
        if (BuildersWand.isLandsEnabled()) {
            if (!LandsHook.canBuild(player, target.getLocation(), BuildersWand.getInstance())) {
                return false;
            }
        }
        if (BuildersWand.isWorldGuardEnabled()) {
            if (!WorldGuardHook.canBuild(player, target.getLocation())) {
                return false;
            }
        }
        return BlockFinderUtil.isReplaceable(target);
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
                Particle particle = tryParseParticle(wand.getPreviewParticle());
                Particle.DustOptions dustOptions = particle == Particle.DUST ?
                    new Particle.DustOptions(
                        Color.fromRGB(wand.getPreviewParticleOptionsRed(), wand.getPreviewParticleOptionsGreen(), wand.getPreviewParticleOptionsBlue()),
                        (float) wand.getPreviewParticleOptionsSize()
                    ) : null;

                for (Block preview : session.previewBlocks) {
                    if (!preview.getWorld().equals(world)) {
                        continue;
                    }
                    Location loc = preview.getLocation().add(0.5, 0.5, 0.5);
                    if (dustOptions != null) {
                        world.spawnParticle(Particle.DUST, loc, wand.getPreviewParticleCount(),
                            wand.getPreviewParticleOffsetX(), wand.getPreviewParticleOffsetY(), wand.getPreviewParticleOffsetZ(),
                            wand.getPreviewParticleSpeed(), dustOptions);
                    }
                    else {
                        world.spawnParticle(particle, loc, wand.getPreviewParticleCount(),
                            wand.getPreviewParticleOffsetX(), wand.getPreviewParticleOffsetY(), wand.getPreviewParticleOffsetZ(),
                            wand.getPreviewParticleSpeed());
                    }
                }
            }
        };
        session.particleTask.runTaskTimer(BuildersWand.getInstance(), 0L, 5L);
    }

    private Particle tryParseParticle(String particleName) {
        try {
            return Particle.valueOf(particleName);
        }
        catch (Exception e) {
            return Particle.DUST;
        }
    }

    public void unlockPlayer(Player player) {
        getSession(player).placing = false;
    }
}
