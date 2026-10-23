/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.block.IcicleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** New terrain only. The pack's ceiling scan supplies the candidate position. */
public final class IcicleFeature extends Feature<NoneFeatureConfiguration> {
    public IcicleFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        BlockPos root = level.getBlockState(context.origin()).isAir() ? context.origin() : context.origin().below();
        if (!level.getBlockState(root).isAir() || !level.getBlockState(root.above()).isFaceSturdy(level, root.above(), Direction.DOWN)
                || IcicleBlock.hasNearbyRoot(level, root)) return false;
        return placeChain(level, root, context.random(), false);
    }

    static boolean placeCaveIcicle(WorldGenLevel level, BlockPos root, RandomSource random) {
        if (!level.getBlockState(root).isAir()
                || !level.getBlockState(root.above()).isFaceSturdy(level, root.above(), Direction.DOWN)) return false;
        // Worldgen spacing only. Runtime growth, fracture and maximum length continue to use IcicleBlock unchanged.
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int dy = -1; dy <= 1; dy++) {
                if (IcicleBlock.isIcicle(level.getBlockState(root.relative(side).above(dy)))) return false;
            }
        }
        return placeChain(level, root, random, true);
    }

    private static boolean placeChain(WorldGenLevel level, BlockPos root, RandomSource random, boolean cave) {
        int wanted = Mth.clamp((int) Math.round(2 + random.nextGaussian() * 3.75), 1, IcicleBlock.MAX_NATURAL_LENGTH);
        int length = 0;
        while (length < wanted && !level.isOutsideBuildHeight(root.below(length))
                && level.getBlockState(root.below(length)).isAir()
                && (!cave || IceCaveTerrain.inBiome(level, root.below(length)))) length++;
        for (int i = 0; i < length; i++) {
            int shape = i == length - 1 ? IcicleBlock.TIP : i == 0 ? IcicleBlock.ROOT
                    : i == length - 2 ? IcicleBlock.TAIL : IcicleBlock.MIDDLE;
            BlockState state = FHBlocks.ICICLE.get().defaultBlockState().setValue(IcicleBlock.SHAPE, shape);
            level.setBlock(root.below(i), state, 2);
        }
        return length > 0;
    }
}
