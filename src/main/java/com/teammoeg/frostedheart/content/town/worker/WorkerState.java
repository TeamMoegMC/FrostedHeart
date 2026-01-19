package com.teammoeg.frostedheart.content.town.worker;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.frostedheart.content.town.OccupiedArea;
import com.teammoeg.frostedheart.content.town.TownWorkerStatus;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;

public abstract class WorkerState {
	@Getter
	private List<UUID> residents=new ArrayList<>();
	@Getter
	@Setter
	private OccupiedArea occupiedArea=new OccupiedArea();
	@Getter
	@Setter
	protected double rating=-1;
	public int maxResidents;
	public TownWorkerStatus status=TownWorkerStatus.NOT_INITIALIZED;
	public static final Codec<List<UUID>> UUID_LIST_CODEC=Codec.list(CodecUtil.catchingCodec(Codec.STRING.xmap(UUID::fromString,UUID::toString)));
	public WorkerState() {
		
	}
	public void writeNBT(CompoundTag tag,boolean isNetwork) {
		CodecUtil.encodeNBT(UUID_LIST_CODEC, tag, "residents", new ArrayList<>(residents));
		tag.putInt("maxResidents", maxResidents);
		tag.put("occupiedArea", occupiedArea.toNBT());
		tag.putByte("status", (byte) status.getStateNum());
		tag.putDouble("rating",rating);
	};
	public void readNBT(CompoundTag tag,boolean isNetwork) {
		residents.clear();
		List<UUID> readResidents=CodecUtil.decodeNBT(UUID_LIST_CODEC, tag, "residents");
		if(readResidents!=null)
			residents.addAll(readResidents);
		maxResidents=tag.getInt("maxResidents");
		occupiedArea=OccupiedArea.fromNBT(tag.getCompound("occupiedArea"));
		status=TownWorkerStatus.fromByte(tag.getByte("status"));
		rating = tag.getDouble("rating");
	};
	public boolean addResident(UUID resident) {
		for(UUID uuid:residents) {
			if(uuid.equals(resident))
				return false;
		}
		return residents.add(resident);
	}
	public boolean removeResident(UUID resident) {
		return residents.remove(resident);
	}
	
}
