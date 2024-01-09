package com.teammoeg.frostedheart.town;

import java.util.HashMap;
import java.util.Map;

public class TownResource implements ITownResource{
	Map<TownResourceType,Integer> storage;
	Map<TownResourceType,Integer> backupStorage;
	Map<TownResourceType,Integer> service=new HashMap<>();
	Map<TownResourceType,Integer> maxStorage=new HashMap<>();
	TownData town;
	public TownResource(TownData td) {
		super();
		this.storage = td.resources;
		this.backupStorage = td.backupResources;
		this.town=td;
	}
	private int getIntMaxStorage(TownResourceType name) {
		return maxStorage.computeIfAbsent(name, t->t.getIntMaxStorage(this));
	}
	@Override
	public double get(TownResourceType name) {
		int val=storage.getOrDefault(name, 0);
		val+=backupStorage.getOrDefault(name,0);
		val+=service.getOrDefault(name, 0);
		
		return val/1000d;
	}

	@Override
	public double add(TownResourceType name, double val, boolean simulate) {
		int newVal=storage.getOrDefault(name, 0);
		newVal+=val*1000;
		int max=getIntMaxStorage(name)-backupStorage.getOrDefault(name,0);
		int remain=0;
		if(newVal>max) {
			remain=newVal-max;
			newVal=max;
		}
		if(!simulate)
			storage.put(name, newVal);
		return val-remain/1000d;
	}

	@Override
	public double addService(TownResourceType name, double val) {
		storage.compute(name, (n,t)->(int)(t==null?val*1000:t+val*1000));
		return val;
	}

	@Override
	public double cost(TownResourceType name, double val, boolean simulate) {
		int servVal=service.getOrDefault(name, 0);
		int curVal=storage.getOrDefault(name, 0);
		int buVal=backupStorage.getOrDefault(name, 0);
		int remain=0;
		servVal-=val*1000;
		if(servVal<0) {
			curVal+=servVal;
			servVal=0;
		}
		if(curVal<0) {
			buVal+=curVal;
			curVal=0;
		}
		if(buVal<0) {
			remain=-buVal;
			buVal=0;
		}
		if(!simulate) {
			if(servVal>0) {
				service.put(name, servVal);
			}else {
				service.remove(name);
			}
			if(curVal>0) {
				storage.put(name, curVal);
			}else {
				storage.remove(name);
			}
			if(buVal>0) {
				backupStorage.put(name, buVal);
			}else {
				backupStorage.remove(name);
			}
		}
		return val-remain/1000d;
	}
	@Override
	public double costService(TownResourceType name, double val, boolean simulate) {
		int servVal=service.getOrDefault(name, 0);
		int remain=0;
		int ival=(int) (val*1000);
		if(servVal>=ival) {
			servVal-=ival;
		}else {
			remain=ival-servVal;
			servVal=0;
		}
		if(!simulate) {
			if(servVal>0) {
				service.put(name, servVal);
			}else {
				service.remove(name);
			}
		}
		return val-remain/1000d;
	}
	@Override
	public TownData getTown() {
		// TODO Auto-generated method stub
		return town;
	}
}
