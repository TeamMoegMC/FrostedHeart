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

/**
 * 世界级别的数据存储容器。为每个世界/维度提供独立的 {@link DataHolderMap} 数据持有者，
 * 用于存储与特定世界关联的数据组件。
 * <p>
 * World-level data storage container. Provides an independent {@link DataHolderMap} data holder for each world/dimension,
 * used to store data components associated with a specific world.
 */
public class WorldDataStorage {
	/**
	 * 世界数据持有者映射的内部实现类。
	 * <p>
	 * Internal implementation class for the world data holder map.
	 */
	public static class WorldDataHolderMap extends DataHolderMap<WorldDataHolderMap>{

		/**
		 * 构造一个新的世界数据持有者映射。
		 * <p>
		 * Constructs a new world data holder map.
		 *
		 * @param markerName 用于日志记录的标记名称 / the marker name used for logging
		 */
		public WorldDataHolderMap(String markerName) {
			super(markerName);
		}
		
	}
	WorldDataHolderMap map;
	/**
	 * 构造一个新的世界数据存储。
	 * <p>
	 * Constructs a new world data storage.
	 *
	 * @param name 用于日志标记的名称 / the name used for log markers
	 */
	public WorldDataStorage(String name) {
		map=new WorldDataHolderMap(name);

	}

}
