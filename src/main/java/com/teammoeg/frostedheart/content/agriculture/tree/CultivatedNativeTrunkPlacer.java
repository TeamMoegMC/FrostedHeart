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
    private final boolean giant;

    public CultivatedNativeTrunkPlacer(TrunkPlacer nativeTrunk, int height) {
        this(nativeTrunk, height, false);
    }

    /** Giant clearance covers all four columns rooted at the northwest origin. */
    public CultivatedNativeTrunkPlacer(TrunkPlacer nativeTrunk, int height, boolean giant) {
        super(height, 0, 0);
        this.nativeTrunk = nativeTrunk;
        this.giant = giant;
    }

    /** Original species algorithm, retained for shape validation and configuration inspection. */
    public TrunkPlacer nativeTrunk() { return nativeTrunk; }

    @Override
    public boolean isFree(LevelSimulatedReader level, BlockPos pos) {
        return nativeTrunk.isFree(level, pos) && (!giant
                || nativeTrunk.isFree(level, pos.east())
                && nativeTrunk.isFree(level, pos.south())
                && nativeTrunk.isFree(level, pos.east().south()));
    }

    @Override
    public List<FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter,
            RandomSource random, int height, BlockPos origin, TreeConfiguration configuration) {
        return nativeTrunk.placeTrunk(level, setter, random, height, origin, configuration);
    }
}
