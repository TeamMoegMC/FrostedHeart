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
		long dt=newTime-lastdaytime;
		if(dt<0) {// if time run backwards, it's command done the trick
			long nextday=lastdaytime+24000L;
			nextday=nextday-nextday%24000L;
			dt=newTime%24000L+nextday-lastdaytime;//assumpt it's next day and continue
		}
		secs+=dt/20;
		lastdaytime=newTime-newTime%20;
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
	public long getHours() {
		return (secs/50);
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
