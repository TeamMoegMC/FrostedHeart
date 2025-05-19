package com.teammoeg.frostedheart.content.agriculture;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class FertilizedFarmlandBlock extends FarmBlock {

    public FertilizedFarmlandBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(FertilizedDirt.FERTILIZER, Fertilizer.FertilizerType.ACCELERATED)
                .setValue(FertilizedDirt.STORAGE, 1)
                .setValue(MOISTURE,0)
                .setValue(FertilizedDirt.ADVANCED, Fertilizer.FertilizerGrade.BASIC));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FertilizedDirt.FERTILIZER,FertilizedDirt.ADVANCED, FertilizedDirt.STORAGE);
    }
}
