package com.teammoeg.frostedheart.town;

import java.util.function.Function;

public enum TownResourceType {
	/** Storage space */
	STORAGE(null),
	/** Residents */
	RESIDENT(t->1000D),
	/** Work force */
	WORK(null),
	/** Generator power */
	HEAT(null),
	WOOD(t->250D+100*t.get(STORAGE)),
	IRON(t->250D+100*t.get(STORAGE)),
	STONE(t->250D+100*t.get(STORAGE)),
	TOOL(t->250D+100*t.get(STORAGE)),
	RAW_FOOD(t->250D+100*t.get(STORAGE)),
	PREP_FOOD(t->250D+100*t.get(STORAGE))
	
	;
	Function<Town,Double> maxStorage;
	/**
	 * Create a new type
	 * @param maxStorage provider for max storage calculations. Must be null for services
	 * */
	private TownResourceType(Function<Town, Double> maxStorage) {
		this.maxStorage = maxStorage;
	}
	public String getKey() {
		return this.name().toLowerCase();
	}
	public static TownResourceType from(String t) {
		return TownResourceType.valueOf(t.toUpperCase());
	}
	public double getMaxStorage(Town rc) {
		if(maxStorage==null)return 0;
		return maxStorage.apply(rc);
	}
	public int getIntMaxStorage(Town rc) {
		if(maxStorage==null)return 0;
		return (int) (maxStorage.apply(rc)*1000);
	}
}
