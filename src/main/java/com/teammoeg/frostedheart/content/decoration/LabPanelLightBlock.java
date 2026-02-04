package com.teammoeg.frostedheart.content.decoration;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class LabPanelLightBlock extends RedstoneLampBlock {

	public LabPanelLightBlock(Properties pProperties) {
		super(pProperties);
	}

	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder.add(BlockStateProperties.HORIZONTAL_FACING));
	}
}
