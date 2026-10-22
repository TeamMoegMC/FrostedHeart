package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A one-block-deep basin recessed below an existing passable stone shore. */
public final class IceCaveSpringFeature extends Feature<NoneFeatureConfiguration> {
    public IceCaveSpringFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos center = findBasin(level, context.origin());
        if (center == null) return false;

        // Level only one-block bumps and dips, preserving a solid bed and a dry stone rim.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz > 10) continue;
                BlockPos air = center.offset(dx, 0, dz);
                if (!level.getBlockState(air).isAir()) level.setBlock(air, Blocks.AIR.defaultBlockState(), 2);
                if (dx * dx + dz * dz > 4 && level.getBlockState(air.below()).isAir()) {
                    level.setBlock(air.below(), Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > 4) continue;
                BlockPos air = center.offset(dx, 0, dz);
                BlockPos basin = air.below();
                level.setBlock(basin.below(), context.random().nextInt(4) == 0
                        ? Blocks.CALCITE.defaultBlockState() : FHBlocks.HYDROTHERMAL_ROCK.get().defaultBlockState(), 2);
                level.setBlock(basin, FHFluids.HOT_SPRING.get().getSource().defaultFluidState().createLegacyBlock(), 2);
            }
        }

        // Follow one actual open approach rather than scattering unrelated directional hints.
        Direction entry = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        for (int attempt = 0; attempt < 4; attempt++, entry = entry.getClockWise()) {
            boolean open = true;
            for (int distance = 2; distance <= 4; distance++) {
                BlockPos air = center.relative(entry, distance);
                if (!level.getBlockState(air).isAir() || !level.getBlockState(air.above()).isAir()) {
                    open = false;
                    break;
                }
            }
            if (open) break;
        }
        // Short, discontinuous indication at the pool and immediate entry, never a long-distance route.
        for (int distance = 2; distance <= 4; distance++) {
            BlockPos air = center.relative(entry, distance);
            if (!level.getBlockState(air).isAir()) continue;
            BlockPos base = air.below();
            if (IceCaveSpace.isRock(level.getBlockState(base))
                    && (distance == 3 || context.random().nextBoolean())) {
                level.setBlock(base, Blocks.ICE.defaultBlockState(), 2);
            }
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos ice = air.relative(side);
                if (level.getBlockState(ice).is(Blocks.ICE)) {
                    level.setBlock(ice, FHBlocks.GLOWSTONE_CRYSTAL.get().defaultBlockState(), 2);
                    var crystal = FHBlocks.LARGE_ICE_CRYSTAL.get().defaultBlockState()
                            .setValue(AmethystClusterBlock.FACING, side.getOpposite());
                    if (crystal.canSurvive(level, air)) {
                        level.setBlock(air, crystal, 2);
                        break;
                    }
                    level.setBlock(ice, Blocks.ICE.defaultBlockState(), 2);
                }
            }
            if (level.getBlockState(air).isAir() && level.getBlockState(base).is(Blocks.ICE)) {
                level.setBlock(base, FHBlocks.GLOWSTONE_CRYSTAL.get().defaultBlockState(), 2);
                var crystal = FHBlocks.LARGE_ICE_CRYSTAL.get().defaultBlockState()
                        .setValue(AmethystClusterBlock.FACING, Direction.UP);
                if (crystal.canSurvive(level, air)) level.setBlock(air, crystal, 2);
                else level.setBlock(base, Blocks.ICE.defaultBlockState(), 2);
            }
        }
        return true;
    }

    static BlockPos findBasin(WorldGenLevel level, BlockPos origin) {
        // A small fixed grid finds nearby cave floor without scanning a chunk or changing placement frequency.
        BlockPos anchor = IceCaveSpace.insideChunk(origin, 7);
        for (int dx : new int[] {0, -2, 2}) {
            for (int dz : new int[] {0, -2, 2}) {
                BlockPos center = IceCaveSpace.findFloorAir(level, anchor.offset(dx, 0, dz), 8);
                if (center == null || center.getY() > 0
                        || !(IceCaveSpace.isWide(level, center) || IceCaveSpace.isWide(level, center.above()))) continue;
                boolean valid = true;
                for (int x = -3; x <= 3 && valid; x++) for (int z = -3; z <= 3; z++) {
                    if (x * x + z * z > 10) continue;
                    BlockPos air = center.offset(x, 0, z);
                    var top = level.getBlockState(air);
                    var floor = level.getBlockState(air.below());
                    if ((!top.isAir() && !IceCaveSpace.isRock(top))
                            || !level.getBlockState(air.above()).isAir()
                            || (!floor.isAir() && !IceCaveSpace.isRock(floor))
                            || !IceCaveSpace.isRock(level.getBlockState(air.below(2)))) {
                        valid = false;
                        break;
                    }
                }
                if (valid) return center;
            }
        }
        return null;
    }
}
