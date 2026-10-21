package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/** Same ore algorithm and vein size as local pyrite, with terrain-based attempt thinning. */
public final class CavePyrolusiteFeature extends Feature<OreConfiguration> {
    public CavePyrolusiteFeature(Codec<OreConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<OreConfiguration> context) {
        int denominator = IceCaveSpace.nearWideAir(context.level(), context.origin()) ? 2 : 4;
        return context.random().nextInt(denominator) == 0 && Feature.ORE.place(context);
    }
}
