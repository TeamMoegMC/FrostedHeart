package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ThinIceBlock extends IceBlock {
    public ThinIceBlock(Properties pProperties) {
        super(pProperties);
    }

    // no random tick
    @Override
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adj, Direction side) {
        if (adj.is(this)) {
                return true;
        }
        if (adj.is(FHBlocks.LAYERED_THIN_ICE.get()) && adj.hasProperty(LayeredThinIceBlock.LAYERS)) {
            return adj.getValue(LayeredThinIceBlock.LAYERS).equals(LayeredThinIceBlock.MAX_HEIGHT);
        }
        return super.skipRendering(state, adj, side);
    }
}
