package com.teammoeg.frostedheart.content.town;

import java.util.function.Supplier;

public enum TerrainResourceType {
	WOOD,ORE,HUNT,POI;
	private Supplier<Double> recoverSpeed;
	private Supplier<Double> resourcePerSq;
	
}
