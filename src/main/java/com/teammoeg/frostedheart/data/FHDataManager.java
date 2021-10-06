/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.climate.ITempAdjustFood;
import com.teammoeg.frostedheart.climate.IWarmKeepingEquipment;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fluids.FluidStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class FHDataManager {
    public static class ResourceMap<T extends JsonDataHolder> extends HashMap<ResourceLocation, T> {
        public ResourceMap() {
            super();
        }

        public ResourceMap(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        public ResourceMap(int initialCapacity) {
            super(initialCapacity);
        }

        public ResourceMap(Map<? extends ResourceLocation, ? extends T> m) {
            super(m);
        }
    }

    private FHDataManager() {
    }

    public static final ResourceMap<FoodTempData> foodData = new ResourceMap<>();
    public static final ResourceMap<ArmorTempData> armorData = new ResourceMap<>();
    public static final ResourceMap<BiomeTempData> biomeData = new ResourceMap<>();
    public static final ResourceMap<BlockTempData> blockData = new ResourceMap<>();
    public static final ResourceMap<DrinkTempData> drinkData = new ResourceMap<>();
    public static final EnumMap<FHDataTypes, ResourceMap> datas = new EnumMap<>(FHDataTypes.class);
    public static boolean synched=false;
    private static final JsonParser parser = new JsonParser();
    static {
        datas.put(FHDataTypes.Armor, armorData);
        datas.put(FHDataTypes.Biome, biomeData);
        datas.put(FHDataTypes.Block, blockData);
        datas.put(FHDataTypes.Food, foodData);
        datas.put(FHDataTypes.Drink,drinkData);
    }
    public static final void reset() {
    	synched=false;
    	for(ResourceMap rm:datas.values())
    		rm.clear();
    }
    @SuppressWarnings("unchecked")
    public static final void register(FHDataTypes dt, JsonObject data) {
        JsonDataHolder jdh = dt.type.create(data);
        //System.out.println("registering "+dt.type.location+": "+jdh.getId());
        datas.get(dt).put(jdh.getId(), jdh);
        synched=false;
    }
    @SuppressWarnings("unchecked")
	public static final void load(DataEntry[] entries) {
    	reset();
    	for(DataEntry de:entries) {
	        JsonDataHolder jdh = de.type.type.create(parser.parse(de.data).getAsJsonObject());
	        //System.out.println("registering "+dt.type.location+": "+jdh.getId());
	        datas.get(de.type).put(jdh.getId(), jdh);
    	}
    }
    public static final DataEntry[] save() {
    	int tsize=0;
    	for(ResourceMap map:datas.values()) {
    		tsize+=map.size();
    	}
    	DataEntry[] entries=new DataEntry[tsize];
    	int i=-1;
    	for(Entry<FHDataTypes, ResourceMap> entry:datas.entrySet()) {
    		for(Object jdh:entry.getValue().values()) {
    			entries[++i]=new DataEntry(entry.getKey(),((JsonDataHolder)jdh).getData());
    		}
    	}
    	return entries;
    }
    public static ITempAdjustFood getFood(ItemStack is) {
        return foodData.get(is.getItem().getRegistryName());
    }

    public static IWarmKeepingEquipment getArmor(ItemStack is) {
        //System.out.println(is.getItem().getRegistryName());
        return armorData.get(is.getItem().getRegistryName());
    }
    public static IWarmKeepingEquipment getArmor(String is) {
        //System.out.println(is.getItem().getRegistryName());
        return armorData.get(new ResourceLocation(is));
    }

    public static Byte getBiomeTemp(Biome b) {
        BiomeTempData data = biomeData.get(b.getRegistryName());
        if (data != null)
            return data.getTemp().byteValue();
        return null;
    }

    public static BlockTempData getBlockData(Block b) {
        return blockData.get(b.getRegistryName());
    }
    public static BlockTempData getBlockData(ItemStack b) {
        return blockData.get(b.getItem().getRegistryName());
    }
    public static float getDrinkHeat(FluidStack f) {
    	DrinkTempData dtd=drinkData.get(f.getFluid().getRegistryName());
    	if(dtd!=null)
    		return dtd.getHeat();
    	return -0.3f;
    }
}
