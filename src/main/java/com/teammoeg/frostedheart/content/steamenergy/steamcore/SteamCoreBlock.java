package com.teammoeg.frostedheart.content.steamenergy.steamcore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.simibubi.create.content.contraptions.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.utility.VoxelShaper;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.steamenergy.ISteamEnergyBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.*;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public class SteamCoreBlock extends DirectionalKineticBlock implements ISteamEnergyBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    static final VoxelShaper shape = VoxelShaper.forDirectional(VoxelShapes.or(Block.makeCuboidShape(0, 0, 0, 16, 16, 16)), Direction.SOUTH);


    public SteamCoreBlock( Properties blockProps) {
        super(blockProps);
        this.setDefaultState(this.stateContainer.getBaseState().with(LIT, Boolean.FALSE).with(BlockStateProperties.FACING, Direction.SOUTH));
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
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(LIT);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return shape.get(state.get(BlockStateProperties.FACING));
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState blockState) {
        return blockState.get(BlockStateProperties.FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(IWorldReader arg0, BlockPos arg1, BlockState state, Direction dir) {
        return dir == state.get(BlockStateProperties.FACING);
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        ActionResultType superResult = super.onBlockActivated(state, world, pos, player, hand, hit);
        return superResult;
    }


    @Override
    public boolean canConnectFrom(IWorld world, BlockPos pos, BlockState state, Direction dir) {
        return dir == state.get(BlockStateProperties.FACING);
    }


	@Override
	public Direction getPreferredFacing(BlockItemUseContext arg0) {
		Direction dir= super.getPreferredFacing(arg0);
				
		if(dir!=null)dir=dir.getOpposite();
		return dir;
	}
}
