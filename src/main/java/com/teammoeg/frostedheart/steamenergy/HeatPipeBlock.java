package com.teammoeg.frostedheart.steamenergy;

import java.util.Random;
import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.block.FHBaseBlock;
import com.teammoeg.frostedheart.content.FHTileTypes;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class HeatPipeBlock extends FHBaseBlock {

	public HeatPipeBlock(String name, Properties blockProps,
			BiFunction<Block, net.minecraft.item.Item.Properties, Item> createItemBlock) {
		super(name, blockProps, createItemBlock);
	}


    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.HEATPIPE.get().create();
    }

    @Override
	public void tick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand) {
    	TileEntity te=Utils.getExistingTileEntity(worldIn,pos);
    	if(te instanceof HeatPipeTileEntity)
    		((HeatPipeTileEntity) te).findPathToMaster(null);
	}


	@Override
	public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
			boolean isMoving) {
		TileEntity te=Utils.getExistingTileEntity(worldIn,pos);
		((HeatPipeTileEntity) te).findPathToMaster(null);
		super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
	}

	@Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }




}
