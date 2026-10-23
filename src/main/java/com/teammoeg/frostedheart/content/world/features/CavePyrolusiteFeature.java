package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/** Vanilla ore algorithm with independently configured vein size and terrain-based attempt thinning. */
public final class CavePyrolusiteFeature extends Feature<OreConfiguration> {
    public CavePyrolusiteFeature(Codec<OreConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<OreConfiguration> context) {
        return passesAttempt(context.level(), context.origin(), context.random()) && Feature.ORE.place(context);
    }

    static boolean passesAttempt(WorldGenLevel level, BlockPos origin, RandomSource random) {
        // For vanilla random sources, nextInt(2) == nextInt(4) >> 1 from the same
        // state; both consume one draw. Only quartile 1 depends on cave clearance.
        int roll = random.nextInt(4);
        return roll == 0 || roll == 1 && IceCaveSpace.nearWideAir(level, origin);
    }
}
