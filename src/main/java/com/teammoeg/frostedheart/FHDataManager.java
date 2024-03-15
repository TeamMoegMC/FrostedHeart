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
import com.teammoeg.frostedheart.util.io.SerializeUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
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
    public static class DataType<T> {
        public static final List<DataType<?>> types = new ArrayList<>();
        final int id;
        final Class<T> dataCls;
        final String location;
        final String domain;
        final Codec<DataReference<T>> codec;

        public DataType(Class<T> dataCls, String domain, String location, MapCodec<T> codec) {
            this.location = location;
            this.dataCls = dataCls;
            this.domain = domain;
            this.codec=DataReference.createCodec(codec).codec();
            this.id=types.size();
            types.add(this);
        }

        public DataReference<T> create(JsonElement jo) {
            return codec.decode(JsonOps.INSTANCE, jo).result().map(t->t.getFirst()).orElse(null);
        }
        public void write(DataReference<T> obj,PacketBuffer pb) {
        	
        	SerializeUtil.writeCodec(pb, codec, obj);
        };
        public DataReference<T> read(PacketBuffer pb) {
        	return SerializeUtil.readCodec(pb, codec);
        };
        public String getLocation() {
            return domain + "/" + location;
        }

		public int getId() {
			return id;
		}
    }

    public static final DataType<ArmorTempData> Armor = (new DataType<>(ArmorTempData.class, "temperature", "armor", ArmorTempData.CODEC));
    public static final DataType<BiomeTempData> Biome = (new DataType<>(BiomeTempData.class, "temperature", "biome", BiomeTempData.CODEC));
    public static final DataType<FoodTempData>  Food  = (new DataType<>(FoodTempData.class , "temperature", "food" , FoodTempData.CODEC ));
    public static final DataType<BlockTempData> Block = (new DataType<>(BlockTempData.class, "temperature", "block", BlockTempData.CODEC));
    public static final DataType<DrinkTempData> Drink = (new DataType<>(DrinkTempData.class, "temperature", "drink", DrinkTempData.CODEC));
    public static final DataType<CupData>       Cup   = (new DataType<>(CupData.class      , "temperature", "cup"  , CupData.CODEC      ));
    public static final DataType<WorldTempData> World = (new DataType<>(WorldTempData.class, "temperature", "world", WorldTempData.CODEC));
    public static void main(String[] args) {
    	System.out.println(ArmorTempData.CODEC.decode(JsonOps.INSTANCE, JsonOps.INSTANCE.getMap(new JsonParser().parse("{\"factor\":12}")).result().get()).result().orElse(null));
    	//Object nbt=FHDataType.Armor.type.codec.encodeStart(DataOps.COMPRESSED,(DataReference<Object>)FHDataType.Armor.type.create(new JsonParser().parse("{\"id\":\"abc:def\",\"factor\":12}"))).result().orElse(null);
    	ByteBuf bb=ByteBufAllocator.DEFAULT.buffer(256);
    	PacketBuffer pb=new PacketBuffer(bb);
    	Armor.write(Armor.create(new JsonParser().parse("{\"id\":\"abc:def\",\"factor\":12}")), pb);
    	System.out.println(bb.writerIndex());
    	bb.resetReaderIndex();
    	for(int i=0;i<bb.writerIndex();i++)
    		System.out.print(String.format("%2x", bb.readByte())+" ");
    	bb.resetReaderIndex();
    	System.out.println();
    	for(int i=0;i<bb.writerIndex();i++) {
    		byte data=bb.readByte();
    		if(data!='\r'&&data!='\n')
    			System.out.print(String.format(" %c", data)+" ");
    		else
    			System.out.print("   ");
    	}
    	System.out.println();
    	bb.resetReaderIndex();
    	System.out.println(Armor.read(pb));
    }

    public static final Map<DataType<?>, ResourceMap<Object>> ALL_DATA = new HashMap<>();

    public static boolean synched = false;
    static {
        for (DataType<?> dt : DataType.types) {
            ALL_DATA.put(dt, new ResourceMap<>());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> ResourceMap<T> get(DataType<T> dt) {
        return (ResourceMap<T>)ALL_DATA.get(dt);

    }

    public static ArmorTempData getArmor(ItemStack is) {
        //System.out.println(is.getItem().getRegistryName());
        return FHDataManager.<ArmorTempData>get(Armor).get(RegistryUtils.getRegistryName(is.getItem()));
    }

    public static ArmorTempData getArmor(String is) {
        //System.out.println(is.getItem().getRegistryName());
        return FHDataManager.<ArmorTempData>get(Armor).get(new ResourceLocation(is));
    }

    public static Float getBiomeTemp(Biome b) {
        if (b == null) return 0f;
        BiomeTempData data = FHDataManager.get(Biome).get(RegistryUtils.getRegistryName(b));
        if (data != null)
            return data.getTemp();
        return 0F;
    }

    public static BlockTempData getBlockData(Block b) {
        return FHDataManager.get(Block).get(RegistryUtils.getRegistryName(b));
    }

    public static BlockTempData getBlockData(ItemStack b) {
        return FHDataManager.get(Block).get(RegistryUtils.getRegistryName(b.getItem()));
    }

    public static float getDrinkHeat(FluidStack f) {
        DrinkTempData dtd = FHDataManager.get(Drink).get(RegistryUtils.getRegistryName(f.getFluid()));
        if (dtd != null)
            return dtd.getHeat();
        return -0.3f;
    }

    public static ITempAdjustFood getFood(ItemStack is) {
        CupData data = FHDataManager.get(Cup).get(RegistryUtils.getRegistryName(is.getItem()));
        ResourceMap<FoodTempData> foodData = FHDataManager.get(Food);
        if (data != null) {
            return new CupTempAdjustProxy(data.getEfficiency(), foodData.get(RegistryUtils.getRegistryName(is.getItem())));
        }
        return foodData.get(RegistryUtils.getRegistryName(is.getItem()));
    }

    public static Float getWorldTemp(World w) {
        WorldTempData data = FHDataManager.get(World).get(w.getDimensionKey().getLocation());
        if (data != null)
            return data.getTemp();
        return null;
    }

    public static <T> void load(DataType<T> type, List<DataReference<?>> entries) {
    	ResourceMap<Object> map=(ALL_DATA.get(type));
    	map.clear();
        for (DataReference<?> de : entries) {
        	map.put(de.getId(), de.getObj());
        }
    }

    public static <T> void register(DataType<T> dt, JsonObject data) {
    	DataReference<T> jdh = dt.create(data);
        //System.out.println("registering "+dt.type.location+": "+jdh.getId());
    	((ResourceMap<Object>)ALL_DATA.get(dt)).put(jdh.getId(), jdh.getObj());
        synched = false;
    }

    public static void reset() {
        synched = false;
        for (ResourceMap<?> rm : ALL_DATA.values())
            rm.clear();
    }

    public static <T> List<DataReference<?>> save(DataType<T> type) {
    	List<DataReference<?>> entries = new ArrayList<>();
        for (Entry<ResourceLocation, ?> jdh : ALL_DATA.get(type).entrySet()) {
            entries.add(new DataReference<>(jdh.getKey(), jdh.getValue()));
        }
        return entries;
    }

    private FHDataManager() {
    }
}
