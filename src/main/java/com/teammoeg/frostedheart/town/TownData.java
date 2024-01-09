package com.teammoeg.frostedheart.town;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import blusunrize.immersiveengineering.common.util.Utils;

import java.util.PriorityQueue;

import com.teammoeg.frostedheart.research.data.TeamResearchData;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
/**
 * Town data for a whole team.
 * 
 * */
public class TownData {
	Map<TownResourceType,Integer> resources=new HashMap<>();
	Map<TownResourceType,Integer> backupResources=new HashMap<>();
	Map<BlockPos,TownWorkerData> blocks=new LinkedHashMap<>();
	TeamResearchData team;
	public TownData(TeamResearchData team) {
		super();
		this.team = team;
	}
	public CompoundNBT serialize(boolean updatePacket) {
		CompoundNBT nbt=new CompoundNBT();
		if(!updatePacket) {
			ListNBT list=new ListNBT();
			for(TownWorkerData v:blocks.values()) {
				list.add(v.serialize());
			}
			nbt.put("blocks", list);
		}
		CompoundNBT list2=new CompoundNBT();
		for(Entry<TownResourceType, Integer> v:resources.entrySet()) {
			list2.putInt(v.getKey().getKey(), v.getValue());
			
		}
		nbt.put("resource", list2);
		CompoundNBT list3=new CompoundNBT();
		for(Entry<TownResourceType, Integer> v:backupResources.entrySet()) {
			list3.putInt(v.getKey().getKey(), v.getValue());
		}
		nbt.put("backupResource", list2);
		return nbt;
	}
	public void deserialize(CompoundNBT data, boolean updatePacket) {
		for(INBT i:data.getList("blocks", Constants.NBT.TAG_COMPOUND)) {
			CompoundNBT nbt=(CompoundNBT) i;
			TownWorkerData t=new TownWorkerData(nbt);
			blocks.put(t.getPos(), t);
		}
		CompoundNBT rec=data.getCompound("resource");
		for(String i:rec.keySet()) {
			resources.put(TownResourceType.from(i),rec.getInt(i));
		}
	}
	public void registerTownBlock(BlockPos pos,ITownBlockTE tile) {
		TownWorkerData data=blocks.computeIfAbsent(pos, TownWorkerData::new);
		data.fromBlock(tile);
	}
	public void removeTownBlock(BlockPos pos) {
		blocks.remove(pos);
	}
	public CompoundNBT getTownBlockData(BlockPos pos) {
		TownWorkerData twd=blocks.get(pos);
		if(twd==null)
			return null;
		return twd.getWorkData();
	}
	/**
	 * This tick only works per 20 tick.
	 * */
	public void tick(ServerWorld world) {
		PriorityQueue<TownWorkerData> pq=new PriorityQueue<TownWorkerData>(Comparator.comparingLong(TownWorkerData::getPriority).reversed());
		blocks.values().removeIf(v->{
			BlockPos pos=v.getPos();
			v.loaded=false;
			if(world.isBlockLoaded(pos)) {
				v.loaded=true;
				BlockState bs=world.getBlockState(pos);
				TileEntity te=Utils.getExistingTileEntity(world, pos);
				TownWorkerType twt=v.getType();
				if(twt.getBlock()!=bs.getBlock()||te==null||!(te instanceof ITownBlockTE)||!((ITownBlockTE)te).isWorkValid()) {
					return true;
				}
			}
			return false;
		});
		for(TownWorkerData v:blocks.values()) {
			pq.add(v);
		}
		ITownResource itt=new TownResource(this);
		for(TownWorkerData t:pq) {
			t.beforeWork(itt);
		}
		for(TownWorkerData t:pq) {
			t.work(itt);
		}
		for(TownWorkerData t:pq) {
			t.afterWork(itt);
		}
		for(TownWorkerData t:pq) {
			t.setData(world);
		}
	}
}
