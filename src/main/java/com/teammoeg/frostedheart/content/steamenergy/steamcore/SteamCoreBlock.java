package com.teammoeg.frostedheart.content.steamenergy.steamcore;

import java.util.function.Supplier;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.utility.VoxelShaper;
import com.teammoeg.frostedheart.FHBlockEntityTypes;
import com.teammoeg.frostedheart.base.block.FHEntityBlock;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;

public class SteamCoreBlock extends DirectionalKineticBlock implements FHEntityBlock{
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    static final VoxelShaper shape = VoxelShaper.forDirectional(Shapes.or(Block.box(0, 0, 0, 16, 16, 16)), Direction.SOUTH);


    public SteamCoreBlock( Properties blockProps) {
        super(blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE).setValue(BlockStateProperties.FACING, Direction.SOUTH));
    }



    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return shape.get(state.getValue(BlockStateProperties.FACING));
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState blockState) {
        return blockState.getValue(BlockStateProperties.FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader arg0, BlockPos arg1, BlockState state, Direction dir) {
        return dir == state.getValue(BlockStateProperties.FACING);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return super.use(state, world, pos, player, hand, hit);
    }

	@Override
	public Direction getPreferredFacing(BlockPlaceContext arg0) {
		Direction dir= super.getPreferredFacing(arg0);
				
		if(dir!=null)dir=dir.getOpposite();
		return dir;
	}


	@Override
	public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource rand) {
		super.animateTick(stateIn, worldIn, pos, rand);
		if(stateIn.getValue(LIT)&&rand.nextBoolean())
			ClientUtils.spawnSteamParticles(worldIn, pos);
	}



	@Override
	public Supplier<BlockEntityType<?>> getBlock() {
		return FHBlockEntityTypes.STEAM_CORE::get;
	}
}
