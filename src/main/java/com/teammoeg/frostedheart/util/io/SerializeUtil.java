/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.util.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.RegistryUtils;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ByteArrayNBT;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.FloatNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntArrayNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.LongArrayNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.nbt.ShortNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class SerializeUtil {
    public static class CompoundBuilder {
        CompoundNBT nbt = new CompoundNBT();

        public static CompoundBuilder create() {
            return new CompoundBuilder();
        }

        public CompoundNBT build() {
            return nbt;
        }

        public CompoundBuilder put(String key, INBT val) {
            nbt.put(key, val);
            return this;
        }

        public CompoundBuilder put(String key, int val) {
            nbt.putInt(key, val);
            return this;
        }

        public CompoundBuilder put(String key, UUID val) {
            nbt.putUniqueId(key, val);
            return this;
        }
    }

    public static class Deserializer<T extends JsonElement, U extends Writeable> {
        private int id;
        public Function<T, U> fromJson;
        public Function<PacketBuffer, U> fromPacket;

        public Deserializer(Function<T, U> fromJson, Function<PacketBuffer, U> fromPacket) {
            super();
            this.fromJson = fromJson;
            this.fromPacket = fromPacket;
        }

        public U read(PacketBuffer packet) {
            return fromPacket.apply(packet);
        }

        public U read(T json) {
            return fromJson.apply(json);
        }

        public JsonElement serialize(U obj) {
            return obj.serialize();
        }

        public void write(PacketBuffer packet, U obj) {
            packet.writeVarInt(id);
            obj.write(packet);
        }
    }


    public static ItemStack fromJson(JsonElement elm) {
        if (elm.isJsonPrimitive())
            return new ItemStack(RegistryUtils.getItem(new ResourceLocation(elm.getAsString())));
        else if (elm.isJsonObject()) {
            JsonObject jo = elm.getAsJsonObject();
            ItemStack ret = new ItemStack(RegistryUtils.getItem(new ResourceLocation(jo.get("id").getAsString())));
            if (jo.has("count"))
                ret.setCount(jo.get("count").getAsInt());
            if (jo.has("nbt"))
                try {
                    ret.setTag(JsonToNBT.getTagFromJson(jo.get("nbt").getAsString()));
                } catch (CommandSyntaxException e) {
                    FHMain.LOGGER.warn(e.getMessage());
                }
            return ret;
        }
        return ItemStack.EMPTY;
    }
	public static <K,V> Codec<Pair<K,V>> pairCodec(String nkey,Codec<K> key,String nval,Codec<V> val){
		return RecordCodecBuilder.create(t->t.group(key.fieldOf(nkey).forGetter(Pair::getFirst), val.fieldOf(nval).forGetter(Pair::getSecond))
			.apply(t,Pair::of));
	} 
	public static <K,V> Codec<Map<K,V>> mapCodec(Codec<K> keyCodec,Codec<V> valueCodec){
		return Codec.compoundList(keyCodec, valueCodec).xmap(pl->pl.stream().collect(Collectors.toMap(Pair::getFirst,Pair::getSecond)),pl->pl.entrySet().stream().map(ent->Pair.of(ent.getKey(), ent.getValue())).toList()); 
	}

    public static <T> List<T> parseJsonElmList(JsonElement elm, Function<JsonElement, T> mapper) {
        if (elm == null)
            return Lists.newArrayList();
        if (elm.isJsonArray())
            return StreamSupport.stream(elm.getAsJsonArray().spliterator(), false).map(mapper)
                    .collect(Collectors.toList());
        return Lists.newArrayList(mapper.apply(elm));
    }

    public static <T> List<T> parseJsonList(JsonElement elm, Function<JsonObject, T> mapper) {
        if (elm == null)
            return Lists.newArrayList();
        if (elm.isJsonArray())
            return StreamSupport.stream(elm.getAsJsonArray().spliterator(), false).map(JsonElement::getAsJsonObject)
                    .map(mapper).collect(Collectors.toList());
        return Lists.newArrayList(mapper.apply(elm.getAsJsonObject()));
    }

    public static boolean[] readBooleans(PacketBuffer buffer) {
        boolean[] ret = new boolean[8];
        byte in = buffer.readByte();
        for (int i = ret.length - 1; i >= 0; i--) {
            ret[i] = (in & 1) != 0;
            in >>= 1;
        }
        return ret;
    }

    public static <T> List<T> readList(PacketBuffer buffer, Function<PacketBuffer, T> func) {
        if (!buffer.readBoolean())
            return null;
        int cnt = buffer.readVarInt();
        List<T> nums = new ArrayList<>(cnt);
        for (int i = 0; i < cnt; i++)
            nums.add(func.apply(buffer));
        return nums;
    }

    public static <K, V> Map<K, V> readMap(PacketBuffer buffer, Map<K, V> map, Function<PacketBuffer, K> keyreader, Function<PacketBuffer, V> valuereader) {
        map.clear();
        if (!buffer.readBoolean())
            return map;
        int cnt = buffer.readVarInt();
        for (int i = 0; i < cnt; i++)
            map.put(keyreader.apply(buffer), valuereader.apply(buffer));
        return map;
    }

    public static <T> Optional<T> readOptional(PacketBuffer buffer, Function<PacketBuffer, T> func) {
        if (buffer.readBoolean())
            return Optional.ofNullable(func.apply(buffer));
        return Optional.empty();
    }

    public static short[] readShortArray(PacketBuffer buffer) {
        if (!buffer.readBoolean())
            return null;
        int cnt = buffer.readVarInt();
        short[] nums = new short[cnt];
        for (int i = 0; i < cnt; i++)
            nums[i] = buffer.readShort();
        return nums;
    }

    public static <V> Map<String, V> readStringMap(PacketBuffer buffer, Map<String, V> map, Function<PacketBuffer, V> valuereader) {
        return readMap(buffer, map, PacketBuffer::readString, valuereader);
    }

    public static JsonElement toJson(ItemStack stack) {
        boolean hasCount = stack.getCount() > 1, hasTag = stack.hasTag();
        if (!hasCount && !hasTag)
            return new JsonPrimitive(RegistryUtils.getRegistryName(stack.getItem()).toString());
        JsonObject jo = new JsonObject();
        jo.addProperty("id", RegistryUtils.getRegistryName(stack.getItem()).toString());
        if (hasCount)
            jo.addProperty("count", stack.getCount());
        if (hasTag)
            jo.addProperty("nbt", stack.getTag().toString());
        return jo;
    }

    public static <T> JsonArray toJsonList(Collection<T> li, Function<T, JsonElement> mapper) {
        JsonArray ja = new JsonArray();
        li.stream().map(mapper).forEach(ja::add);
        return ja;
    }

    public static <T, B> JsonArray toJsonStringList(Collection<T> li, Function<T, B> mapper) {
        JsonArray ja = new JsonArray();
        li.stream().map(mapper).map(B::toString).forEach(ja::add);
        return ja;
    }

    public static <T> ListNBT toNBTList(Collection<T> stacks, Function<T, INBT> mapper) {
        ListNBT nbt = new ListNBT();
        stacks.stream().map(mapper).forEach(nbt::add);
        return nbt;
    }

    /**
     * Write boolean as a byte into buffer
     *
     * @param elms elements to write, 8 elements max
     */
    public static void writeBooleans(PacketBuffer buffer, boolean... elms) {
        if (elms.length > 8) {
            throw new IllegalArgumentException("count of boolean must not excess 8");
        }
        byte b = 0;
        for (int i = 0; i < 8; i++) {
            boolean bl = elms.length > i && elms[i];
            b <<= 1;
            b |= (byte) (bl ? 1 : 0);

        }
        buffer.writeByte(b);
    }

    public static <T> void writeList(PacketBuffer buffer, Collection<T> elms, BiConsumer<T, PacketBuffer> func) {
        if (elms == null) {
            buffer.writeBoolean(false);
            return;
        }
        buffer.writeBoolean(true);
        buffer.writeVarInt(elms.size());
        elms.forEach(e -> func.accept(e, buffer));
    }

    public static <T> void writeList2(PacketBuffer buffer, Collection<T> elms, BiConsumer<PacketBuffer, T> func) {
        if (elms == null) {
            buffer.writeBoolean(false);
            return;
        }
        buffer.writeBoolean(true);
        buffer.writeVarInt(elms.size());
        elms.forEach(e -> func.accept(buffer, e));
    }

    public static <K, V> void writeMap(PacketBuffer buffer, Map<K, V> elms, BiConsumer<K, PacketBuffer> keywriter, BiConsumer<V, PacketBuffer> valuewriter) {
        writeList(buffer, elms.entrySet(), (p, b) -> {
            keywriter.accept(p.getKey(), b);
            valuewriter.accept(p.getValue(), b);
        });
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> void writeOptional(PacketBuffer buffer, Optional<T> data, BiConsumer<T, PacketBuffer> func) {
        if (data.isPresent()) {
            buffer.writeBoolean(true);
            func.accept(data.get(), buffer);
            return;
        }
        buffer.writeBoolean(false);
    }


    public static <T> void writeOptional(PacketBuffer buffer, T data, BiConsumer<T, PacketBuffer> func) {
        writeOptional(buffer, Optional.ofNullable(data), func);
    }

    public static <T> void writeOptional2(PacketBuffer buffer, T data, BiConsumer<PacketBuffer, T> func) {
        writeOptional(buffer, data, (a, b) -> func.accept(b, a));
    }

    public static void writeShortArray(PacketBuffer buffer, short[] arr) {
        if (arr == null) {
            buffer.writeBoolean(false);
            return;
        }
        buffer.writeBoolean(true);
        buffer.writeVarInt(arr.length);
        for (short s : arr)
            buffer.writeShort(s);
    }

    public static <V> void writeStringMap(PacketBuffer buffer, Map<String, V> elms, BiConsumer<V, PacketBuffer> valuewriter) {
        writeMap(buffer, elms, (p, b) -> b.writeString(p), valuewriter);
    }
    private static final Map<Class<?>,Marshaller> marshallers=new HashMap<>();
    private static boolean isBasicInitialized=false;
    public static void initializeMarshallers() {
    	if(!isBasicInitialized) {
	    	marshallers.put(byte.class, new BasicMarshaller<>(ByteNBT.class,ByteNBT::getByte,ByteNBT::valueOf,(byte)0));
	    	marshallers.put(Byte.class, new BasicMarshaller<>(ByteNBT.class,ByteNBT::getByte,ByteNBT::valueOf));
	    	
	    	marshallers.put(double.class, new BasicMarshaller<>(DoubleNBT.class,DoubleNBT::getDouble,DoubleNBT::valueOf,0d));
	    	marshallers.put(Double.class, new BasicMarshaller<>(DoubleNBT.class,DoubleNBT::getDouble,DoubleNBT::valueOf));
	    	
	    	marshallers.put(float.class, new BasicMarshaller<>(FloatNBT.class,FloatNBT::getFloat,FloatNBT::valueOf,0f));
	    	marshallers.put(Float.class, new BasicMarshaller<>(FloatNBT.class,FloatNBT::getFloat,FloatNBT::valueOf));
	    	
	    	marshallers.put(int.class, new BasicMarshaller<>(IntNBT.class,IntNBT::getInt,IntNBT::valueOf,0));
	    	marshallers.put(Integer.class, new BasicMarshaller<>(IntNBT.class,IntNBT::getInt,IntNBT::valueOf));
	    	
	    	marshallers.put(long.class, new BasicMarshaller<>(LongNBT.class,LongNBT::getLong,LongNBT::valueOf, 0L));
	    	marshallers.put(Long.class, new BasicMarshaller<>(LongNBT.class,LongNBT::getLong,LongNBT::valueOf));
	    	
	       	marshallers.put(short.class, new BasicMarshaller<>(ShortNBT.class,ShortNBT::getShort,ShortNBT::valueOf,(short)0));
	    	marshallers.put(Short.class, new BasicMarshaller<>(ShortNBT.class,ShortNBT::getShort,ShortNBT::valueOf));
	    	
	       	marshallers.put(String.class, new BasicMarshaller<>(StringNBT.class,StringNBT::getString,StringNBT::valueOf));
	    	
	    	marshallers.put(byte[].class, new BasicMarshaller<>(ByteArrayNBT.class,ByteArrayNBT::getByteArray,ByteArrayNBT::new));
	    	marshallers.put(int[].class, new BasicMarshaller<>(IntArrayNBT.class,IntArrayNBT::getIntArray,IntArrayNBT::new));
	    	marshallers.put(long[].class, new BasicMarshaller<>(LongArrayNBT.class,LongArrayNBT::getAsLongArray,LongArrayNBT::new));
	    	isBasicInitialized=true;
    	}
    }
    public static <T> Marshaller create(Class<T> type) {
    	if(List.class.isAssignableFrom(type)) {
    		return new ListMarshaller<>(Object.class,t-> new ListListWrapper<>((List<Object>) t),ListListWrapper::new);
    	}else if(type.isArray()) {
    		return new ListMarshaller<>(type, ArrayListWrapper::new,ArrayListWrapper::new);
    	}
    	return ClassInfo.valueOf(type);
    }
    public static Marshaller getOrCreate(Class<?> type) {
    	return marshallers.computeIfAbsent(type,SerializeUtil::create);
    }
    public static Object deserialize(Class<?> type,INBT nbt) {
    	Marshaller msl=getOrCreate(type);
    	if(msl!=null)
    		return msl.fromNBT(nbt);
    	return null;
    }
    public static INBT serialize(Object data) {
    	if(data==null)return null;
    	Marshaller msl=getOrCreate(data.getClass());
    	if(msl!=null)
    		return msl.toNBT(data);
    	return null;
    	
    }

    private SerializeUtil() {

    }
}
