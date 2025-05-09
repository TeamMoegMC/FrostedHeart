package com.teammoeg.frostedheart.content.agriculture;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class FertilizedDirt extends Block {
    public static final IntegerProperty FERTILIZER = IntegerProperty.create("fertilizer", 0, 3);
    public FertilizedDirt(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FERTILIZER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FERTILIZER);
    }
}
