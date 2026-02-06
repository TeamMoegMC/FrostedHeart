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

package com.teammoeg.chorda.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.io.nbtbuilder.ArrayNBTBuilder;
import com.teammoeg.chorda.io.nbtbuilder.CompoundNBTBuilder;
import com.teammoeg.chorda.util.CRegistryHelper;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * utils wrapped function for serialization and networking.
 */
@ParametersAreNonnullByDefault
public class SerializeUtil {
	
	/**
	 * The Class Deserializer.
	 *
	 * @param <T> the generic type
	 * @param <U> the generic type
	 */
	public static class Deserializer<T extends JsonElement, U extends Writeable> {
		
		/** The id. */
		private int id;
		
		/** The from json. */
		public Function<T, U> fromJson;
		
		/** The from packet. */
		public Function<FriendlyByteBuf, U> fromPacket;

		/**
		 * Instantiates a new deserializer.
		 *
		 * @param fromJson the from json
		 * @param fromPacket the from packet
		 */
		public Deserializer(Function<T, U> fromJson, Function<FriendlyByteBuf, U> fromPacket) {
			super();
			this.fromJson = fromJson;
			this.fromPacket = fromPacket;
		}

		/**
		 * Read.
		 *
		 * @param packet the packet
		 * @return the u
		 */
		public U read(FriendlyByteBuf packet) {
			return fromPacket.apply(packet);
		}

		/**
		 * Read.
		 *
		 * @param json the json
		 * @return the u
		 */
		public U read(T json) {
			return fromJson.apply(json);
		}

		/**
		 * Serialize.
		 *
		 * @param obj the obj
		 * @return the json element
		 */
		public JsonElement serialize(U obj) {
			return obj.serialize();
		}

		/**
		 * Write.
		 *
		 * @param packet the packet
		 * @param obj the obj
		 */
		public void write(FriendlyByteBuf packet, U obj) {
			packet.writeVarInt(id);
			obj.write(packet);
		}
	}
	
	/**
	 * Read ItemStack from json
	 * @see #toJson
	 * @param elm the elm
	 * @return the item stack
	 */
	@Nonnull
	public static ItemStack fromJson(JsonElement elm) {
		if (elm.isJsonPrimitive())
			return new ItemStack(CRegistryHelper.getItem(new ResourceLocation(elm.getAsString())));
		else if (elm.isJsonObject()) {
			JsonObject jo = elm.getAsJsonObject();
			ItemStack ret = new ItemStack(CRegistryHelper.getItem(new ResourceLocation(jo.get("id").getAsString())));
			if (jo.has("count"))
				ret.setCount(jo.get("count").getAsInt());
			if (jo.has("nbt"))
				try {
					ret.setTag(TagParser.parseTag(jo.get("nbt").getAsString()));
				} catch (CommandSyntaxException e) {
					Chorda.LOGGER.warn(e.getMessage());
				}
			return ret;
		}
		return ItemStack.EMPTY;
	}

	/**
	 * Cached generator
	 * @deprecated use {@link net.minecraft.Util#memoize(java.util.function.Function) Util.memorize} for better multithreading
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param func the generator function
	 * @return the cached generator function
	 */
	@Deprecated
	public static <K, V> Function<K, V> cached(Function<K, V> func) {
		Map<K, V> map = new HashMap<>();
		return k -> map.computeIfAbsent(k, func);
	}

	/**
	 * Parses the json element list, this would check if elm is array/null.<p>
	 * case array, mapper would be called to serialize each element.<br>
	 * case null, an empty list would be returned<br>
	 * case other, mapper would be called to serialize to element itself
	 * 
	 *
	 * @param <T> the return object
	 * @param elm the json element read from file
	 * @param mapper the mapper converts json to object
	 * @return the object list
	 */
	@Nonnull
	public static <T> List<T> parseJsonElmList(@Nullable JsonElement elm, Function<JsonElement, T> mapper) {
		if (elm == null)
			return Lists.newArrayList();
		if (elm.isJsonArray())
			return StreamSupport.stream(elm.getAsJsonArray().spliterator(), false).map(mapper)
				.collect(Collectors.toList());
		return Lists.newArrayList(mapper.apply(elm));
	}

	/**
	 * Parses the json object list, this would check if elm is array/null.<p>
	 * case array, mapper would be called to deserialize each element.<br>
	 * case null, an empty list would be returned<br>
	 * case object, mapper would be called to deserialize element itself
	 * 
	 * @throws IllegalStateException when element of array or the element itself is not json object
	 * 
	 * @param <T> the generic type
	 * @param elm the elm
	 * @param mapper the mapper
	 * @return the list
	 */
	@Nonnull
	public static <T> List<T> parseJsonList(@Nullable JsonElement elm, Function<JsonObject, T> mapper) {
		if (elm == null)
			return Lists.newArrayList();
		if (elm.isJsonArray())
			return StreamSupport.stream(elm.getAsJsonArray().spliterator(), false).map(JsonElement::getAsJsonObject)
				.map(mapper).collect(Collectors.toList());
		return Lists.newArrayList(mapper.apply(elm.getAsJsonObject()));
	}

	/**
	 * Read 8 boolean flags from byte bits
	 * @see #writeBooleans(FriendlyByteBuf, boolean...)
	 * @param buffer the network buffer
	 * @return the boolean flags
	 */
	public static boolean[] readBooleans(FriendlyByteBuf buffer) {
		return readBooleans(buffer.readByte());
	}

	/**
	 * Read boolean flags from byte bits.
	 * @see #writeBooleans(boolean...)
	 * @param in the input byte
	 * @return the boolean flags
	 */
	public static boolean[] readBooleans(byte in) {
		boolean[] ret = new boolean[8];
		for (int i = ret.length - 1; i >= 0; i--) {
			ret[i] = (in & 1) != 0;
			in >>= 1;
		}
		return ret;
	}
	
	/**
	 * Read booleans with a maximum count of bits, in BE order
	 * @see #writeLongBooleans
	 * @param in the input long
	 * @param size the flag count
	 * @return the flags
	 */
	public static boolean[] readLongBooleans(long in,int size) {
		boolean[] ret = new boolean[size];
		for (int i = ret.length - 1; i >= 0; i--) {
			ret[i] = (in & 1) != 0;
			in >>= 1;
		}
		return ret;
	}
	
	/**
	 * Write booleans to a long in BE order
	 * @see #readLongBooleans
	 * @param elms the flags
	 * @return the long
	 */
	public static long writeLongBooleans(boolean... elms) {
		long b = 0;
		for (int i = 0; i < elms.length; i++) {
			boolean bl = elms[i];
			b <<= 1;
			b |= (long) (bl ? 1 : 0);

		}
		return b;
	}

	/**
	 * Read list from buffer, may return null if the list written is null
	 * the function would be called to read each element to read from packet to required object.
	 * @see #readList(FriendlyByteBuf, Function)
	 * @see #writeListNullable(FriendlyByteBuf, Collection, BiConsumer)
	 * @see #writeListNullable2(FriendlyByteBuf, Collection, BiConsumer)
	 * @param <T> the list element type
	 * @param buffer the buffer
	 * @param func the read method for the required object
	 * @return the list
	 */
	@Nullable
	public static <T> List<T> readListNullable(FriendlyByteBuf buffer, Function<FriendlyByteBuf, T> func) {
		if (!buffer.readBoolean())
			return null;
		return readList(buffer, func);
	}

	/**
	 * Read a list from buffer
	 * the function would be called to read each element to read from packet to required object.
	 * @see #writeList(FriendlyByteBuf, Collection, BiConsumer)
	 * @see #writeList2(FriendlyByteBuf, Collection, BiConsumer)
	 * @param <T> the list element type
	 * @param buffer the buffer
	 * @param func the read method for the required object
	 * @return the list
	 */
	public static <T> List<T> readList(FriendlyByteBuf buffer, Function<FriendlyByteBuf, T> func) {
		int cnt = buffer.readVarInt();
		List<T> nums = new ArrayList<>(cnt);
		for (int i = 0; i < cnt; i++)
			nums.add(func.apply(buffer));
		return nums;
	}

	/**
	 * Read values and set the provided map to the read key values with separate key and value reader.
	 * The key reader and value reader would be called to read each entry
	 * @see #writeMap(FriendlyByteBuf, Map, BiConsumer, BiConsumer)
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param buffer the buffer
	 * @param map the map to write entrys within
	 * @param keyreader the key reader
	 * @param valuereader the value reader
	 * @return the given map
	 */
	public static <K, V> Map<K, V> readMap(FriendlyByteBuf buffer, Map<K, V> map, Function<FriendlyByteBuf, K> keyreader, Function<FriendlyByteBuf, V> valuereader) {
		map.clear();
		if (!buffer.readBoolean())
			return map;
		int cnt = buffer.readVarInt();
		for (int i = 0; i < cnt; i++)
			map.put(keyreader.apply(buffer), valuereader.apply(buffer));
		return map;
	}

	/**
	 * Read values and set the provided map to the read key values with single entry reader.
	 * The entry reader would be called to read each entry
	 * @see #writeEntry(FriendlyByteBuf, Map, BiConsumer)
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param buffer the buffer
	 * @param map the map to write entrys within
	 * @param reader the entry reader
	 * @return the given map
	 */
	public static <K, V> Map<K, V> readEntry(FriendlyByteBuf buffer, Map<K, V> map, BiConsumer<FriendlyByteBuf, BiConsumer<K, V>> reader) {
		map.clear();
		int cnt = buffer.readVarInt();
		for (int i = 0; i < cnt; i++)
			reader.accept(buffer, map::put);
		return map;
	}

	/**
	 * Read optional from packet
	 * @see #writeOptional(FriendlyByteBuf, Optional, BiConsumer)
	 * @see #writeOptional(FriendlyByteBuf, Object, BiConsumer)
	 * @see #writeOptional2(FriendlyByteBuf, Object, BiConsumer)
	 * @param <T> the optional type
	 * @param buffer the buffer
	 * @param func the read function if object present
	 * @return the optional value
	 */
	public static <T> Optional<T> readOptional(FriendlyByteBuf buffer, Function<FriendlyByteBuf, T> func) {
		if (buffer.readBoolean())
			return Optional.ofNullable(func.apply(buffer));
		return Optional.empty();
	}

	/**
	 * Read short array from packet
	 * @see #writeShortArray(FriendlyByteBuf, short[])
	 * @param buffer the buffer
	 * @return the short array
	 */
	public static short[] readShortArray(FriendlyByteBuf buffer) {
		if (!buffer.readBoolean())
			return null;
		int cnt = buffer.readVarInt();
		short[] nums = new short[cnt];
		for (int i = 0; i < cnt; i++)
			nums[i] = buffer.readShort();
		return nums;
	}

	/**
	 * Read string-value map and set the provided map to the read key values with value reader.
	 * The value reader would be called to read each entry value
	 * @see #readStringMap(FriendlyByteBuf, Map, Function)
	 * @param <V> the value type
	 * @param buffer the buffer
	 * @param map the map to write entrys within
	 * @param valuereader the value reader
	 * @return the given map
	 */
	public static <V> Map<String, V> readStringMap(FriendlyByteBuf buffer, Map<String, V> map, Function<FriendlyByteBuf, V> valuereader) {
		return readMap(buffer, map, FriendlyByteBuf::readUtf, valuereader);
	}

	/**
	 * Save itemstack to json
	 * @see #fromJson(JsonElement)
	 * @param stack the stack
	 * @return the json element
	 */
	public static JsonElement toJson(ItemStack stack) {
		boolean hasCount = stack.getCount() > 1, hasTag = stack.hasTag();
		if (!hasCount && !hasTag)
			return new JsonPrimitive(CRegistryHelper.getRegistryName(stack.getItem()).toString());
		JsonObject jo = new JsonObject();
		jo.addProperty("id", CRegistryHelper.getRegistryName(stack.getItem()).toString());
		if (hasCount)
			jo.addProperty("count", stack.getCount());
		if (hasTag)
			jo.addProperty("nbt", stack.getTag().toString());
		return jo;
	}

	/**
	 * convert collection to json list.
	 *
	 * @param <T> the collection element list
	 * @param li the collection
	 * @param mapper the function convert element to json
	 * @return the json array
	 */
	public static <T> JsonArray toJsonList(Collection<T> li, Function<T, JsonElement> mapper) {
		JsonArray ja = new JsonArray();
		li.stream().map(mapper).forEach(ja::add);
		return ja;
	}

	/**
	 * To json string list.
	 * convert object to a type with toString, and this would call toString to serialize items to json string.
	 * @see #toJsonList(Collection, Function)
	 *
	 * @param <T> the original object type
	 * @param <B> the toString type
	 * @param li the collection
	 * @param mapper the function convert element to toString object
	 * @return the json array
	 */
	public static <T, B> JsonArray toJsonStringList(Collection<T> li, Function<T, B> mapper) {
		JsonArray ja = new JsonArray();
		li.stream().map(mapper).map(B::toString).forEach(ja::add);
		return ja;
	}

	/**
	 * Serialize a collection to a NBT list
	 * 
	 * @param <T> the element type
	 * @param stacks the collection
	 * @param mapper the element serializer, the second argument is array nbt element builder
	 * @return the list tag
	 */
	public static <T> ListTag toNBTList(Collection<T> stacks, BiConsumer<T, ArrayNBTBuilder<Void>> mapper) {
		ArrayNBTBuilder<Void> arrayBuilder = ArrayNBTBuilder.create();
		stacks.stream().forEach(t -> mapper.accept(t, arrayBuilder));
		return arrayBuilder.build();
	}

	/**
	 * Serialize a collection to a NBT map.
	 *
	 * @param <T> the element type
	 * @param stacks the collection
	 * @param mapper the element serializer, the second argument is map nbt entry builder
	 * @return the compound tag
	 */
	public static <T> CompoundTag toNBTMap(Collection<T> stacks, BiConsumer<T, CompoundNBTBuilder<Void>> mapper) {
		CompoundNBTBuilder<Void> compoundBuilder = CompoundNBTBuilder.create();
		stacks.stream().forEach(t -> mapper.accept(t, compoundBuilder));
		return compoundBuilder.build();
	}

	/**
	 * Write boolean flags as a byte into buffer.
	 *
	 * @param buffer the buffer
	 * @param elms elements to write, 8 elements max
	 */
	public static void writeBooleans(FriendlyByteBuf buffer, boolean... elms) {
		buffer.writeByte(writeBooleans(elms));
	}

	/**
	 * Write boolean flags to a byte bit.
	 * @see #readBooleans(byte)
	 * @param elms the elms
	 * @return the byte
	 */
	public static byte writeBooleans(boolean... elms) {
		if (elms.length > 8) {
			throw new IllegalArgumentException("count of boolean must not excess 8");
		}
		byte b = 0;
		for (int i = 0; i < 8; i++) {
			boolean bl = elms.length > i && elms[i];
			b <<= 1;
			b |= (byte) (bl ? 1 : 0);

		}
		return b;
	}

	/**
	 * Write a nullable colleciton to the packet, each element is written by calling the function.
	 * @see #readListNullable(FriendlyByteBuf, Function)
	 * @see #writeListNullable2(FriendlyByteBuf, Collection, BiConsumer)
	 * @param <T> the element type
	 * @param buffer the buffer
	 * @param elms the colleciton
	 * @param func the write function
	 */
	public static <T> void writeListNullable(FriendlyByteBuf buffer,@Nullable Collection<T> elms, BiConsumer<T, FriendlyByteBuf> func) {
		if (elms == null) {
			buffer.writeBoolean(false);
			return;
		}
		buffer.writeBoolean(true);
		writeList(buffer, elms, func);
	}

	/**
	 * Write a nullable colleciton to the packet, each element is written by calling the function.
	 * This method is different from {@link #writeListNullable(FriendlyByteBuf, Collection, BiConsumer) writeListNullable} with reversed function parameter order, so that :: operator can be used
	 * @see #readListNullable(FriendlyByteBuf, Function)
	 * @see #writeListNullable(FriendlyByteBuf, Collection, BiConsumer)
	 * @param <T> the element type
	 * @param buffer the buffer
	 * @param elms the colleciton
	 * @param func the write function
	 */
	public static <T> void writeListNullable2(FriendlyByteBuf buffer, Collection<T> elms, BiConsumer<FriendlyByteBuf, T> func) {
		if (elms == null) {
			buffer.writeBoolean(false);
			return;
		}
		buffer.writeBoolean(true);
		writeList2(buffer, elms, func);
	}

	/**
	 * Write a nonnull colleciton to the packet, each element is written by calling the function.
	 * @see #readList(FriendlyByteBuf, Function)
	 * @see #writeList2(FriendlyByteBuf, Collection, BiConsumer)
	 * @param <T> the element type
	 * @param buffer the buffer
	 * @param elms the colleciton
	 * @param func the write function
	 */
	public static <T> void writeList(FriendlyByteBuf buffer, Collection<T> elms, BiConsumer<T, FriendlyByteBuf> func) {
		buffer.writeVarInt(elms.size());
		elms.forEach(e -> func.accept(e, buffer));
	}

	/**
	 * Write entries of the map to packet with given write function
	 * @see #readEntry(FriendlyByteBuf, Map, BiConsumer)
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param buffer the buffer
	 * @param elms the map
	 * @param func the write function
	 */
	public static <K, V> void writeEntry(FriendlyByteBuf buffer, Map<K, V> elms, BiConsumer<Map.Entry<K, V>, FriendlyByteBuf> func) {
		buffer.writeVarInt(elms.size());
		elms.entrySet().forEach(e -> func.accept(e, buffer));
	}

	/**
	 * Write a nonnull colleciton to the packet, each element is written by calling the function.
	 * This method is different from {@link #writeList(FriendlyByteBuf, Collection, BiConsumer) writeList} with reversed function parameter order, so that :: operator can be used
	 * @see #readList(FriendlyByteBuf, Function)
	 * @see #writeList(FriendlyByteBuf, Collection, BiConsumer)
	 * @param <T> the element type
	 * @param buffer the buffer
	 * @param elms the colleciton
	 * @param func the write function
	 */
	public static <T> void writeList2(FriendlyByteBuf buffer, Collection<T> elms, BiConsumer<FriendlyByteBuf, T> func) {
		buffer.writeVarInt(elms.size());
		elms.forEach(e -> func.accept(buffer, e));
	}

	/**
	 * Write map to packet with the given key and value writer for each entry.
	 * @see #readMap(FriendlyByteBuf, Map, Function, Function)
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param buffer the buffer
	 * @param elms the map
	 * @param keywriter the key writer
	 * @param valuewriter the value writer
	 */
	public static <K, V> void writeMap(FriendlyByteBuf buffer, Map<K, V> elms, BiConsumer<K, FriendlyByteBuf> keywriter, BiConsumer<V, FriendlyByteBuf> valuewriter) {
		writeListNullable(buffer, elms.entrySet(), (p, b) -> {
			keywriter.accept(p.getKey(), b);
			valuewriter.accept(p.getValue(), b);
		});
	}

	/**
	 * Write optional value to packet, the write function would be called to write data when optional is not null
	 *
	 * @param <T> the optional type
	 * @param buffer the buffer
	 * @param data the optional data
	 * @param func the write function
	 */
	public static <T> void writeOptional(FriendlyByteBuf buffer, Optional<T> data, BiConsumer<T, FriendlyByteBuf> func) {
		if (data.isPresent()) {
			buffer.writeBoolean(true);
			func.accept(data.get(), buffer);
			return;
		}
		buffer.writeBoolean(false);
	}

	/**
	 * Write nullable data as optional to packet.
	 * @see #writeOptional(FriendlyByteBuf, Optional, BiConsumer)
	 * @param <T> the optional type
	 * @param buffer the buffer
	 * @param data the data
	 * @param func the write function
	 */
	public static <T> void writeOptional(FriendlyByteBuf buffer,@Nullable T data, BiConsumer<T, FriendlyByteBuf> func) {
		writeOptional(buffer, Optional.ofNullable(data), func);
	}

	/**
	 * Write nullable data as optional to packet.
	 * This method is different from {@link #writeOptional(FriendlyByteBuf, Object, BiConsumer) writeOptional} with reversed function parameter order, so that :: operator can be used 
	 * @see #writeOptional(FriendlyByteBuf, Optional, BiConsumer)
	 * @param <T> the optional type
	 * @param buffer the buffer
	 * @param data the data
	 * @param func the write function
	 */
	public static <T> void writeOptional2(FriendlyByteBuf buffer, T data, BiConsumer<FriendlyByteBuf, T> func) {
		writeOptional(buffer, data, (a, b) -> func.accept(b, a));
	}

	/**
	 * Write short array.
	 *
	 * @param buffer the buffer
	 * @param arr the array
	 */
	public static void writeShortArray(FriendlyByteBuf buffer, short[] arr) {
		if (arr == null) {
			buffer.writeBoolean(false);
			return;
		}
		buffer.writeBoolean(true);
		buffer.writeVarInt(arr.length);
		for (short s : arr)
			buffer.writeShort(s);
	}

	/**
	 * Write string-value map to the packet, the value writer is called to write each entry.
	 *
	 * @see #readStringMap(FriendlyByteBuf, Map, Function)
	 * @param <V> the value type
	 * @param buffer the buffer
	 * @param elms the map
	 * @param valuewriter the value writer
	 */
	public static <V> void writeStringMap(FriendlyByteBuf buffer, Map<String, V> elms, BiConsumer<V, FriendlyByteBuf> valuewriter) {
		writeMap(buffer, elms, (p, b) -> b.writeUtf(p), valuewriter);
	}
	private SerializeUtil() {

	}
}
