package com.teammoeg.frostedheart.steamenergy;

import java.util.EnumMap;

import com.teammoeg.frostedheart.content.FHTileTypes;
import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class HeatPipeTileEntity extends IEBaseTileEntity{
	protected EnumMap<Direction,Integer> dMaster;
	private SteamEnergyNetwork network;
	public HeatPipeTileEntity() {
		super(FHTileTypes.HEATPIPE.get());
	}

	@Override
	public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
	}

	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
	}
	public SteamEnergyNetwork getNetwork() {
		return network;
	}
	protected int findPathToMaster(Direction from) {
		int result=-1;
		dMaster.remove(from);//avoid cycle detection
		if(!dMaster.isEmpty()) {
			for(int i:dMaster.values())
				result=Math.min(result,i);
			return result;
		}
		network=null;//assume no network
		for(Direction d:Direction.values()) {
			if(d==from)continue;
			BlockPos n=this.getPos().add(d.getDirectionVec());
			TileEntity te = Utils.getExistingTileEntity(this.getWorld(),n);
			if (te instanceof HeatPipeTileEntity) {
				int rs=((HeatPipeTileEntity) te).findPathToMaster(d.getOpposite())+1;
				if(rs>0){
					if(result==-1)
						result=rs;
					else
						result=Math.min(rs,result);
					dMaster.put(d,rs);
					SteamEnergyNetwork newNetwork=((HeatPipeTileEntity) te).getNetwork();
					if(network!=null&&network!=newNetwork) {//network conflict
						dMaster.clear();
						return -1;//disconnect
					}
					network=newNetwork;
				}
			}
		}
			
		return result;
	}
	public void disconnectAt(Direction from) {
		findPathToMaster(from);//try find new path
	}
	public void connectAt(Direction from) {
		findPathToMaster(from);
	}
}
