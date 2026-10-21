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

/** Small exposed patches keep stone footholds and follow the real cave shape. */
public final class IceCaveSurfaceFeature extends Feature<NoneFeatureConfiguration> {
    public IceCaveSurfaceFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = IceCaveSpace.insideChunk(context.origin(), 4);
        BlockPos floor = IceCaveSpace.findFloorAir(level, origin, 8);
        if (floor == null) return false;
        boolean wide = IceCaveSpace.isWide(level, floor);
        int changed = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > 5) continue;
                BlockPos air = floor.offset(dx, 0, dz);
                if (!level.getBlockState(air).isAir()) continue;
                BlockPos ground = air.below();
                // Leave approximately one third of the floor as bare stone for passage and rest.
                if (IceCaveSpace.isRock(level.getBlockState(ground)) && !isSpringShore(level, ground)
                        && context.random().nextInt(3) != 0) {
                    level.setBlock(ground, Blocks.ICE.defaultBlockState(), 2);
                    changed++;
                }
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos wall = air.relative(direction);
                    if (IceCaveSpace.isRock(level.getBlockState(wall)) && context.random().nextInt(4) != 0) {
                        level.setBlock(wall, Blocks.ICE.defaultBlockState(), 2);
                        changed++;
                    }
                }
            }
        }
        if (wide && changed > 0) {
            if (context.random().nextInt(8) == 0) {
                BlockPos hazard = floor.offset(context.random().nextInt(5) - 2, 0, context.random().nextInt(5) - 2);
                if (level.getBlockState(hazard).isAir() && level.getBlockState(hazard.above()).isAir()
                        && level.getBlockState(hazard.below()).is(Blocks.ICE)
                        && IceCaveSpace.isRock(level.getBlockState(hazard.below(2)))) {
                    level.setBlock(hazard.below(2), Blocks.WATER.defaultBlockState(), 2);
                    level.setBlock(hazard.below(), FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState(), 2);
                }
            }
            // The luminous block always replaces ice, while the ordinary crystal occupies air beside it.
            for (int attempt = 0; attempt < 8; attempt++) {
                BlockPos air = floor.offset(context.random().nextInt(5) - 2, context.random().nextInt(3),
                        context.random().nextInt(5) - 2);
                if (!level.getBlockState(air).isAir()) continue;
                for (Direction direction : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST,
                        Direction.WEST, Direction.DOWN}) {
                    BlockPos ice = air.relative(direction);
                    if (level.getBlockState(ice).is(Blocks.ICE)) {
                        level.setBlock(ice, FHBlocks.GLOWSTONE_CRYSTAL.get().defaultBlockState(), 2);
                        var crystal = FHBlocks.ICE_CRYSTAL.get().defaultBlockState()
                                .setValue(AmethystClusterBlock.FACING, direction.getOpposite());
                        if (crystal.canSurvive(level, air)) {
                            level.setBlock(air, crystal, 2);
                            return true;
                        }
                        level.setBlock(ice, Blocks.ICE.defaultBlockState(), 2);
                    }
                }
            }
        }
        return changed > 0;
    }

    private static boolean isSpringShore(WorldGenLevel level, BlockPos ground) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && level.getFluidState(ground.offset(dx, 0, dz))
                        .getType().isSame(FHFluids.HOT_SPRING.get())) return true;
            }
        }
        return false;
    }
}
