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
import dev.heypr.buildersWand.managers.WandSession;
import dev.heypr.buildersWand.managers.WandStorage;
import dev.heypr.buildersWand.managers.io.ConfigManager;
import dev.heypr.buildersWand.managers.io.MessageManager;
import dev.heypr.buildersWand.utility.BlockFinderUtil;
import dev.heypr.buildersWand.utility.BlockPlacementUtil;
import dev.heypr.buildersWand.utility.ComponentUtil;
import dev.heypr.buildersWand.utility.InventoryUtil;
import dev.heypr.buildersWand.utility.WandItemUtil;
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
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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
        if (!WandItemUtil.isWand(wandItem)) {
            return;
        }
        Wand wand = WandItemUtil.getWand(wandItem);
        if (wand != null && wand.generatePreviewOnMove()) {
            generatePreview(player, wand);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack wandItem = player.getInventory().getItemInMainHand();
        if (!WandItemUtil.isWand(wandItem)) {
            return;
        }
        Wand wand = WandItemUtil.getWand(wandItem);
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
        ComponentUtil.debug("Calling handlePlacement for " + player.getName() + ", blocks=" + session.previewBlocks.size());
        handlePlacement(player, wand, session);
    }

    @EventHandler
    public void onPlayerSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack wandItem = player.getInventory().getItemInMainHand();
        if (!WandItemUtil.isWand(wandItem)) {
            return;
        }
        if (!BuildersWand.isWandStorageAvailable()) {
            return;
        }
        event.setCancelled(true);
        Wand wand = WandItemUtil.getWand(wandItem);
        if (wand == null) {
            return;
        }
        WandStorage storage = BuildersWand.getStorageManager().getStorage(wand, wandItem);
        if (storage == null) return;
        storage.open(player);
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
        ItemStack wandItem = player.getInventory().getItemInMainHand();
        Block targetBlock = session.target.block();
        if (!player.getGameMode().equals(GameMode.CREATIVE) && wand.consumesItems()) {
            int available = InventoryUtil.getItemCount(player, targetBlock.getType(), wand, wandItem);
            if (available < needed) {
                MessageManager.sendMessage(player, MessageManager.Messages.INSUFFICIENT_BLOCKS, "needed", needed - available, "material", targetBlock.getType().name());
                session.previewBlocks.clear();
                session.target = null;
                return;
            }
        }
        session.lastRightClickTime = now;
        BlockPlaceEvent bpe = new BlockPlaceEvent(
                targetBlock,
                targetBlock.getState(),
                targetBlock,
                new ItemStack(targetBlock.getType()),
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
            WandPlaceEvent wandEvent = new WandPlaceEvent(player, wandItem, wand, session.previewBlocks);
            Bukkit.getPluginManager().callEvent(wandEvent);
            if (wandEvent.isCancelled()) return;
            finalBlocksToPlace = wandEvent.getBlocksToPlace();
        }
        storeUndoHistory(wand, session, finalBlocksToPlace);
        if (!player.getGameMode().equals(GameMode.CREATIVE) && wand.consumesItems()) {
            InventoryUtil.removeItems(player, targetBlock, needed, wand, wandItem);
        }
        placeBlocks(player, wand, wandItem, session, finalBlocksToPlace);
        handleDurability(player, wand, wandItem);
        if (!wand.generatePreviewOnMove()) {
            session.initialPlace = true;
        }
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
        if (!WandItemUtil.isWand(itemInHand)) {
            return;
        }
        if (!WandItemUtil.isRegisteredWand(itemInHand)) {
            if (ConfigManager.shouldDestroyInvalidWands()) {
                ComponentUtil.error("Removing misconfigured wand from their inventory...");
                player.getInventory().removeItem(itemInHand);
                MessageManager.sendMessage(player, MessageManager.Messages.MISCONFIGURED);
            }
            else {
                ComponentUtil.error("Misconfigured wand not removed due to configuration option.");
            }
        }
        Wand wand = WandItemUtil.getWand(itemInHand);
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
            InventoryUtil.returnItems(player, itemsToReturn, wand, itemInHand);
        }
        MessageManager.sendActionBar(player, MessageManager.Messages.ACTION_UNDONE, "remaining", session.undoHistory.size());
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

    private void placeBlocks(Player player, Wand wand, ItemStack wandItem, WandSession session, Set<Block> blocks) {
        Block targetBlock = session.target.block();
        Material targetMaterial = targetBlock.getType();
        boolean fireEvents = ConfigManager.shouldFireBlockPlaceEvent();
        if (ConfigManager.isPlacementQueueEnabled()) {
            new PlacementQueueManager(player, blocks, targetMaterial, ConfigManager.getMaxBlocksPerTick(), targetBlock, fireEvents, wand, wandItem).start();
        }
        else {
            int skipped = 0;
            for (Block block : blocks) {
                if (!BlockPlacementUtil.place(block, targetMaterial, targetBlock, player, fireEvents)) {
                    skipped++;
                }
            }
            if (skipped > 0 && wand.consumesItems()) {
                InventoryUtil.returnItems(player, targetMaterial, skipped, wand, wandItem);
            }
        }
        session.previewBlocks.clear();
        session.target = null;
    }

    private void handleDurability(Player player, Wand wand, ItemStack wandItem) {
        if (!WandItemUtil.isWandDurabilityEnabled(wandItem)) {
            WandItemUtil.handleInfiniteDurability(wandItem);
            return;
        }
        if (WandItemUtil.getWandDurability(wandItem) <= 1) {
            player.getInventory().removeItem(wandItem);
            if (wand.isBreakSoundEnabled()) {
                playBreakSound(player, wand);
            }
        }
        else {
            WandItemUtil.decrementWandDurability(wandItem);
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

    public void generatePreview(Player player, Wand wand) {
        RayTraceResult rtr = player.rayTraceBlocks(wand.getMaxRayTraceDistance(), FluidCollisionMode.NEVER);
        if (rtr == null || rtr.getHitBlock() == null || rtr.getHitBlockFace() == null) {
            ComponentUtil.debug("rayTraceBlocks returned null for " + player.getName());
            return;
        }
        WandSession session = getSession(player);
        if (session.placing) {
            return;
        }
        Block hitBlock = rtr.getHitBlock();
        BlockFace face = rtr.getHitBlockFace();
        if (session.target != null && hitBlock.equals(session.target.block()) && face == session.target.face() && !session.previewBlocks.isEmpty()) {
            return;
        }
        if (wand.getBlockedMaterials().contains(hitBlock.getType())) {
            return;
        }
        if (session.currentCalculation != null && !session.currentCalculation.isDone()) {
            session.currentCalculation.cancel(true);
        }
        session.target = new WandSession.TargetBlock(hitBlock, face);
        session.currentCalculation = CompletableFuture.supplyAsync(() -> {
            if (wand.getWandType() == Wand.WandType.STATIC) {
                return BlockFinderUtil.getStaticBlocks(hitBlock, face, wand.getStaticLength(), wand.getStaticWidth());
            }
            else {
                return BlockFinderUtil.findConnectedBlocks(hitBlock, face, wand.getMaxSize(), wand.getBlockedMaterials());
            }
        }).thenAcceptAsync(sourceBlocks -> {
            Bukkit.getScheduler().runTask(BuildersWand.getInstance(), () -> {
                Set<Block> validTargets = new HashSet<>();
                for (Block source : sourceBlocks) {
                    Block target = source.getRelative(face);
                    if (isValidLocation(player, target)) {
                        validTargets.add(target);
                    }
                }
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
        }).exceptionally(ex -> {
            ComponentUtil.error("generatePreview async failed: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    private boolean isValidLocation(Player player, Block target) {
        try {
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
        }
        catch (NoClassDefFoundError | Exception ex) {
            return false;
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
                if (!WandItemUtil.isWand(wandItem)) {
                    session.previewBlocks.clear();
                    session.target = null;
                    this.cancel();
                    session.particleTask = null;
                    return;
                }
                World world = player.getWorld();
                Particle particle = tryParseParticle(wand.getPreviewParticle());
                Particle.DustOptions dustOptions = particle == Particle.DUST ? new Particle.DustOptions(wand.getPreviewParticleColor(), (float) wand.getPreviewParticleOptionsSize()) : null;
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
