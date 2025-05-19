package com.teammoeg.frostedheart.content.agriculture;

import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerGrade;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerType;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class FertilizedDirt extends Block {
    public static final EnumProperty<FertilizerType> FERTILIZER = EnumProperty.create("fertilizer",FertilizerType.class);
    public static final IntegerProperty STORAGE = IntegerProperty.create("storage", 1, 8);
    public static final EnumProperty<FertilizerGrade> ADVANCED = EnumProperty.create("advanced",FertilizerGrade.class);
    public FertilizedDirt(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FERTILIZER, FertilizerType.ACCELERATED).setValue(ADVANCED, FertilizerGrade.BASIC).setValue(STORAGE, 1));
    }

	@Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FERTILIZER, ADVANCED, STORAGE);
    }
}
