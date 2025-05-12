package com.teammoeg.frostedheart.content.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class FertilizedFarmlandBlock extends FarmBlock {
    public static final IntegerProperty FERTILIZER = IntegerProperty.create("fertilizer", 0, 3);
    //public static final BooleanProperty FERTILIZED = BooleanProperty.create("fertilized");

    public FertilizedFarmlandBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FERTILIZER, 0).setValue(MOISTURE,0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FERTILIZER);
    }
}
