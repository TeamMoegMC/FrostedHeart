package com.teammoeg.frostedheart.steamenergy;

import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;

public interface ISteamEnergyBlock {
	boolean canConnectFrom(IBlockDisplayReader world,BlockPos pos,BlockState state,Direction dir);
}
