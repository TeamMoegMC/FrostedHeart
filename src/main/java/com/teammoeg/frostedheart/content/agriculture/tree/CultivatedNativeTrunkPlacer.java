package com.teammoeg.frostedheart.content.agriculture.tree;

import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer.FoliageAttachment;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;

/** Fixes the sampled size while delegating every native write and canopy attachment unchanged. */
public final class CultivatedNativeTrunkPlacer extends StraightTrunkPlacer {
    private final TrunkPlacer nativeTrunk;

    public CultivatedNativeTrunkPlacer(TrunkPlacer nativeTrunk, int height) {
        super(height, 0, 0);
        this.nativeTrunk = nativeTrunk;
    }

    /** Original species algorithm, retained for shape validation and configuration inspection. */
    public TrunkPlacer nativeTrunk() { return nativeTrunk; }

    @Override
    public boolean isFree(LevelSimulatedReader level, BlockPos pos) { return nativeTrunk.isFree(level, pos); }

    @Override
    public List<FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter,
            RandomSource random, int height, BlockPos origin, TreeConfiguration configuration) {
        return nativeTrunk.placeTrunk(level, setter, random, height, origin, configuration);
    }
}
