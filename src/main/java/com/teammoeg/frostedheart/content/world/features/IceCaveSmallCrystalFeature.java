package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.common.Tags;

/** Sparse surface crystals: vanilla glow-lichen candidate frequency and bounded local searches. */
public final class IceCaveSmallCrystalFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SEARCH_RANGE = 20;

    public IceCaveSmallCrystalFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!IceCaveIceBody.canRead(level, origin) || !level.getBlockState(origin).isAir()) return false;
        if (tryPlace(level, origin, random)) return true;
        // As with multiface growth, start from an empty candidate and search finite rays.
        // Solid terrain stops a ray; this never searches an entire chunk's air volume.
        for (Direction ray : Direction.allShuffled(random)) {
            for (int distance = 1; distance <= SEARCH_RANGE; distance++) {
                BlockPos pos = origin.relative(ray, distance);
                if (!IceCaveIceBody.canRead(level, pos) || !level.getBlockState(pos).isAir()) break;
                if (tryPlace(level, pos, random)) return true;
            }
        }
        return false;
    }

    static boolean tryPlace(WorldGenLevel level, BlockPos pos, RandomSource random) {
        if (!IceCaveIceBody.canRead(level, pos) || !level.ensureCanWrite(pos)
                || !IceCaveTerrain.inBiome(level, pos) || !level.getBlockState(pos).isAir()) return false;
        for (Direction side : Direction.allShuffled(random)) {
            BlockPos support = pos.relative(side);
            if (!IceCaveIceBody.canRead(level, support)) continue;
            BlockState base = level.getBlockState(support);
            if (!(IceCaveIceBody.isBase(base) || IceCaveTerrain.stone(base)
                    || base.is(Tags.Blocks.ORES) || base.is(FHBlocks.THIN_ICE_BLOCK.get()))) continue;
            BlockState crystal = FHBlocks.ICE_CRYSTAL.get().defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, side.getOpposite());
            if (crystal.canSurvive(level, pos)) {
                level.setBlock(pos, crystal, 2);
                return true;
            }
        }
        return false;
    }
}
