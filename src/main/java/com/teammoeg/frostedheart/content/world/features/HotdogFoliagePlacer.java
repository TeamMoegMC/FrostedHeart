package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.world.FHFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
public class HotdogFoliagePlacer extends FoliagePlacer {
    public static final Codec<HotdogFoliagePlacer> CODEC = RecordCodecBuilder.create(instance ->
            foliagePlacerParts(instance)
                    .and(Codec.intRange(1, 32).fieldOf("foliage_height").forGetter(p -> p.foliageHeight))
                    .apply(instance, HotdogFoliagePlacer::new));

    private final int foliageHeight;

    public HotdogFoliagePlacer(IntProvider radius, IntProvider offset, int foliageHeight) {
        super(radius, offset);
        this.foliageHeight = foliageHeight;
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return FHFeatures.FHFoliagePlacerTypes.HOTDOG_FOLIAGE_PLACER.get();
    }

    @Override
    protected void createFoliage(LevelSimulatedReader level, FoliageSetter setter,
                                 RandomSource random, TreeConfiguration config,
                                 int maxFreeTreeHeight, FoliageAttachment attachment,
                                 int foliageHeight, int foliageRadius, int offset) {

        BlockPos topLog = attachment.pos().below(1);

        // 包裹树干的十字形叶子
        for (int y = 0; y < this.foliageHeight; y++) {
            BlockPos center = topLog.below(y);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                tryPlaceLeaf(level, setter, random, config, center.relative(dir));
            }
        }

        // 顶部封口：放在 attachment.pos()，刚好紧挨树干顶部
        tryPlaceLeaf(level, setter, random, config, attachment.pos());
    }


    @Override
    public int foliageHeight(RandomSource random, int height, TreeConfiguration config) {
        return foliageHeight;
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int localX, int localZ, int range, int trunkOffsetY, boolean large) {
        return false;
    }
}
