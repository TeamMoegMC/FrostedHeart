/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.town.terrainresource;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

public enum TerrainResourceType {
	WOOD(FHConfig.SERVER.TOWN.RESOURCE.treeRecovery,FHConfig.SERVER.TOWN.RESOURCE.treeCount),
	ORE(FHConfig.SERVER.TOWN.RESOURCE.oreRecovery,FHConfig.SERVER.TOWN.RESOURCE.oreCount),
	HUNT(FHConfig.SERVER.TOWN.RESOURCE.huntRecovery,FHConfig.SERVER.TOWN.RESOURCE.huntCount),
	POI(FHConfig.SERVER.TOWN.RESOURCE.poiRecovery,FHConfig.SERVER.TOWN.RESOURCE.poiCount),//鸭蛋：khj写的，不知道是啥
	SALVAGE(FHConfig.SERVER.TOWN.RESOURCE.salvageRecovery,FHConfig.SERVER.TOWN.RESOURCE.salvageCount);//暂无用途
	private final Supplier<Double> recoverSpeed;
	private final Supplier<Double> resourcePerSq;
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
