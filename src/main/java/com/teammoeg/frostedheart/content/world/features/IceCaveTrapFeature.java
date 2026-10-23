package com.teammoeg.frostedheart.content.world.features;

import java.util.ArrayList;
import java.util.List;
import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.common.Tags;

/** Thin-ice lids over sealed, three-to-twelve-block air pits or entirely water-filled cavities. */
public final class IceCaveTrapFeature extends Feature<NoneFeatureConfiguration> {
    public IceCaveTrapFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        for (int delta = 0; delta <= 16; delta++) {
            for (int sign : delta == 0 ? new int[] {1} : new int[] {1, -1}) {
                BlockPos air = context.origin().above(sign * delta);
                if (level.getBlockState(air).isAir() && IceCaveTerrain.substrate(level.getBlockState(air.below()))
                        && IceCaveSpace.isWide(level, air) && tryPlace(level, air.below(), context.random())) return true;
            }
        }
        return false;
    }

    static boolean tryPlace(WorldGenLevel level, BlockPos lid, RandomSource random) {
        int depth = 3 + random.nextInt(10);
        int radiusSquared = random.nextBoolean() ? 2 : 5;
        List<BlockPos> lids = new ArrayList<>();
        List<BlockPos> cavity = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (dx * dx + dz * dz > radiusSquared) continue;
            BlockPos top = lid.offset(dx, 0, dz);
            if (!level.getBlockState(top.above()).isAir() || !level.getBlockState(top.above(2)).isAir()) return false;
            lids.add(top);
            for (int dy = 0; dy <= depth; dy++) {
                BlockPos pos = top.below(dy);
                if (!level.ensureCanWrite(pos) || !IceCaveTerrain.inBiome(level, pos)
                        || !(IceCaveTerrain.substrate(level.getBlockState(pos)) || level.getBlockState(pos).is(Tags.Blocks.ORES))
                        || IceCaveTerrain.springNearby(level, pos)) return false;
                if (dy > 0) cavity.add(pos);
                // Check the complete retaining wall and floor before opening the cavity. No lower cave or water leaks.
                for (Direction side : Direction.values()) {
                    if (side == Direction.UP) continue;
                    BlockPos support = pos.relative(side);
                    if (level.isOutsideBuildHeight(support) || !level.hasChunk(support.getX() >> 4, support.getZ() >> 4)) return false;
                    var state = level.getBlockState(support);
                    if (!state.getFluidState().isEmpty() || !state.isFaceSturdy(level, support, side.getOpposite())) return false;
                }
            }
        }
        // One choice for the complete successful cavity; rejected candidates do not draw its wet/dry outcome.
        boolean wet = random.nextInt(5) == 0;
        for (BlockPos pos : cavity) level.setBlock(pos, (wet ? Blocks.WATER : Blocks.CAVE_AIR).defaultBlockState(), 2);
        for (BlockPos pos : lids) level.setBlock(pos, FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState(), 2);
        return true;
    }
}
