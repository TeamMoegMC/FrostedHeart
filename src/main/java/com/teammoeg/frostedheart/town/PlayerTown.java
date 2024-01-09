package com.teammoeg.frostedheart.town;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;



public class PlayerTown implements Town{
	Map<TownResourceType,Integer> storage;
	Map<TownResourceType,Integer> backupStorage;
	Map<TownResourceType,Integer> service=new EnumMap<>(TownResourceType.class);
	Map<TownResourceType,Integer> costedService=new EnumMap<>(TownResourceType.class);
	Map<TownResourceType,Integer> maxStorage=new EnumMap<>(TownResourceType.class);
	TeamTownData town;
	public PlayerTown(TeamTownData td) {
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
		storage.merge(name,(int)val*1000,Integer::sum);
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
	public double costAsService(TownResourceType name, double val, boolean simulate) {
		int toCost=(int) (val*1000);
		int curVal=storage.getOrDefault(name, 0);
		//int buVal=backupStorage.getOrDefault(name, 0);
		int servVal=service.getOrDefault(name, 0);
		int remain=0;
		int costedRC=0;
		servVal-=toCost;
		if(servVal<0) {
			costedRC=-servVal;
			curVal+=servVal;
			servVal=0;
		}
		if(curVal<0) {
			remain=-curVal;
			curVal=0;
		}
		
		/*if(buVal<0) {
			remain=-buVal;
			buVal=0;
		}*/
		costedRC-=remain;
		int costed=toCost-remain;
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
			/*if(buVal>0) {
				backupStorage.put(name, buVal);
			}else {
				backupStorage.remove(name);
			}*/
			if(costedRC>0) {
				costedRC+=costedService.getOrDefault(name,0);
				costedService.put(name, costedRC);
			}
		}
		return costed/1000d;
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
	public Optional<TeamTownData> getTownData() {
		return Optional.of(town);
	}
	public void finishWork() {
		costedService.forEach((k,v)->storage.put(k, v));
		for(Entry<TownResourceType, Integer> ent:storage.entrySet()) {
			int max=getIntMaxStorage(ent.getKey());
			if(ent.getValue()>max) {
				ent.setValue(max);
			}
		}
	}
}
