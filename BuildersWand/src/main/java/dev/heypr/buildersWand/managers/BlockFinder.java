package dev.heypr.buildersWand.managers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.*;

public class BlockFinder {

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

    public static List<Block> findConnectedBlocks(Block startBlock, BlockFace face, int maxSize, List<Material> blockedMaterials) {
        List<Block> result = new ArrayList<>();
        if (face == null || maxSize <= 0) {
            return result;
        }

        Material targetMaterial = startBlock.getType();
        if (blockedMaterials.contains(targetMaterial)) {
            return result;
        }

        BlockFace[] searchDirections = getPlaneDirections(face);

        Queue<Block> queue = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();

        queue.add(startBlock);
        visited.add(startBlock);

        while (!queue.isEmpty() && result.size() < maxSize) {
            Block current = queue.poll();

            if (current.getType() != targetMaterial) continue;

            Block targetPlaceLocation = current.getRelative(face);
            if (!isReplaceable(targetPlaceLocation)) continue;

            result.add(current);

            for (BlockFace neighborFace : searchDirections) {
                Block neighbor = current.getRelative(neighborFace);
                if (visited.add(neighbor)) {
                    if (neighbor.getType() == targetMaterial) {
                        queue.add(neighbor);
                    }
                }
            }
        }
        return result;
    }

    public static List<Block> getStaticBlocks(Block startBlock, BlockFace face, int length, int width) {
        List<Block> result = new ArrayList<>();
        if (startBlock == null || face == null) return result;

        Material targetMaterial = startBlock.getType();
        BlockFace[] directions = getPlaneDirections(face);

        if (directions.length < 4) {
            return result;
        }

        int lRadius = length / 2;
        int wRadius = width / 2;

        for (int l = -lRadius; l <= (length % 2 == 0 ? lRadius - 1 : lRadius); l++) {
            for (int w = -wRadius; w <= (width % 2 == 0 ? wRadius - 1 : wRadius); w++) {
                Block current = startBlock;

                if (l > 0) for (int i = 0; i < l; i++) current = current.getRelative(directions[0]);
                else if (l < 0) for (int i = 0; i < Math.abs(l); i++) current = current.getRelative(directions[1]);

                if (w > 0) for (int i = 0; i < w; i++) current = current.getRelative(directions[2]);
                else if (w < 0) for (int i = 0; i < Math.abs(w); i++) current = current.getRelative(directions[3]);

                if (current.getType() != targetMaterial) {
                    continue;
                }

                if (!current.getBlockData().matches(startBlock.getBlockData())) continue;

                Block targetPlaceLocation = current.getRelative(face);
                if (isReplaceable(targetPlaceLocation)) {
                    result.add(current);
                }
            }
        }
        return result;
    }

    private static BlockFace[] getPlaneDirections(BlockFace clickedFace) {
        return switch (clickedFace) {
            case UP, DOWN -> new BlockFace[]{BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH};
            case NORTH, SOUTH -> new BlockFace[]{BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
            case EAST, WEST -> new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN};
            default -> new BlockFace[0];
        };
    }

    public static boolean isReplaceable(Block block) {
        return block != null && (block.isReplaceable() || REPLACEABLE.contains(block.getType()));
    }
}
