/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.block.IcicleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
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
        int wanted = Mth.clamp((int) Math.round(2 + context.random().nextGaussian() * 3.75), 1, IcicleBlock.MAX_NATURAL_LENGTH);
        int length = 0;
        while (length < wanted && !level.isOutsideBuildHeight(root.below(length)) && level.getBlockState(root.below(length)).isAir()) length++;
        for (int i = 0; i < length; i++) {
            int shape = i == length - 1 ? IcicleBlock.TIP : i == 0 ? IcicleBlock.ROOT
                    : i == length - 2 ? IcicleBlock.TAIL : IcicleBlock.MIDDLE;
            BlockState state = FHBlocks.ICICLE.get().defaultBlockState().setValue(IcicleBlock.SHAPE, shape);
            level.setBlock(root.below(i), state, 2);
        }
        return length > 0;
    }
}
