/*
 * Copyright (c) 2021-2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.base.block;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.teammoeg.frostedheart.content.steamenergy.ISteamEnergyBlock;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.block.SixWayBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.TickPriority;
import net.minecraft.world.World;

public class FluidPipeBlock<T extends FluidPipeBlock<T>> extends SixWayBlock implements IWaterLoggable {
	Class<T> type;
	protected int lightOpacity;
	public static final BooleanProperty CASING = BooleanProperty.create("casing");
	public static final BooleanProperty RNORTH = BooleanProperty.create("north_rim");
	public static final BooleanProperty REAST = BooleanProperty.create("east_rim");
	public static final BooleanProperty RSOUTH = BooleanProperty.create("south_rim");
	public static final BooleanProperty RWEST = BooleanProperty.create("west_rim");
	public static final BooleanProperty RUP = BooleanProperty.create("up_rim");
	public static final BooleanProperty RDOWN = BooleanProperty.create("down_rim");
	public static final Map<Direction, BooleanProperty> RIM_PROPERTY_MAP = Util.make(Maps.newEnumMap(Direction.class), (directions) -> {
		directions.put(Direction.NORTH, RNORTH);
		directions.put(Direction.EAST, REAST);
		directions.put(Direction.SOUTH, RSOUTH);
		directions.put(Direction.WEST, RWEST);
		directions.put(Direction.UP, RUP);
		directions.put(Direction.DOWN, RDOWN);
	});

	public FluidPipeBlock(Class<T> type, Properties blockProps) {
		super(4 / 16f, blockProps);
		lightOpacity = 15;

		this.type = type;

		BlockState defaultState = getDefaultState().with(BlockStateProperties.WATERLOGGED, false).with(CASING, false);
		for (Direction d : Direction.values()) {
			defaultState = defaultState.with(FACING_TO_PROPERTY_MAP.get(d), false);
			defaultState = defaultState.with(RIM_PROPERTY_MAP.get(d), false);
		}

		this.setDefaultState(defaultState);
	}

	@Override
	public boolean allowsMovement(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
		return false;
	}

	public boolean canConnectTo(IWorld world, BlockPos neighbourPos, BlockState neighbour, Direction direction) {
		if (neighbour.getBlock() instanceof ISteamEnergyBlock && ((ISteamEnergyBlock) neighbour.getBlock()).canConnectFrom(world, neighbourPos, neighbour, direction))
			return true;

		return false;
	}

	@Override
	protected void fillStateContainer(net.minecraft.state.StateContainer.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, BlockStateProperties.WATERLOGGED);
		builder.add(RNORTH, REAST, RSOUTH, RWEST, RUP, RDOWN, CASING);
		super.fillStateContainer(builder);
	}

	@Nullable
	private Axis getAxis(IBlockReader world, BlockPos pos, BlockState state) {
		if (!state.matchesBlock(this)) return null;
		for (Axis axis : Axis.values()) {
			Direction d1 = Direction.getFacingFromAxis(AxisDirection.NEGATIVE, axis);
			Direction d2 = Direction.getFacingFromAxis(AxisDirection.POSITIVE, axis);
			boolean openAt1 = isOpenAt(state, d1);
			boolean openAt2 = isOpenAt(state, d2);
			if (openAt1 && openAt2) {
				return axis;
			}
		}
		return null;
	}

	public BlockState getAxisState(Axis axis) {
		BlockState defaultState = getDefaultState();
		for (Direction d : Direction.values())
			defaultState = defaultState.with(FACING_TO_PROPERTY_MAP.get(d), d.getAxis() == axis);
		return defaultState;
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : Fluids.EMPTY.getDefaultState();
	}

	@Override
	public int getOpacity(BlockState state, IBlockReader worldIn, BlockPos pos) {
		if (state.isOpaqueCube(worldIn, pos))
			return lightOpacity;
		else
			return state.propagatesSkylightDown(worldIn, pos) ? 0 : 1;
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		FluidState FluidState = context.getWorld()
			.getFluidState(context.getPos());
		return updateBlockState(getDefaultState(), context.getNearestLookingDirection(), null, context.getWorld(),
			context.getPos()).with(BlockStateProperties.WATERLOGGED,
				Boolean.valueOf(FluidState.getFluid() == Fluids.WATER));
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	public boolean isCornerOrEndPipe(IBlockDisplayReader world, BlockPos pos, BlockState state) {
		return (state.matchesBlock(this)) && getAxis(world, pos, state) == null
			&& !shouldDrawCasing(world, pos, state);
	}

	public boolean isOpenAt(BlockState state, Direction direction) {
		return state.get(FACING_TO_PROPERTY_MAP.get(direction));
	}

	public T setLightOpacity(int opacity) {
		lightOpacity = opacity;
		return (T) this;
	}

	public boolean shouldDrawCasing(IBlockDisplayReader world, BlockPos pos, BlockState state) {
		if (!state.matchesBlock(this))
			return false;
		Axis axis = getAxis(world, pos, state);
		if (axis == null) return false;
		for (Direction direction : Direction.values())
			if (direction.getAxis() != axis && isOpenAt(state, direction))
				return true;
		return false;
	}

	public boolean shouldDrawRim(IWorld world, BlockPos pos, BlockState state,
		Direction direction) {
		if (!isOpenAt(state, direction))
			return false;
		BlockPos offsetPos = pos.offset(direction);
		BlockState facingState = world.getBlockState(offsetPos);
		if (!facingState.matchesBlock(this))
			return true;
		if (!canConnectTo(world, offsetPos, facingState, direction))
			return true;
		if (!isCornerOrEndPipe(world, pos, state))
			return false;
		if (getAxis(world, pos, facingState) != null)
			return true;
		if (!shouldDrawCasing(world, pos, state) && shouldDrawCasing(world, offsetPos, facingState))
			return true;
		if (isCornerOrEndPipe(world, offsetPos, facingState))
			return direction.getAxisDirection() == AxisDirection.POSITIVE;
		return true;
	}
	public void checkNewConnection(IWorld world,BlockPos pos,BlockState old,BlockState neo) {
		PipeTileEntity te=FHUtils.getExistingTileEntity(world, pos, PipeTileEntity.class);
		if(te!=null) {
			for (Direction d : Direction.values()) {
				if(old.get(FACING_TO_PROPERTY_MAP.get(d))!=neo.get(FACING_TO_PROPERTY_MAP.get(d)))
					te.onFaceChange(d, neo.get(FACING_TO_PROPERTY_MAP.get(d)));
			}
		}
	}
	public BlockState updateBlockState(BlockState state, @Nullable Direction direction, @Nullable Direction ignore,
		IWorld world, BlockPos pos) {
		
		if (direction != null) {
			state = state.with(FACING_TO_PROPERTY_MAP.get(direction), canConnectTo(world, pos.offset(direction), world.getBlockState(pos.offset(direction)), direction));
		} else {
			for (Direction d : Direction.values())
				if (d != ignore) {
					state = state.with(FACING_TO_PROPERTY_MAP.get(d), canConnectTo(world, pos.offset(d), world.getBlockState(pos.offset(d)), d));
				}
		}
		for (Direction d : Direction.values())
			if (d != ignore) {
				state = state.with(RIM_PROPERTY_MAP.get(d), this.shouldDrawRim(world, pos, state, d));

			}
		state = state.with(CASING, this.shouldDrawCasing(world, pos, state));
		return state;
	}

	@Override
	public BlockState updatePostPlacement(BlockState state, Direction direction, BlockState neighbourState,
		IWorld world, BlockPos pos, BlockPos neighbourPos) {
		if (state.get(BlockStateProperties.WATERLOGGED))
			world.getPendingFluidTicks()
				.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		if (isOpenAt(state, direction) && neighbourState.hasProperty(BlockStateProperties.WATERLOGGED))
			world.getPendingBlockTicks().scheduleTick(pos, this, 1, TickPriority.HIGH);
		return updateBlockState(state, null, direction.getOpposite(), world, pos);
	}

	@Override
	public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
		boolean isMoving) {
		super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
		Direction d = FHUtils.dirBetween(fromPos, pos);
		BlockState updated=updateBlockState(state, d, null, worldIn, pos);
		checkNewConnection(worldIn,pos,state,updated);
		worldIn.setBlockState(pos,updated);
	}

}