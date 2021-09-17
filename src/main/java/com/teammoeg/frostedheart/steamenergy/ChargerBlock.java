package com.teammoeg.frostedheart.steamenergy;

import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.block.FHBaseBlock;
import com.teammoeg.frostedheart.content.FHTileTypes;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class ChargerBlock extends FHBaseBlock  implements ISteamEnergyBlock{
	
	public ChargerBlock(String name, Properties blockProps,
			BiFunction<Block, net.minecraft.item.Item.Properties, Item> createItemBlock) {
		super(name, blockProps, createItemBlock);
	}


    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.CHARGER.get().create();
    }
    
    @Override
	protected void fillStateContainer(Builder<Block, BlockState> builder) {
    	builder.add(BlockStateProperties.FACING);
		super.fillStateContainer(builder);
	}


	@Override
	public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
			boolean isMoving) {
		TileEntity te=Utils.getExistingTileEntity(worldIn,pos);
		if(te instanceof ChargerTileEntity) {
			Vector3i vec=fromPos.subtract(pos);
			Direction dir=Direction.getFacingFromVector(vec.getX(),vec.getY(),vec.getZ());
			((IConnectable) te).connectAt(dir);
		}
	}


	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		
		return this.getDefaultState().with(BlockStateProperties.FACING,context.getFace().getOpposite());
	}


	@Override
	public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
	{
		ActionResultType superResult = super.onBlockActivated(state, world, pos, player, hand, hit);
		if(superResult.isSuccessOrConsume())
			return superResult;
		ItemStack item=player.getHeldItem(hand);
		TileEntity te=Utils.getExistingTileEntity(world,pos);
		if(te instanceof ChargerTileEntity) {
			//if(item.getItem() instanceof IChargable) {
				return ((ChargerTileEntity) te).onClick(player,item);
			//}
		}
		return superResult;
	}


	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}


	@Override
	public boolean canConnectFrom(IBlockDisplayReader world, BlockPos pos, BlockState state, Direction dir) {
		return dir==state.get(BlockStateProperties.FACING).getOpposite();
	}

}
