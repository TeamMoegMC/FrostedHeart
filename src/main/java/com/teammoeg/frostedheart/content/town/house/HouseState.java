package com.teammoeg.frostedheart.content.town.house;

import java.util.ArrayList;
import java.util.List;

import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;

public class HouseState extends WorkerState {
	@Getter
	double rating=-1,temperatureRating,decorationRating,spaceRating;
	@Getter
	List<BlockPos> beds=new ArrayList<>();
	public HouseState() {
		
	}
	@Override
	public void writeNBT(CompoundTag tag, boolean isNetwork) {
		super.writeNBT(tag, isNetwork);
		tag.putDouble("rating",rating);
        tag.putDouble("temperatureRating",temperatureRating);
        tag.putDouble("decorationRating",decorationRating);
        tag.putDouble("spaceRating",spaceRating);
        tag.put("beds", SerializeUtil.toNBTList(beds,(o,b)->b.addLong(o.asLong())));
	}

	@Override
	public void readNBT(CompoundTag tag, boolean isNetwork) {
		super.readNBT(tag, isNetwork);
		rating = tag.getDouble("rating");
        temperatureRating = tag.getDouble("temperatureRating");
        decorationRating = tag.getDouble("decorationRating");
        spaceRating = tag.getDouble("spaceRating");
        beds.clear();
        ListTag bedstag=tag.getList("beds", Tag.TAG_LONG);
        for(Tag bedtag:bedstag) {
        	beds.add(BlockPos.of(((LongTag)bedtag).getAsLong()));
        }
	}

}
