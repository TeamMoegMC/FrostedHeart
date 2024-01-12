/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.climate.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.climate.player.ITempAdjustFood;
import com.teammoeg.frostedheart.climate.player.IWarmKeepingEquipment;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fluids.FluidStack;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class FHDataManager {
    public static enum FHDataType {
        Armor(new DataType<>(ArmorTempData.class, "temperature", "armor")),
        Biome(new DataType<>(BiomeTempData.class, "temperature", "biome")),
        Food(new DataType<>(FoodTempData.class, "temperature", "food")),
        Block(new DataType<>(BlockTempData.class, "temperature", "block")),
        Drink(new DataType<>(DrinkTempData.class, "temperature", "drink")),
        Cup(new DataType<>(CupData.class, "temperature", "cup")),
        World(new DataType<>(WorldTempData.class, "temperature", "world"));

        static class DataType<T extends JsonDataHolder> {
            final Class<T> dataCls;
            final String location;
            final String domain;

            public DataType(Class<T> dataCls, String domain, String location) {
                this.location = location;
                this.dataCls = dataCls;
                this.domain = domain;
            }

            public T create(JsonObject jo) {
                try {
                    return dataCls.getConstructor(JsonObject.class).newInstance(jo);
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                         | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    // TODO Auto-generated catch block
                    throw new RuntimeException(e);
                }
            }

            public String getLocation() {
                return domain + "/" + location;
            }
        }

        public final DataType<? extends JsonDataHolder> type;

        private FHDataType(DataType<? extends JsonDataHolder> type) {
            this.type = type;
        }

    }

    public static class ResourceMap<T extends JsonDataHolder> extends HashMap<ResourceLocation, T> {
        /**
         *
         */
        private static final long serialVersionUID = 1564047056157250446L;

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

    @SuppressWarnings("rawtypes")
    public static final EnumMap<FHDataType, ResourceMap> ALL_DATA = new EnumMap<>(FHDataType.class);
    public static boolean synched = false;
    private static final JsonParser parser = new JsonParser();

    static {
        for (FHDataType dt : FHDataType.values()) {
            ALL_DATA.put(dt, new ResourceMap<>());
        }
    }

    public static final void reset() {
        synched = false;
        for (ResourceMap<?> rm : ALL_DATA.values())
            rm.clear();
    }

    @SuppressWarnings("unchecked")
    public static final void register(FHDataType dt, JsonObject data) {
        JsonDataHolder jdh = dt.type.create(data);
        //System.out.println("registering "+dt.type.location+": "+jdh.getId());
        ALL_DATA.get(dt).put(jdh.getId(), jdh);
        synched = false;
    }

    @SuppressWarnings("unchecked")
    public static final <T extends JsonDataHolder> ResourceMap<T> get(FHDataType dt) {
        return ALL_DATA.get(dt);

    }

    @SuppressWarnings("unchecked")
    public static final void load(DataEntry[] entries) {
        reset();
        for (DataEntry de : entries) {
            JsonDataHolder jdh = de.type.type.create(parser.parse(de.data).getAsJsonObject());
            //System.out.println("registering "+dt.type.location+": "+jdh.getId());
            ALL_DATA.get(de.type).put(jdh.getId(), jdh);
        }
    }

    @SuppressWarnings("rawtypes")
    public static final DataEntry[] save() {
        int tsize = 0;
        for (ResourceMap map : ALL_DATA.values()) {
            tsize += map.size();
        }
        DataEntry[] entries = new DataEntry[tsize];
        int i = -1;
        for (Entry<FHDataType, ResourceMap> entry : ALL_DATA.entrySet()) {
            for (Object jdh : entry.getValue().values()) {
                entries[++i] = new DataEntry(entry.getKey(), ((JsonDataHolder) jdh).getData());
            }
        }
        return entries;
    }

    public static ITempAdjustFood getFood(ItemStack is) {
        CupData data = FHDataManager.<CupData>get(FHDataType.Cup).get(is.getItem().getRegistryName());
        ResourceMap<FoodTempData> foodData = FHDataManager.get(FHDataType.Food);
        if (data != null) {
            return new CupTempAdjustProxy(data.getEfficiency(), foodData.get(is.getItem().getRegistryName()));
        }
        return foodData.get(is.getItem().getRegistryName());
    }

    public static IWarmKeepingEquipment getArmor(ItemStack is) {
        //System.out.println(is.getItem().getRegistryName());
        return FHDataManager.<ArmorTempData>get(FHDataType.Armor).get(is.getItem().getRegistryName());
    }

    public static IWarmKeepingEquipment getArmor(String is) {
        //System.out.println(is.getItem().getRegistryName());
        return FHDataManager.<ArmorTempData>get(FHDataType.Armor).get(new ResourceLocation(is));
    }

    public static Float getBiomeTemp(Biome b) {
        if (b == null) return 0f;
        BiomeTempData data = FHDataManager.<BiomeTempData>get(FHDataType.Biome).get(b.getRegistryName());
        if (data != null)
            return data.getTemp();
        return 0F;
    }

    public static Float getWorldTemp(World w) {
        WorldTempData data = FHDataManager.<WorldTempData>get(FHDataType.World).get(w.getDimensionKey().getLocation());
        if (data != null)
            return data.getTemp();
        return null;
    }

    public static BlockTempData getBlockData(Block b) {
        return FHDataManager.<BlockTempData>get(FHDataType.Block).get(b.getRegistryName());
    }

    public static BlockTempData getBlockData(ItemStack b) {
        return FHDataManager.<BlockTempData>get(FHDataType.Block).get(b.getItem().getRegistryName());
    }

    public static float getDrinkHeat(FluidStack f) {
        DrinkTempData dtd = FHDataManager.<DrinkTempData>get(FHDataType.Drink).get(f.getFluid().getRegistryName());
        if (dtd != null)
            return dtd.getHeat();
        return -0.3f;
    }
}
