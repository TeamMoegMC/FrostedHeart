package com.teammoeg.frostedheart.block;

import com.teammoeg.frostedheart.steamenergy.IConnectable;
import com.teammoeg.frostedheart.steamenergy.ISteamEnergyBlock;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.RegistryObject;

public class HeatedGeneratorMultiBlock extends GeneratorMultiblockBlock implements ISteamEnergyBlock{

	public HeatedGeneratorMultiBlock(String name, RegistryObject type) {
		super(name, type);
	}


	@Override
	public boolean canConnectFrom(IBlockDisplayReader world, BlockPos pos, BlockState state, Direction dir) {
		if(dir==Direction.UP)
			return true;
		return false;
	}
    @Override
	public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
			boolean isMoving) {
		TileEntity te=Utils.getExistingTileEntity(worldIn,fromPos);
		if(te instanceof IConnectable) {
			Vector3i vec=pos.subtract(fromPos);
			Direction dir=Direction.getFacingFromVector(vec.getX(),vec.getY(),vec.getZ());
			((IConnectable) te).connectAt(dir);
		}
	}
    
}
