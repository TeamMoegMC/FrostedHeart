package com.teammoeg.frostedheart.town;

import java.util.function.Function;

public enum TownResourceType {
	WOOD(t->250D),
	IRON(t->250D),
	STONE(t->250D),
	TOOL(t->250D),
	RAW_FOOD(t->250D),
	PREP_FOOD(t->250D);
	Function<ITownResource,Double> maxStorage;
	private TownResourceType(Function<ITownResource, Double> maxStorage) {
		this.maxStorage = maxStorage;
	}
	public String getKey() {
		return this.name().toLowerCase();
	}
	public static TownResourceType from(String t) {
		return TownResourceType.valueOf(t.toUpperCase());
	}
	public double getMaxStorage(ITownResource rc) {
		return maxStorage.apply(rc);
	}
	public int getIntMaxStorage(ITownResource rc) {
		return (int) (maxStorage.apply(rc)*1000);
	}
}
