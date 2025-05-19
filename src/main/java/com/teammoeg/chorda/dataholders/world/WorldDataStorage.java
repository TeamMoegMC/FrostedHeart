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
