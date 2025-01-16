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

package com.teammoeg.chorda.block;

import java.util.Map;
import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.teammoeg.chorda.blockentity.CPipeBlockEntity;
import com.teammoeg.chorda.util.CUtils;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.ticks.TickPriority;

public class CPipeBlock<T extends CPipeBlock<T>> extends PipeBlock implements SimpleWaterloggedBlock{
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

	public CPipeBlock(Class<T> type, Properties blockProps) {
		super(4 / 16f, blockProps);
		lightOpacity = 15;

		this.type = type;

		BlockState defaultState = defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false).setValue(CASING, false);
		for (Direction d : Direction.values()) {
			defaultState = defaultState.setValue(PROPERTY_BY_DIRECTION.get(d), false);
			defaultState = defaultState.setValue(RIM_PROPERTY_MAP.get(d), false);
		}

		this.registerDefaultState(defaultState);
	}
	
	@Override
	public boolean isPathfindable(BlockState state, BlockGetter worldIn, BlockPos pos, PathComputationType type) {
		return false;
	}

	public boolean canConnectTo(LevelAccessor world, BlockPos neighbourPos, BlockState neighbour, Direction direction) {
		return false;
	}

	@Override
	protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, BlockStateProperties.WATERLOGGED);
		builder.add(RNORTH, REAST, RSOUTH, RWEST, RUP, RDOWN, CASING);
		super.createBlockStateDefinition(builder);
	}

	@Nullable
	private Axis getAxis(BlockGetter world, BlockPos pos, BlockState state) {
		if (!state.is(this)) return null;
		for (Axis axis : Axis.values()) {
			Direction d1 = Direction.get(AxisDirection.NEGATIVE, axis);
			Direction d2 = Direction.get(AxisDirection.POSITIVE, axis);
			boolean openAt1 = isOpenAt(state, d1);
			boolean openAt2 = isOpenAt(state, d2);
			if (openAt1 && openAt2) {
				return axis;
			}
		}
		return null;
	}

	public BlockState getAxisState(Axis axis) {
		BlockState defaultState = defaultBlockState();
		for (Direction d : Direction.values())
			defaultState = defaultState.setValue(PROPERTY_BY_DIRECTION.get(d), d.getAxis() == axis);
		return defaultState;
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		if (state.isSolidRender(worldIn, pos))
			return lightOpacity;
		else
			return state.propagatesSkylightDown(worldIn, pos) ? 0 : 1;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState FluidState = context.getLevel()
			.getFluidState(context.getClickedPos());
		return updateBlockState(defaultBlockState(), context.getNearestLookingDirection(), null, context.getLevel(),
			context.getClickedPos()).setValue(BlockStateProperties.WATERLOGGED,
                FluidState.getType() == Fluids.WATER);
	}

	public boolean isCornerOrEndPipe(BlockAndTintGetter world, BlockPos pos, BlockState state) {
		return (state.is(this)) && getAxis(world, pos, state) == null
			&& !shouldDrawCasing(world, pos, state);
	}

	public boolean isOpenAt(BlockState state, Direction direction) {
		return state.getValue(PROPERTY_BY_DIRECTION.get(direction));
	}

	public T setLightOpacity(int opacity) {
		lightOpacity = opacity;
		return (T) this;
	}

	public boolean shouldDrawCasing(BlockAndTintGetter world, BlockPos pos, BlockState state) {
		if (!state.is(this))
			return false;
		Axis axis = getAxis(world, pos, state);
		if (axis == null) return false;
		for (Direction direction : Direction.values())
			if (direction.getAxis() != axis && isOpenAt(state, direction))
				return true;
		return false;
	}

	public boolean shouldDrawRim(LevelAccessor world, BlockPos pos, BlockState state,
		Direction direction) {
		if (!isOpenAt(state, direction))
			return false;
		BlockPos offsetPos = pos.relative(direction);
		BlockState facingState = world.getBlockState(offsetPos);
		if (!facingState.is(this))
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
	public void checkNewConnection(LevelAccessor world,BlockPos pos,BlockState old,BlockState neo) {
		CPipeBlockEntity te= CUtils.getExistingTileEntity(world, pos, CPipeBlockEntity.class);
		if(te!=null) {
			for (Direction d : Direction.values()) {
				BooleanProperty prop=PROPERTY_BY_DIRECTION.get(d);
				if(old.getValue(prop)!=neo.getValue(prop)) {
					//System.out.println(pos+"face "+d+" "+neo.getValue(prop));
					te.onFaceChange(d, neo.getValue(prop));
				}
			}
		}
	}
	public BlockState updateBlockState(BlockState state, @Nullable Direction direction, @Nullable Direction ignore,
		LevelAccessor world, BlockPos pos) {
		
		if (direction != null) {
			state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(world, pos.relative(direction), world.getBlockState(pos.relative(direction)), direction));
		} else {
			for (Direction d : Direction.values())
				if (d != ignore) {
					state = state.setValue(PROPERTY_BY_DIRECTION.get(d), canConnectTo(world, pos.relative(d), world.getBlockState(pos.relative(d)), d));
				}
		}
		for (Direction d : Direction.values())
			if (d != ignore) {
				state = state.setValue(RIM_PROPERTY_MAP.get(d), this.shouldDrawRim(world, pos, state, d));

			}
		state = state.setValue(CASING, this.shouldDrawCasing(world, pos, state));
		return state;
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
		LevelAccessor world, BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(BlockStateProperties.WATERLOGGED))
			world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		if (isOpenAt(state, direction) && neighbourState.hasProperty(BlockStateProperties.WATERLOGGED))
			world.scheduleTick(pos, this, 1, TickPriority.HIGH);
		BlockState newstate= updateBlockState(state, direction,null , world, pos);
		//System.out.println("Update post placement");
		checkNewConnection(world,pos,state,newstate);
		return newstate;
	}

	@Override
	public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
		boolean isMoving) {
		super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
		//Direction d = CUtils.dirBetween(fromPos, pos);
		//System.out.println("changed")2
		if(!worldIn.isClientSide)
			worldIn.scheduleTick(pos, this, 10);

	}

	@Override 
	public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource pRandom) {
		//System.out.println("ticked "+pos);
		super.tick(state, worldIn, pos, pRandom);
		BlockState updated=updateBlockState(state, null, null, worldIn, pos);
		worldIn.setBlock(pos, updated, 2);
		worldIn.sendBlockUpdated(pos, state, updated, 2);
		System.out.println("update pipe "+pos);
		checkNewConnection(worldIn,pos,state,updated);
		//System.out.println(pos+" requested update "+updated.getValue(PROPERTY_BY_DIRECTION.get(Direction.UP)));
		//if(state!=updated) {
			//System.out.println("requested update "+updated.getValue(PROPERTY_BY_DIRECTION.get(Direction.UP)));
		//worldIn.blockUpdated(pos, null);

		//}
	}

	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(worldIn, pos, state, placer, stack);
		BlockState updated=updateBlockState(state, null, null, worldIn, pos);
		checkNewConnection(worldIn,pos,state,updated);
		worldIn.setBlockAndUpdate(pos,updated);
	}


}