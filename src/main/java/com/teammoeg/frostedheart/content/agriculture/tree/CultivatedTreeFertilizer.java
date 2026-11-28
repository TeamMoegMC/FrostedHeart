package com.teammoeg.frostedheart.content.agriculture.tree;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.agriculture.FertilizedDirt;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** One settlement per successfully placed tree, matching the legacy sapling event's four-unit cost. */
public final class CultivatedTreeFertilizer {
    public static final int COST_PER_SUCCESSFUL_TREE = 4;
    private CultivatedTreeFertilizer() {}

    /** A giant uses its northwest generation origin, just like the original mega-tree grower event. */
    public static void consumeAfterSuccess(ServerLevel level, BlockPos treeOrigin) {
        BlockPos soilPos = treeOrigin.below();
        var soil = level.getBlockState(soilPos);
        if (!soil.is(FHBlocks.FERTILIZED_DIRT.get())) return;
        int remaining = soil.getValue(FertilizedDirt.STORAGE) - COST_PER_SUCCESSFUL_TREE;
        level.setBlock(soilPos, remaining > 0 ? soil.setValue(FertilizedDirt.STORAGE, remaining)
                : Blocks.DIRT.defaultBlockState(), 3);
    }
}
