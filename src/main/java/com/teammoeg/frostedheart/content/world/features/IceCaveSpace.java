package com.teammoeg.frostedheart.content.world.features;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Bounded probes of the terrain that actually exists at feature placement time. */
final class IceCaveSpace {
    private IceCaveSpace() {}

    static BlockPos insideChunk(BlockPos origin, int margin) {
        int minX = (origin.getX() >> 4) << 4;
        int minZ = (origin.getZ() >> 4) << 4;
        return new BlockPos(Mth.clamp(origin.getX(), minX + margin, minX + 15 - margin),
                origin.getY(), Mth.clamp(origin.getZ(), minZ + margin, minZ + 15 - margin));
    }

    static boolean isRock(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.TUFF);
    }

    static BlockPos findFloorAir(WorldGenLevel level, BlockPos origin, int search) {
        return findFloorAir(level, origin, search, false);
    }

    private static BlockPos findFloorAir(WorldGenLevel level, BlockPos origin, int search, boolean spatialProbe) {
        for (int offset = 0; offset <= search; offset++) {
            BlockPos below = origin.below(offset);
            if (floorAir(level, below, spatialProbe)) return below;
            if (offset != 0) {
                BlockPos above = origin.above(offset);
                if (floorAir(level, above, spatialProbe)) return above;
            }
        }
        return null;
    }

    private static boolean floorAir(WorldGenLevel level, BlockPos pos, boolean spatialProbe) {
        BlockPos floor = pos.below();
        if (level.isOutsideBuildHeight(floor) || !level.getBlockState(pos).isAir()
                || !level.getBlockState(pos.above()).isAir()) return false;
        BlockState support = level.getBlockState(floor);
        // Space remains wide after an earlier ore feature replaces its rock floor.
        // Surface/basin callers retain their separate, narrow replacement predicate.
        return spatialProbe ? support.isFaceSturdy(level, floor, Direction.UP) : isRock(support);
    }

    static boolean isWide(WorldGenLevel level, BlockPos floorAir) {
        return isWide(floorAir, candidate -> level.getBlockState(candidate).isAir());
    }

    static boolean isWide(BlockPos floorAir, Predicate<BlockPos> isAir) {
        for (int y = 0; y < 5; y++) {
            if (!isAir.test(floorAir.above(y))) return false;
        }
        int open = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            boolean clear = true;
            for (int distance = 1; distance <= 4; distance++) {
                BlockPos candidate = floorAir.relative(direction, distance);
                if (!isAir.test(candidate) || !isAir.test(candidate.above())) {
                    clear = false;
                    break;
                }
            }
            if (clear) open++;
        }
        return open >= 3;
    }

    static boolean nearWideAir(WorldGenLevel level, BlockPos origin) {
        for (int dx = -3; dx <= 3; dx += 3) {
            for (int dz = -3; dz <= 3; dz += 3) {
                BlockPos floor = findFloorAir(level, origin.offset(dx, 0, dz), 5, true);
                if (floor != null && isWide(level, floor)) return true;
            }
        }
        return false;
    }
}
