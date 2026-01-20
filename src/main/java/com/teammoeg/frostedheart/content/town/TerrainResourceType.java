package com.teammoeg.frostedheart.content.town;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

public enum TerrainResourceType {
	WOOD(FHConfig.SERVER.TOWN.RESOURCE.treeRecovery,FHConfig.SERVER.TOWN.RESOURCE.treeCount),
	ORE(FHConfig.SERVER.TOWN.RESOURCE.oreRecovery,FHConfig.SERVER.TOWN.RESOURCE.oreCount),
	HUNT(FHConfig.SERVER.TOWN.RESOURCE.huntRecovery,FHConfig.SERVER.TOWN.RESOURCE.huntCount),
	POI(FHConfig.SERVER.TOWN.RESOURCE.poiRecovery,FHConfig.SERVER.TOWN.RESOURCE.poiCount),
	SALVAGE(FHConfig.SERVER.TOWN.RESOURCE.salvageRecovery,FHConfig.SERVER.TOWN.RESOURCE.salvageCount);
	private Supplier<Double> recoverSpeed;
	private Supplier<Double> resourcePerSq;
	private TerrainResourceType(Supplier<Double> recoverSpeed, Supplier<Double> resourcePerSq) {
		this.recoverSpeed = recoverSpeed;
		this.resourcePerSq = resourcePerSq;
	}
	public double getRecoverSpeed() {
		return recoverSpeed.get();
	}
	public double getResourcePerSq() {
		return resourcePerSq.get();
	}
}
