package com.teammoeg.chorda.block;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class CDirectionalRotatableBlock extends Block {
	public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<AttachFace> ATTACH_FACE=BlockStateProperties.ATTACH_FACE;
	public CDirectionalRotatableBlock(Properties p_52591_) {
		super(p_52591_);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		Direction clickedFace=pContext.getClickedFace();
		return this.defaultBlockState().setValue(ATTACH_FACE, (clickedFace.getAxis()==Axis.Y)?(clickedFace==Direction.UP?AttachFace.FLOOR:AttachFace.CEILING):AttachFace.WALL)
				.setValue(FACING, pContext.getHorizontalDirection().getOpposite());
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder);
		pBuilder.add(FACING).add(ATTACH_FACE);
	}

	@Override
	public @NotNull BlockState rotate(BlockState pState, Rotation pRot) {
		return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)));
	}

	@Override
	public @NotNull BlockState mirror(BlockState pState, Mirror pMirror) {
		return pState.setValue(FACING, pMirror.mirror(pState.getValue(FACING)));
	}

}
