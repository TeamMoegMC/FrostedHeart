package com.teammoeg.frostedheart.block;

import java.util.Map;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.FHContent;
import com.teammoeg.frostedheart.steamenergy.ISteamEnergyBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.block.SixWayBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.TickPriority;

public class FluidPipeBlock<T extends FluidPipeBlock<T>> extends SixWayBlock implements IWaterLoggable {
	Class<T> type;
    public final String name;
    protected int lightOpacity;
    public static final BooleanProperty CASING=BooleanProperty.create("casing");
    public static final Map<Direction, BooleanProperty> RIM_MAP = Util.make(Maps.newEnumMap(Direction.class), (directions) -> {
        directions.put(Direction.NORTH,BooleanProperty.create("north_rim"));
        directions.put(Direction.EAST,BooleanProperty.create("east_rim"));
        directions.put(Direction.SOUTH,BooleanProperty.create("south_rim"));
        directions.put(Direction.WEST,BooleanProperty.create("west_rim"));
        directions.put(Direction.UP,BooleanProperty.create("up_rim"));
        directions.put(Direction.DOWN,BooleanProperty.create("down_rim"));
     });
    public ResourceLocation createRegistryName() {
        return new ResourceLocation(FHMain.MODID, name);
    }

    public T setLightOpacity(int opacity) {
        lightOpacity = opacity;
        return (T) this;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getOpacity(BlockState state, IBlockReader worldIn, BlockPos pos) {
        if (state.isOpaqueCube(worldIn, pos))
            return lightOpacity;
        else
            return state.propagatesSkylightDown(worldIn, pos) ? 0 : 1;
    }
	public FluidPipeBlock(Class<T> type,String name, Properties blockProps, BiFunction<Block, Item.Properties, Item> createItemBlock) {
		super(4 / 16f, blockProps);
        this.name = name;
        lightOpacity = 15;

        ResourceLocation registryName = createRegistryName();
        setRegistryName(registryName);

        FHContent.registeredFHBlocks.add(this);
        Item item = createItemBlock.apply(this, new Item.Properties().group(FHMain.itemGroup));
        if (item != null) {
            item.setRegistryName(registryName);
            FHContent.registeredFHItems.add(item);
        }
		this.type=type;
		
		BlockState defaultState = getDefaultState().with(BlockStateProperties.WATERLOGGED, false);
		for (Direction d : Direction.values())
			defaultState = defaultState.with(FACING_TO_PROPERTY_MAP.get(d),false);
		this.setDefaultState(defaultState);
	}


	public BlockState getAxisState(Axis axis) {
		BlockState defaultState = getDefaultState();
		for (Direction d : Direction.values())
			defaultState = defaultState.with(FACING_TO_PROPERTY_MAP.get(d), d.getAxis() == axis);
		return defaultState;
	}
	
	@Nullable
	private Axis getAxis(IBlockReader world, BlockPos pos, BlockState state) {
		if(!type.isInstance(state.getBlock()))return null;
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

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}


	public boolean canConnectTo(IBlockDisplayReader world, BlockPos neighbourPos, BlockState neighbour, Direction direction) {
		if (neighbour.getBlock() instanceof ISteamEnergyBlock)
			return true;
		return false;
	}

	public boolean shouldDrawRim(IBlockDisplayReader world, BlockPos pos, BlockState state,
		Direction direction) {
		if(!isOpenAt(state,direction))
			return false;
		BlockPos offsetPos = pos.offset(direction);
		BlockState facingState = world.getBlockState(offsetPos);
		if (!type.isInstance(facingState.getBlock()))
			return true;
		if (!canConnectTo(world, offsetPos, facingState, direction))
			return true;
		if (!isCornerOrEndPipe(world, pos, state))
			return false;
		if (getAxis(world,pos,facingState) != null)
			return true;
		if (!shouldDrawCasing(world, pos, state) && shouldDrawCasing(world, offsetPos, facingState))
			return true;
		if (isCornerOrEndPipe(world, offsetPos, facingState))
			return direction.getAxisDirection() == AxisDirection.POSITIVE;
		return true;
	}

	public boolean isOpenAt(BlockState state, Direction direction) {
		return state.get(FACING_TO_PROPERTY_MAP.get(direction));
	}

	public boolean isCornerOrEndPipe(IBlockDisplayReader world, BlockPos pos, BlockState state) {
		return (type.isInstance(state.getBlock())) && getAxis(world,pos,state) == null
			&& !shouldDrawCasing(world, pos, state);
	}

	public boolean shouldDrawCasing(IBlockDisplayReader world, BlockPos pos, BlockState state) {
		if (!type.isInstance(state.getBlock()))
			return false;
		Axis axis=getAxis(world,pos,state);
		if(axis==null)return false;
		for (Direction direction : Direction.values())
			if (direction.getAxis() != axis && isOpenAt(state, direction))
				return true;
		return false;
	}
	@Override
	protected void fillStateContainer(net.minecraft.state.StateContainer.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN,CASING, BlockStateProperties.WATERLOGGED);
		for(BooleanProperty rim:RIM_MAP.values()){
			builder.add(rim);
		}
		super.fillStateContainer(builder);
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
	public BlockState updatePostPlacement(BlockState state, Direction direction, BlockState neighbourState,
		IWorld world, BlockPos pos, BlockPos neighbourPos) {
		if (state.get(BlockStateProperties.WATERLOGGED))
			world.getPendingFluidTicks()
				.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		if (isOpenAt(state, direction) && neighbourState.hasProperty(BlockStateProperties.WATERLOGGED))
			world.getPendingBlockTicks()
				.scheduleTick(pos, this, 1, TickPriority.HIGH);
		return updateBlockState(state, direction, direction.getOpposite(), world, pos);
	}

	public BlockState updateBlockState(BlockState state, Direction preferredDirection, @Nullable Direction ignore,
		IWorld world, BlockPos pos) {

		for (Direction d : Direction.values())
			if (d != ignore) {
				state = state.with(FACING_TO_PROPERTY_MAP.get(d),canConnectTo(world, pos.offset(d), world.getBlockState(pos.offset(d)), d));
				state = state.with(RIM_MAP.get(d),shouldDrawRim(world,pos,state,d));
			}
		if(shouldDrawCasing(world, pos, state)) {
			state=state.with(CASING,true);
		}else {
			state=state.with(CASING,false);
		}
		System.out.println(shouldDrawCasing(world, pos, state));
		return state;
		/*BracketedTileEntityBehaviour bracket = TileEntityBehaviour.get(world, pos, BracketedTileEntityBehaviour.TYPE);
		if (bracket != null && bracket.isBracketPresent())
			return state;

		BlockState prevState = state;
		int prevStateSides = (int) Arrays.stream(Iterate.directions)
				.map(PROPERTY_BY_DIRECTION::get)
				.filter(prevState::getValue)
				.count();

		// Update sides that are not ignored
		for (Direction d : Iterate.directions)
			if (d != ignore) {
				boolean shouldConnect = canConnectTo(world, pos.relative(d), world.getBlockState(pos.relative(d)), d);
				state = state.setValue(PROPERTY_BY_DIRECTION.get(d), shouldConnect);
			}

		// See if it has enough connections
		Direction connectedDirection = null;
		for (Direction d : Iterate.directions) {
			if (isOpenAt(state, d)) {
				if (connectedDirection != null)
					return state;
				connectedDirection = d;
			}
		}

		// Add opposite end if only one connection
		if (connectedDirection != null)
			return state.setValue(PROPERTY_BY_DIRECTION.get(connectedDirection.getOpposite()), true);

		// If we can't connect to anything and weren't connected before, do nothing
		if (prevStateSides == 2)
			return prevState;

		// Use preferred
		return state.setValue(PROPERTY_BY_DIRECTION.get(preferredDirection), true)
				.setValue(PROPERTY_BY_DIRECTION.get(preferredDirection.getOpposite()), true);*/
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getStillFluidState(false)
			: Fluids.EMPTY.getDefaultState();
	}

	@Override
	public boolean allowsMovement(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
		return false;
	}


}