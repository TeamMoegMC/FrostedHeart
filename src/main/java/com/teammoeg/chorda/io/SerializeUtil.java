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
 * 序列化和网络通信的封装工具类，提供JSON、NBT、网络数据包的读写方法。
 * <p>
 * Utility class with wrapped functions for serialization and networking,
 * providing read/write methods for JSON, NBT, and network packets.
 */
@ParametersAreNonnullByDefault
public class SerializeUtil {
	
	/**
	 * 反序列化器类，支持从JSON和网络数据包两种来源反序列化对象。
	 * <p>
	 * Deserializer class supporting deserialization from both JSON and network packet sources.
	 *
	 * @param <T> JSON元素类型 / the JSON element type
	 * @param <U> 可写入对象类型 / the writable object type
	 */
	public static class Deserializer<T extends JsonElement, U extends Writeable> {

		/** 类型ID / the type ID. */
		private int id;

		/** JSON反序列化函数 / the JSON deserialization function. */
		public Function<T, U> fromJson;

		/** 数据包反序列化函数 / the packet deserialization function. */
		public Function<FriendlyByteBuf, U> fromPacket;

		/**
		 * 创建一个新的反序列化器实例。
		 * <p>
		 * Instantiates a new deserializer.
		 *
		 * @param fromJson JSON反序列化函数 / the JSON deserialization function
		 * @param fromPacket 数据包反序列化函数 / the packet deserialization function
		 */
		public Deserializer(Function<T, U> fromJson, Function<FriendlyByteBuf, U> fromPacket) {
			super();
			this.fromJson = fromJson;
			this.fromPacket = fromPacket;
		}

		/**
		 * 从网络数据包读取对象。
		 * <p>
		 * Reads an object from a network packet.
		 *
		 * @param packet 网络数据包 / the network packet
		 * @return 反序列化的对象 / the deserialized object
		 */
		public U read(FriendlyByteBuf packet) {
			return fromPacket.apply(packet);
		}

		/**
		 * 从JSON元素读取对象。
		 * <p>
		 * Reads an object from a JSON element.
		 *
		 * @param json JSON元素 / the JSON element
		 * @return 反序列化的对象 / the deserialized object
		 */
		public U read(T json) {
			return fromJson.apply(json);
		}

		/**
		 * 将对象序列化为JSON元素。
		 * <p>
		 * Serializes an object to a JSON element.
		 *
		 * @param obj 要序列化的对象 / the object to serialize
		 * @return JSON元素 / the JSON element
		 */
		public JsonElement serialize(U obj) {
			return obj.serialize();
		}

		/**
		 * 将对象写入网络数据包，包含类型ID前缀。
		 * <p>
		 * Writes an object to a network packet with a type ID prefix.
		 *
		 * @param packet 网络数据包 / the network packet
		 * @param obj 要写入的对象 / the object to write
		 */
		public void write(FriendlyByteBuf packet, U obj) {
			packet.writeVarInt(id);
			obj.write(packet);
		}
	}
	
	/**
	 * 从JSON元素读取ItemStack。
	 * <p>
	 * Read ItemStack from JSON element.
	 * @see #toJson
	 * @param elm JSON元素 / the JSON element
	 * @return 物品栈 / the item stack
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
	 * 缓存生成器，将函数结果缓存以避免重复计算。
	 * <p>
	 * Cached generator that caches function results to avoid repeated computation.
	 * @deprecated 请使用 {@link net.minecraft.Util#memoize(java.util.function.Function) Util.memorize} 以获得更好的多线程支持 / use Util.memorize for better multithreading
	 * @param <K> 键类型 / the key type
	 * @param <V> 值类型 / the value type
	 * @param func 生成器函数 / the generator function
	 * @return 缓存后的生成器函数 / the cached generator function
	 */
	@Deprecated
	public static <K, V> Function<K, V> cached(Function<K, V> func) {
		Map<K, V> map = new HashMap<>();
		return k -> map.computeIfAbsent(k, func);
	}

	/**
	 * 解析JSON元素列表，自动检测数组/null/单元素情况。
	 * <p>
	 * Parses a JSON element list, automatically detecting array/null/single-element cases.
	 *
	 * @param <T> 返回对象类型 / the return object type
	 * @param elm 从文件读取的JSON元素 / the JSON element read from file
	 * @param mapper JSON到对象的转换函数 / the mapper that converts JSON to object
	 * @return 对象列表 / the object list
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
	 * 解析JSON对象列表，自动检测数组/null/单对象情况。
	 * <p>
	 * Parses a JSON object list, automatically detecting array/null/single-object cases.
	 *
	 * @throws IllegalStateException 当数组元素或元素本身不是JSON对象时抛出 / when array element or element itself is not a JSON object
	 * @param <T> 返回对象类型 / the return object type
	 * @param elm JSON元素 / the JSON element
	 * @param mapper JSON对象到对象的转换函数 / the mapper that converts JSON object to object
	 * @return 对象列表 / the object list
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
	 * 从网络缓冲区读取8个布尔标志位。
	 * <p>
	 * Reads 8 boolean flags from byte bits in a network buffer.
	 * @see #writeBooleans(FriendlyByteBuf, boolean...)
	 * @param buffer 网络缓冲区 / the network buffer
	 * @return 布尔标志数组 / the boolean flags array
	 */
	public static boolean[] readBooleans(FriendlyByteBuf buffer) {
		return readBooleans(buffer.readByte());
	}

	/**
	 * 从字节位中读取布尔标志。
	 * <p>
	 * Reads boolean flags from byte bits.
	 * @see #writeBooleans(boolean...)
	 * @param in 输入字节 / the input byte
	 * @return 布尔标志数组 / the boolean flags array
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
	 * 从long值中按大端序读取指定数量的布尔标志。
	 * <p>
	 * Reads a specified number of boolean flags from a long value in big-endian order.
	 * @see #writeLongBooleans
	 * @param in 输入long值 / the input long value
	 * @param size 标志数量 / the flag count
	 * @return 布尔标志数组 / the boolean flags array
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
	 * 将布尔标志按大端序写入long值。
	 * <p>
	 * Writes boolean flags to a long value in big-endian order.
	 * @see #readLongBooleans
	 * @param elms 布尔标志 / the boolean flags
	 * @return 编码后的long值 / the encoded long value
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
	 * 从缓冲区读取可空列表，若写入时为null则返回null。
	 * <p>
	 * Reads a nullable list from buffer, may return null if the written list was null.
	 * @see #readList(FriendlyByteBuf, Function)
	 * @see #writeListNullable(FriendlyByteBuf, Collection, BiConsumer)
	 * @see #writeListNullable2(FriendlyByteBuf, Collection, BiConsumer)
	 * @param <T> 列表元素类型 / the list element type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param func 元素读取方法 / the read method for each element
	 * @return 列表或null / the list or null
	 */
	@Nullable
	public static <T> List<T> readListNullable(FriendlyByteBuf buffer, Function<FriendlyByteBuf, T> func) {
		if (!buffer.readBoolean())
			return null;
		return readList(buffer, func);
	}

	/**
	 * 从缓冲区读取列表。
	 * <p>
	 * Reads a list from buffer.
	 * @see #writeList(FriendlyByteBuf, Collection, BiConsumer)
	 * @see #writeList2(FriendlyByteBuf, Collection, BiConsumer)
	 * @param <T> 列表元素类型 / the list element type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param func 元素读取方法 / the read method for each element
	 * @return 读取的列表 / the list
	 */
	public static <T> List<T> readList(FriendlyByteBuf buffer, Function<FriendlyByteBuf, T> func) {
		int cnt = buffer.readVarInt();
		List<T> nums = new ArrayList<>(cnt);
		for (int i = 0; i < cnt; i++)
			nums.add(func.apply(buffer));
		return nums;
	}

	/**
	 * 使用分别的键和值读取器从缓冲区读取并设置Map。
	 * <p>
	 * Reads values from buffer and sets the provided map using separate key and value readers.
	 * @see #writeMap(FriendlyByteBuf, Map, BiConsumer, BiConsumer)
	 * @param <K> 键类型 / the key type
	 * @param <V> 值类型 / the value type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param map 要填充的Map / the map to populate
	 * @param keyreader 键读取器 / the key reader
	 * @param valuereader 值读取器 / the value reader
	 * @return 填充后的Map / the populated map
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
	 * 使用单一条目读取器从缓冲区读取并设置Map。
	 * <p>
	 * Reads values from buffer and sets the provided map using a single entry reader.
	 * @see #writeEntry(FriendlyByteBuf, Map, BiConsumer)
	 * @param <K> 键类型 / the key type
	 * @param <V> 值类型 / the value type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param map 要填充的Map / the map to populate
	 * @param reader 条目读取器 / the entry reader
	 * @return 填充后的Map / the populated map
	 */
	public static <K, V> Map<K, V> readEntry(FriendlyByteBuf buffer, Map<K, V> map, BiConsumer<FriendlyByteBuf, BiConsumer<K, V>> reader) {
		map.clear();
		int cnt = buffer.readVarInt();
		for (int i = 0; i < cnt; i++)
			reader.accept(buffer, map::put);
		return map;
	}

	/**
	 * 从数据包读取Optional值。
	 * <p>
	 * Reads an Optional value from a packet.
	 * @see #writeOptional(FriendlyByteBuf, Optional, BiConsumer)
	 * @see #writeOptional(FriendlyByteBuf, Object, BiConsumer)
	 * @see #writeOptional2(FriendlyByteBuf, Object, BiConsumer)
	 * @param <T> Optional类型 / the optional type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param func 对象存在时的读取函数 / the read function when object is present
	 * @return Optional值 / the optional value
	 */
	public static <T> Optional<T> readOptional(FriendlyByteBuf buffer, Function<FriendlyByteBuf, T> func) {
		if (buffer.readBoolean())
			return Optional.ofNullable(func.apply(buffer));
		return Optional.empty();
	}

	/**
	 * 从数据包读取short数组。
	 * <p>
	 * Reads a short array from a packet.
	 * @see #writeShortArray(FriendlyByteBuf, short[])
	 * @param buffer 网络缓冲区 / the network buffer
	 * @return short数组 / the short array
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
	 * 读取字符串键Map，使用值读取器读取每个条目。
	 * <p>
	 * Reads a string-keyed map, using a value reader for each entry.
	 * @see #readStringMap(FriendlyByteBuf, Map, Function)
	 * @param <V> 值类型 / the value type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param map 要填充的Map / the map to populate
	 * @param valuereader 值读取器 / the value reader
	 * @return 填充后的Map / the populated map
	 */
	public static <V> Map<String, V> readStringMap(FriendlyByteBuf buffer, Map<String, V> map, Function<FriendlyByteBuf, V> valuereader) {
		return readMap(buffer, map, FriendlyByteBuf::readUtf, valuereader);
	}

	/**
	 * 将物品栈保存为JSON。
	 * <p>
	 * Saves an ItemStack to JSON.
	 * @see #fromJson(JsonElement)
	 * @param stack 物品栈 / the item stack
	 * @return JSON元素 / the JSON element
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
	 * 将集合转换为JSON数组。
	 * <p>
	 * Converts a collection to a JSON array.
	 *
	 * @param <T> 集合元素类型 / the collection element type
	 * @param li 集合 / the collection
	 * @param mapper 元素转JSON函数 / the function to convert element to JSON
	 * @return JSON数组 / the JSON array
	 */
	public static <T> JsonArray toJsonList(Collection<T> li, Function<T, JsonElement> mapper) {
		JsonArray ja = new JsonArray();
		li.stream().map(mapper).forEach(ja::add);
		return ja;
	}

	/**
	 * 将集合转换为JSON字符串列表，使用toString序列化。
	 * <p>
	 * Converts a collection to a JSON string list using toString serialization.
	 * @see #toJsonList(Collection, Function)
	 *
	 * @param <T> 原始对象类型 / the original object type
	 * @param <B> toString类型 / the toString type
	 * @param li 集合 / the collection
	 * @param mapper 元素到toString对象的转换函数 / the function to convert element to toString object
	 * @return JSON数组 / the JSON array
	 */
	public static <T, B> JsonArray toJsonStringList(Collection<T> li, Function<T, B> mapper) {
		JsonArray ja = new JsonArray();
		li.stream().map(mapper).map(B::toString).forEach(ja::add);
		return ja;
	}

	/**
	 * 将集合序列化为NBT列表标签。
	 * <p>
	 * Serializes a collection to an NBT list tag.
	 *
	 * @param <T> 元素类型 / the element type
	 * @param stacks 集合 / the collection
	 * @param mapper 元素序列化器，第二个参数为数组NBT元素构建器 / the element serializer, second argument is the array NBT element builder
	 * @return 列表标签 / the list tag
	 */
	public static <T> ListTag toNBTList(Collection<T> stacks, BiConsumer<T, ArrayNBTBuilder<Void>> mapper) {
		ArrayNBTBuilder<Void> arrayBuilder = ArrayNBTBuilder.create();
		stacks.stream().forEach(t -> mapper.accept(t, arrayBuilder));
		return arrayBuilder.build();
	}

	/**
	 * 将集合序列化为NBT复合标签（Map形式）。
	 * <p>
	 * Serializes a collection to an NBT compound tag (map form).
	 *
	 * @param <T> 元素类型 / the element type
	 * @param stacks 集合 / the collection
	 * @param mapper 元素序列化器，第二个参数为Map NBT条目构建器 / the element serializer, second argument is the map NBT entry builder
	 * @return 复合标签 / the compound tag
	 */
	public static <T> CompoundTag toNBTMap(Collection<T> stacks, BiConsumer<T, CompoundNBTBuilder<Void>> mapper) {
		CompoundNBTBuilder<Void> compoundBuilder = CompoundNBTBuilder.create();
		stacks.stream().forEach(t -> mapper.accept(t, compoundBuilder));
		return compoundBuilder.build();
	}

	/**
	 * 将布尔标志作为一个字节写入缓冲区。
	 * <p>
	 * Writes boolean flags as a byte into buffer.
	 *
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param elms 要写入的元素，最多8个 / elements to write, 8 elements max
	 */
	public static void writeBooleans(FriendlyByteBuf buffer, boolean... elms) {
		buffer.writeByte(writeBooleans(elms));
	}

	/**
	 * 将布尔标志写入一个字节的各位。
	 * <p>
	 * Writes boolean flags to a byte's bits.
	 * @see #readBooleans(byte)
	 * @param elms 布尔标志 / the boolean flags
	 * @return 编码后的字节 / the encoded byte
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
	 * 将可空集合写入数据包。
	 * <p>
	 * Writes a nullable collection to the packet.
	 * @see #readListNullable(FriendlyByteBuf, Function)
	 * @see #writeListNullable2(FriendlyByteBuf, Collection, BiConsumer)
	 * @param <T> 元素类型 / the element type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param elms 集合 / the collection
	 * @param func 写入函数 / the write function
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
	 * 将可空集合写入数据包，函数参数顺序反转以支持方法引用。
	 * <p>
	 * Writes a nullable collection to the packet with reversed function parameter order for method reference support.
	 * @see #readListNullable(FriendlyByteBuf, Function)
	 * @see #writeListNullable(FriendlyByteBuf, Collection, BiConsumer)
	 * @param <T> 元素类型 / the element type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param elms 集合 / the collection
	 * @param func 写入函数 / the write function
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
	 * 将非空集合写入数据包。
	 * <p>
	 * Writes a non-null collection to the packet.
	 * @see #readList(FriendlyByteBuf, Function)
	 * @see #writeList2(FriendlyByteBuf, Collection, BiConsumer)
	 * @param <T> 元素类型 / the element type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param elms 集合 / the collection
	 * @param func 写入函数 / the write function
	 */
	public static <T> void writeList(FriendlyByteBuf buffer, Collection<T> elms, BiConsumer<T, FriendlyByteBuf> func) {
		buffer.writeVarInt(elms.size());
		elms.forEach(e -> func.accept(e, buffer));
	}

	/**
	 * 将Map的条目写入数据包。
	 * <p>
	 * Writes map entries to the packet with the given write function.
	 * @see #readEntry(FriendlyByteBuf, Map, BiConsumer)
	 * @param <K> 键类型 / the key type
	 * @param <V> 值类型 / the value type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param elms Map / the map
	 * @param func 写入函数 / the write function
	 */
	public static <K, V> void writeEntry(FriendlyByteBuf buffer, Map<K, V> elms, BiConsumer<Map.Entry<K, V>, FriendlyByteBuf> func) {
		buffer.writeVarInt(elms.size());
		elms.entrySet().forEach(e -> func.accept(e, buffer));
	}

	/**
	 * 将非空集合写入数据包，函数参数顺序反转以支持方法引用。
	 * <p>
	 * Writes a non-null collection to the packet with reversed function parameter order for method reference support.
	 * @see #readList(FriendlyByteBuf, Function)
	 * @see #writeList(FriendlyByteBuf, Collection, BiConsumer)
	 * @param <T> 元素类型 / the element type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param elms 集合 / the collection
	 * @param func 写入函数 / the write function
	 */
	public static <T> void writeList2(FriendlyByteBuf buffer, Collection<T> elms, BiConsumer<FriendlyByteBuf, T> func) {
		buffer.writeVarInt(elms.size());
		elms.forEach(e -> func.accept(buffer, e));
	}

	/**
	 * 使用指定的键和值写入器将Map写入数据包。
	 * <p>
	 * Writes a map to the packet with given key and value writers for each entry.
	 * @see #readMap(FriendlyByteBuf, Map, Function, Function)
	 * @param <K> 键类型 / the key type
	 * @param <V> 值类型 / the value type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param elms Map / the map
	 * @param keywriter 键写入器 / the key writer
	 * @param valuewriter 值写入器 / the value writer
	 */
	public static <K, V> void writeMap(FriendlyByteBuf buffer, Map<K, V> elms, BiConsumer<K, FriendlyByteBuf> keywriter, BiConsumer<V, FriendlyByteBuf> valuewriter) {
		writeListNullable(buffer, elms.entrySet(), (p, b) -> {
			keywriter.accept(p.getKey(), b);
			valuewriter.accept(p.getValue(), b);
		});
	}

	/**
	 * 将Optional值写入数据包，值存在时调用写入函数。
	 * <p>
	 * Writes an Optional value to the packet, calling the write function when the value is present.
	 *
	 * @param <T> Optional类型 / the optional type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param data Optional数据 / the optional data
	 * @param func 写入函数 / the write function
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
	 * 将可空数据作为Optional写入数据包。
	 * <p>
	 * Writes nullable data as an Optional to the packet.
	 * @see #writeOptional(FriendlyByteBuf, Optional, BiConsumer)
	 * @param <T> Optional类型 / the optional type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param data 数据 / the data
	 * @param func 写入函数 / the write function
	 */
	public static <T> void writeOptional(FriendlyByteBuf buffer,@Nullable T data, BiConsumer<T, FriendlyByteBuf> func) {
		writeOptional(buffer, Optional.ofNullable(data), func);
	}

	/**
	 * 将可空数据作为Optional写入数据包，函数参数顺序反转以支持方法引用。
	 * <p>
	 * Writes nullable data as Optional to the packet with reversed function parameter order for method reference support.
	 * @see #writeOptional(FriendlyByteBuf, Optional, BiConsumer)
	 * @param <T> Optional类型 / the optional type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param data 数据 / the data
	 * @param func 写入函数 / the write function
	 */
	public static <T> void writeOptional2(FriendlyByteBuf buffer, T data, BiConsumer<FriendlyByteBuf, T> func) {
		writeOptional(buffer, data, (a, b) -> func.accept(b, a));
	}

	/**
	 * 将short数组写入数据包。
	 * <p>
	 * Writes a short array to the packet.
	 *
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param arr short数组 / the short array
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
	 * 将字符串键Map写入数据包，使用值写入器写入每个条目。
	 * <p>
	 * Writes a string-keyed map to the packet, using a value writer for each entry.
	 *
	 * @see #readStringMap(FriendlyByteBuf, Map, Function)
	 * @param <V> 值类型 / the value type
	 * @param buffer 网络缓冲区 / the network buffer
	 * @param elms Map / the map
	 * @param valuewriter 值写入器 / the value writer
	 */
	public static <V> void writeStringMap(FriendlyByteBuf buffer, Map<String, V> elms, BiConsumer<V, FriendlyByteBuf> valuewriter) {
		writeMap(buffer, elms, (p, b) -> b.writeUtf(p), valuewriter);
	}
	private SerializeUtil() {

	}
}
