package com.teammoeg.frostedheart.content.steamenergy.steamcore;

import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.simibubi.create.content.contraptions.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.utility.VoxelShaper;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import net.minecraft.block.AbstractBlock.Properties;

public class SteamCoreBlock extends DirectionalKineticBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    static final VoxelShaper shape = VoxelShaper.forDirectional(VoxelShapes.or(Block.box(0, 0, 0, 16, 16, 16)), Direction.SOUTH);


    public SteamCoreBlock( Properties blockProps) {
        super(blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE).setValue(BlockStateProperties.FACING, Direction.SOUTH));
    }


    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.STEAM_CORE.get().create();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return shape.get(state.getValue(BlockStateProperties.FACING));
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState blockState) {
        return blockState.getValue(BlockStateProperties.FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(IWorldReader arg0, BlockPos arg1, BlockState state, Direction dir) {
        return dir == state.getValue(BlockStateProperties.FACING);
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        return super.use(state, world, pos, player, hand, hit);
    }

	@Override
	public Direction getPreferredFacing(BlockItemUseContext arg0) {
		Direction dir= super.getPreferredFacing(arg0);
				
		if(dir!=null)dir=dir.getOpposite();
		return dir;
	}


	@Override
	public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
		super.animateTick(stateIn, worldIn, pos, rand);
		if(stateIn.getValue(LIT)&&rand.nextBoolean())
			ClientUtils.spawnSteamParticles(worldIn, pos);
	}
}
