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

package com.teammoeg.frostedheart;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.content.climate.data.BiomeTempData;
import com.teammoeg.frostedheart.content.climate.data.BlockTempData;
import com.teammoeg.frostedheart.content.climate.data.CupData;
import com.teammoeg.frostedheart.content.climate.data.CupTempAdjustProxy;
import com.teammoeg.frostedheart.content.climate.data.DataReference;
import com.teammoeg.frostedheart.content.climate.data.DrinkTempData;
import com.teammoeg.frostedheart.content.climate.data.FoodTempData;
import com.teammoeg.frostedheart.content.climate.data.WorldTempData;
import com.teammoeg.frostedheart.content.climate.player.ITempAdjustFood;
import com.teammoeg.frostedheart.util.RegistryUtils;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fluids.FluidStack;

public class FHDataManager {
    public static class ResourceMap<T> extends HashMap<ResourceLocation, T> {
        /**
         *
         */
        private static final long serialVersionUID = 1564047056157250446L;

        public ResourceMap() {
            super();
        }

        public ResourceMap(int initialCapacity) {
            super(initialCapacity);
        }

        public ResourceMap(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        public ResourceMap(Map<? extends ResourceLocation, ? extends T> m) {
            super(m);
        }
    }
    public enum FHDataType {
        Armor(new DataType<>(ArmorTempData.class, "temperature", "armor", ArmorTempData.CODEC)),
        Biome(new DataType<>(BiomeTempData.class, "temperature", "biome", BiomeTempData.CODEC)),
        Food(new DataType<>(FoodTempData.class, "temperature", "food", FoodTempData.CODEC)),
        Block(new DataType<>(BlockTempData.class, "temperature", "block", BlockTempData.CODEC)),
        Drink(new DataType<>(DrinkTempData.class, "temperature", "drink", DrinkTempData.CODEC)),
        Cup(new DataType<>(CupData.class, "temperature", "cup", CupData.CODEC)),
        World(new DataType<>(WorldTempData.class, "temperature", "world", WorldTempData.CODEC));

        public static class DataType<T> {
            final Class<T> dataCls;
            final String location;
            final String domain;
            final Codec<DataReference<T>> codec;

            public DataType(Class<T> dataCls, String domain, String location, MapCodec<T> codec) {
                this.location = location;
                this.dataCls = dataCls;
                this.domain = domain;
                this.codec=DataReference.createCodec(codec).codec();
            }

            public DataReference<T> create(JsonElement jo) {
                return codec.decode(JsonOps.INSTANCE, jo).result().map(t->t.getFirst()).orElse(null);
            }
            public void write(DataReference<T> obj,PacketBuffer pb) {
            	pb.writeCompoundTag(codec.encodeStart(NBTDynamicOps.INSTANCE, obj).result().map(t->((CompoundNBT)t)).orElseGet(CompoundNBT::new));
            };
            public DataReference<T> read(PacketBuffer pb) {
            	return codec.decode(NBTDynamicOps.INSTANCE,pb.readCompoundTag()).result().map(t->t.getFirst()).orElse(null);
            };
            public String getLocation() {
                return domain + "/" + location;
            }
        }

        public final DataType<?> type;

        FHDataType(DataType<?> type) {
            this.type = type;
        }

    }
    public static void main(String[] args) {
    	System.out.println(ArmorTempData.CODEC.decode(JsonOps.INSTANCE, JsonOps.INSTANCE.getMap(new JsonParser().parse("{\"factor\":12}")).result().get()).result().orElse(null));
    	System.out.println(FHDataType.Armor.type.codec.encodeStart(NBTDynamicOps.INSTANCE,(DataReference)FHDataType.Armor.type.create(new JsonParser().parse("{\"id\":\"abc:def\",\"factor\":12}"))).result().map(Object::toString).orElse(""));
    }

    public static final EnumMap<FHDataType, ResourceMap<?>> ALL_DATA = new EnumMap<>(FHDataType.class);

    public static boolean synched = false;
    static {
        for (FHDataType dt : FHDataType.values()) {
            ALL_DATA.put(dt, new ResourceMap<>());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> ResourceMap<T> get(FHDataType dt) {
        return (ResourceMap<T>)ALL_DATA.get(dt);

    }

    public static ArmorTempData getArmor(ItemStack is) {
        //System.out.println(is.getItem().getRegistryName());
        return FHDataManager.<ArmorTempData>get(FHDataType.Armor).get(RegistryUtils.getRegistryName(is.getItem()));
    }

    public static ArmorTempData getArmor(String is) {
        //System.out.println(is.getItem().getRegistryName());
        return FHDataManager.<ArmorTempData>get(FHDataType.Armor).get(new ResourceLocation(is));
    }

    public static Float getBiomeTemp(Biome b) {
        if (b == null) return 0f;
        BiomeTempData data = FHDataManager.<BiomeTempData>get(FHDataType.Biome).get(RegistryUtils.getRegistryName(b));
        if (data != null)
            return data.getTemp();
        return 0F;
    }

    public static BlockTempData getBlockData(Block b) {
        return FHDataManager.<BlockTempData>get(FHDataType.Block).get(RegistryUtils.getRegistryName(b));
    }

    public static BlockTempData getBlockData(ItemStack b) {
        return FHDataManager.<BlockTempData>get(FHDataType.Block).get(RegistryUtils.getRegistryName(b.getItem()));
    }

    public static float getDrinkHeat(FluidStack f) {
        DrinkTempData dtd = FHDataManager.<DrinkTempData>get(FHDataType.Drink).get(RegistryUtils.getRegistryName(f.getFluid()));
        if (dtd != null)
            return dtd.getHeat();
        return -0.3f;
    }

    public static ITempAdjustFood getFood(ItemStack is) {
        CupData data = FHDataManager.<CupData>get(FHDataType.Cup).get(RegistryUtils.getRegistryName(is.getItem()));
        ResourceMap<FoodTempData> foodData = FHDataManager.get(FHDataType.Food);
        if (data != null) {
            return new CupTempAdjustProxy(data.getEfficiency(), foodData.get(RegistryUtils.getRegistryName(is.getItem())));
        }
        return foodData.get(RegistryUtils.getRegistryName(is.getItem()));
    }

    public static Float getWorldTemp(World w) {
        WorldTempData data = FHDataManager.<WorldTempData>get(FHDataType.World).get(w.getDimensionKey().getLocation());
        if (data != null)
            return data.getTemp();
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void load(FHDataType type, List<DataReference> entries) {
    	ResourceMap<Object> map=((ResourceMap<Object>)ALL_DATA.get(type));
    	map.clear();
        for (DataReference de : entries) {
            //System.out.println("registering "+dt.type.location+": "+jdh.getId());
        	map.put(de.getId(), de.getObj());
        }
    }

    @SuppressWarnings("unchecked")
    public static void register(FHDataType dt, JsonObject data) {
    	DataReference<?> jdh = dt.type.create(data);
        //System.out.println("registering "+dt.type.location+": "+jdh.getId());
    	((ResourceMap<Object>)ALL_DATA.get(dt)).put(jdh.getId(), jdh.getObj());
        synched = false;
    }

    public static void reset() {
        synched = false;
        for (ResourceMap<?> rm : ALL_DATA.values())
            rm.clear();
    }

    @SuppressWarnings("rawtypes")
    public static List<DataReference> save(FHDataType type) {
    	List<DataReference> entries = new ArrayList<>();
        for (Entry<ResourceLocation, ?> jdh : ALL_DATA.get(type).entrySet()) {
            entries.add(new DataReference(jdh.getKey(), jdh.getValue()));
        }
        return entries;
    }

    private FHDataManager() {
    }
}
