package com.teammoeg.frostedheart.climate;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;
/**
 * An anti command and sleep clock source
 * 
 * */
public class WorldClockSource {
	long secs;
	long lastdaytime;
	public WorldClockSource() {
	}
	public void update(ServerWorld w) {
		update(w.getDayTime());
	}
	public void update(long newTime) {
		long cdt=newTime;
		long dt=cdt-lastdaytime;
		if(dt<0) {
			long nextday=lastdaytime+24000L;
			nextday=nextday-nextday%24000L;//assumpt it's next day and continue
			dt=cdt+nextday-lastdaytime;
		}
		
		long ndt=cdt-dt%20;
		secs+=dt/20;
		lastdaytime=ndt;
	}
	public int getHourInDay() {
		return (int) ((secs/50)%24);
	}
	public long getDate() {
		return (secs/50)/24;
	}
	public long getMonth() {
		return (secs/50)/24/30;
	}
	public long getTimeSecs() {
		return secs;
	}
	public CompoundNBT serialize(CompoundNBT cnbt) {
		cnbt.putLong("secs",secs);
		cnbt.putLong("last", lastdaytime);
		return cnbt;
	}
	public CompoundNBT serialize() {
		return serialize(new CompoundNBT());
	}
	public void read(CompoundNBT cnbt) {
		secs=cnbt.getLong("secs");
		lastdaytime=cnbt.getLong("last");
	}
}
