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

package com.teammoeg.chorda.dataholders.world;

import com.teammoeg.chorda.dataholders.DataHolderMap;
import com.teammoeg.chorda.util.CDistHelper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;

public class WorldDataStorage {
	public static class WorldDataHolderMap extends DataHolderMap<WorldDataHolderMap>{

		public WorldDataHolderMap(String markerName) {
			super(markerName);
		}
		
	}
	WorldDataHolderMap map;
	public WorldDataStorage(String name) {
		map=new WorldDataHolderMap(name);

	}

}
