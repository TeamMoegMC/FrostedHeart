package com.teammoeg.frostedheart.trade;

import net.minecraft.nbt.CompoundNBT;

public class PlayerRelationData {
	public static final PlayerRelationData EMPTY=new PlayerRelationData();
	int totalbenefit;
	int sawmurder;
	
	long lastUpdated;
	public void update(long day) {
		long delta=day-lastUpdated;
		if(delta>0) {
			totalbenefit=(int) (totalbenefit*Math.pow(0.75f, delta));
		}
		lastUpdated=day;
	}
	public CompoundNBT serialize(CompoundNBT data) {
		
		data.putInt("murder",sawmurder);
		data.putInt("benefit", totalbenefit);
		
		data.putLong("last", lastUpdated);
		return data;
	}
	public void deserialize(CompoundNBT data) {
		
		sawmurder=data.getInt("murder");
		totalbenefit=data.getInt("benefit");
		
		lastUpdated=data.getLong("last");
	}
}
