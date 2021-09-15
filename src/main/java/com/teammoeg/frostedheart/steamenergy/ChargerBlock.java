package com.teammoeg.frostedheart.steamenergy;

import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.block.FHGuiBlock;
import com.teammoeg.frostedheart.content.FHTileTypes;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class ChargerBlock extends FHGuiBlock  implements ISteamEnergyBlock{
	
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
	public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
			boolean isMoving) {
		TileEntity te=Utils.getExistingTileEntity(worldIn,pos);
		if(te instanceof ChargerTileEntity) {
			Vector3i vec=fromPos.subtract(pos);
			Direction dir=Direction.getFacingFromVector(vec.getX(),vec.getY(),vec.getZ());
			((IConnectable) te).connectAt(dir);
		}
	}

}
