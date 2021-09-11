package com.teammoeg.frostedheart.steamenergy;

import java.util.EnumMap;

import com.teammoeg.frostedheart.content.FHTileTypes;
import com.teammoeg.frostedheart.state.FHBlockInterfaces;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class HeatPipeTileEntity extends IEBaseTileEntity implements EnergyNetworkProvider,FHBlockInterfaces.IActiveState{
	protected EnumMap<Direction,Integer> dMaster=new EnumMap<>(Direction.class);
	private SteamEnergyNetwork network;
	int length;
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
	protected void propagate(Direction from,SteamEnergyNetwork newNetwork,int lengthx) {
		if(network!=null&&newNetwork!=null&&network!=newNetwork) {//network conflict
			return;//disconnect
		}
		if(newNetwork==null) {
			unpropagate(from);
			return;
		}
		setActive(true);
		dMaster.put(from,lengthx);
		length=Integer.MAX_VALUE;
		for(int i:dMaster.values()) {
			length=Math.min(length,i);
		}
		if(network!=newNetwork) {
			network=newNetwork;
			for(Direction d:Direction.values()) {
				if(d==from)continue;
				BlockPos n=this.getPos().offset(d);
				TileEntity te = Utils.getExistingTileEntity(this.getWorld(),n);
				if (te instanceof HeatPipeTileEntity) {
					((HeatPipeTileEntity) te).propagate(d.getOpposite(),network,length+1);
				}
			}
		}
	}
	protected void unpropagate(Direction from) {
		dMaster.remove(from);
		if(dMaster.isEmpty()) {
			network=null;
			setActive(false);
			for(Direction d:Direction.values()) {
				if(d==from)continue;
				BlockPos n=this.getPos().offset(d);
				TileEntity te = Utils.getExistingTileEntity(this.getWorld(),n);
				if (te instanceof HeatPipeTileEntity) {
					((HeatPipeTileEntity) te).unpropagate(d.getOpposite());
				}
			}
		}else {
			length=Integer.MAX_VALUE;
			for(int i:dMaster.values()) {
				length=Math.min(length,i);
			}
			for(Direction d:Direction.values()) {
				if(d==from)continue;
				BlockPos n=this.getPos().offset(d);
				TileEntity te = Utils.getExistingTileEntity(this.getWorld(),n);
				if (te instanceof HeatPipeTileEntity) {
					((HeatPipeTileEntity) te).propagate(d.getOpposite(),network,length+1);
				}
			}
		}
	}
	/*
	protected int findPathToMaster(Direction from) {
		if(isVisiting)return -1;
		try {
			isVisiting=true;
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
				}else if(te instanceof HeatProvider) {
					SteamEnergyNetwork newNetwork=((HeatProvider) te).getNetwork();
					if(network!=null&&network!=newNetwork) {//network conflict
						dMaster.clear();
						return -1;//disconnect
					}
					network=newNetwork;
					result=1;
					dMaster.put(d,result);
				}
			}
				
			return result;
		}finally {
			isVisiting=false;
		}
	}
	*/
	public void disconnectAt(Direction to) {
		if(network==null)return;
		unpropagate(to);//try find new path
	}
	public void connectAt(Direction to) {
		TileEntity te = Utils.getExistingTileEntity(this.getWorld(),this.getPos().offset(to));
		if(te instanceof HeatProvider){
			SteamEnergyNetwork newNetwork=((HeatProvider) te).getNetwork();
			if(network==null) {
				this.propagate(to, newNetwork,1);
			}
			return;
		}
		if(network==null)return;
		if (te instanceof HeatPipeTileEntity) {
			((HeatPipeTileEntity) te).propagate(to.getOpposite(),network, length);
		}else {
			disconnectAt(to);
		}
	}
}
