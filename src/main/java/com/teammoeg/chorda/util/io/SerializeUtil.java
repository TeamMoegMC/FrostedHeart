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

package com.teammoeg.chorda.util.io;

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

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.util.RegistryUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class SerializeUtil {
	public static class Deserializer<T extends JsonElement, U extends Writeable> {
		private int id;
		public Function<T, U> fromJson;
		public Function<FriendlyByteBuf, U> fromPacket;

		public Deserializer(Function<T, U> fromJson, Function<FriendlyByteBuf, U> fromPacket) {
			super();
			this.fromJson = fromJson;
			this.fromPacket = fromPacket;
		}

		public U read(FriendlyByteBuf packet) {
			return fromPacket.apply(packet);
		}

		public U read(T json) {
			return fromJson.apply(json);
		}

		public JsonElement serialize(U obj) {
			return obj.serialize();
		}

		public void write(FriendlyByteBuf packet, U obj) {
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
					ret.setTag(TagParser.parseTag(jo.get("nbt").getAsString()));
				} catch (CommandSyntaxException e) {
					Chorda.LOGGER.warn(e.getMessage());
				}
			return ret;
		}
		return ItemStack.EMPTY;
	}

	public static <K, V> Function<K, V> cached(Function<K, V> func) {
		Map<K, V> map = new HashMap<>();
		return k -> map.computeIfAbsent(k, func);
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

	public static boolean[] readBooleans(FriendlyByteBuf buffer) {
		return readBooleans(buffer.readByte());
	}

	public static boolean[] readBooleans(byte in) {
		boolean[] ret = new boolean[8];
		for (int i = ret.length - 1; i >= 0; i--) {
			ret[i] = (in & 1) != 0;
			in >>= 1;
		}
		return ret;
	}
	
	public static boolean[] readLongBooleans(long in,int size) {
		boolean[] ret = new boolean[size];
		for (int i = ret.length - 1; i >= 0; i--) {
			ret[i] = (in & 1) != 0;
			in >>= 1;
		}
		return ret;
	}
	
	public static long writeLongBooleans(boolean... elms) {
		long b = 0;
		for (int i = 0; i < elms.length; i++) {
			boolean bl = elms[i];
			b <<= 1;
			b |= (long) (bl ? 1 : 0);

		}
		return b;
	}

	public static <T> List<T> readListNullable(FriendlyByteBuf buffer, Function<FriendlyByteBuf, T> func) {
		if (!buffer.readBoolean())
			return null;
		return readList(buffer, func);
	}

	public static <T> List<T> readList(FriendlyByteBuf buffer, Function<FriendlyByteBuf, T> func) {
		int cnt = buffer.readVarInt();
		List<T> nums = new ArrayList<>(cnt);
		for (int i = 0; i < cnt; i++)
			nums.add(func.apply(buffer));
		return nums;
	}

	public static <K, V> Map<K, V> readMap(FriendlyByteBuf buffer, Map<K, V> map, Function<FriendlyByteBuf, K> keyreader, Function<FriendlyByteBuf, V> valuereader) {
		map.clear();
		if (!buffer.readBoolean())
			return map;
		int cnt = buffer.readVarInt();
		for (int i = 0; i < cnt; i++)
			map.put(keyreader.apply(buffer), valuereader.apply(buffer));
		return map;
	}

	public static <K, V> Map<K, V> readEntry(FriendlyByteBuf buffer, Map<K, V> map, BiConsumer<FriendlyByteBuf, BiConsumer<K, V>> reader) {
		map.clear();
		int cnt = buffer.readVarInt();
		for (int i = 0; i < cnt; i++)
			reader.accept(buffer, map::put);
		return map;
	}

	public static <T> Optional<T> readOptional(FriendlyByteBuf buffer, Function<FriendlyByteBuf, T> func) {
		if (buffer.readBoolean())
			return Optional.ofNullable(func.apply(buffer));
		return Optional.empty();
	}

	public static short[] readShortArray(FriendlyByteBuf buffer) {
		if (!buffer.readBoolean())
			return null;
		int cnt = buffer.readVarInt();
		short[] nums = new short[cnt];
		for (int i = 0; i < cnt; i++)
			nums[i] = buffer.readShort();
		return nums;
	}

	public static <V> Map<String, V> readStringMap(FriendlyByteBuf buffer, Map<String, V> map, Function<FriendlyByteBuf, V> valuereader) {
		return readMap(buffer, map, FriendlyByteBuf::readUtf, valuereader);
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

	public static <T> ListTag toNBTList(Collection<T> stacks, BiConsumer<T, ArrayNBTBuilder<Void>> mapper) {
		ArrayNBTBuilder<Void> arrayBuilder = ArrayNBTBuilder.create();
		stacks.stream().forEach(t -> mapper.accept(t, arrayBuilder));
		return arrayBuilder.build();
	}

	public static <T> CompoundTag toNBTMap(Collection<T> stacks, BiConsumer<T, CompoundNBTBuilder<Void>> mapper) {
		CompoundNBTBuilder<Void> compoundBuilder = CompoundNBTBuilder.create();
		stacks.stream().forEach(t -> mapper.accept(t, compoundBuilder));
		return compoundBuilder.build();
	}

	/**
	 * Write boolean as a byte into buffer
	 *
	 * @param elms elements to write, 8 elements max
	 */
	public static void writeBooleans(FriendlyByteBuf buffer, boolean... elms) {
		buffer.writeByte(writeBooleans(elms));
	}

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

	public static <T> void writeListNullable(FriendlyByteBuf buffer, Collection<T> elms, BiConsumer<T, FriendlyByteBuf> func) {
		if (elms == null) {
			buffer.writeBoolean(false);
			return;
		}
		buffer.writeBoolean(true);
		writeList(buffer, elms, func);
	}

	public static <T> void writeListNullable2(FriendlyByteBuf buffer, Collection<T> elms, BiConsumer<FriendlyByteBuf, T> func) {
		if (elms == null) {
			buffer.writeBoolean(false);
			return;
		}
		buffer.writeBoolean(true);
		writeList2(buffer, elms, func);
	}

	public static <T> void writeList(FriendlyByteBuf buffer, Collection<T> elms, BiConsumer<T, FriendlyByteBuf> func) {
		buffer.writeVarInt(elms.size());
		elms.forEach(e -> func.accept(e, buffer));
	}

	public static <K, V> void writeEntry(FriendlyByteBuf buffer, Map<K, V> elms, BiConsumer<Map.Entry<K, V>, FriendlyByteBuf> func) {
		buffer.writeVarInt(elms.size());
		elms.entrySet().forEach(e -> func.accept(e, buffer));
	}

	public static <T> void writeList2(FriendlyByteBuf buffer, Collection<T> elms, BiConsumer<FriendlyByteBuf, T> func) {
		buffer.writeVarInt(elms.size());
		elms.forEach(e -> func.accept(buffer, e));
	}

	public static <K, V> void writeMap(FriendlyByteBuf buffer, Map<K, V> elms, BiConsumer<K, FriendlyByteBuf> keywriter, BiConsumer<V, FriendlyByteBuf> valuewriter) {
		writeListNullable(buffer, elms.entrySet(), (p, b) -> {
			keywriter.accept(p.getKey(), b);
			valuewriter.accept(p.getValue(), b);
		});
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public static <T> void writeOptional(FriendlyByteBuf buffer, Optional<T> data, BiConsumer<T, FriendlyByteBuf> func) {
		if (data.isPresent()) {
			buffer.writeBoolean(true);
			func.accept(data.get(), buffer);
			return;
		}
		buffer.writeBoolean(false);
	}

	public static <T> void writeOptional(FriendlyByteBuf buffer, T data, BiConsumer<T, FriendlyByteBuf> func) {
		writeOptional(buffer, Optional.ofNullable(data), func);
	}

	public static <T> void writeOptional2(FriendlyByteBuf buffer, T data, BiConsumer<FriendlyByteBuf, T> func) {
		writeOptional(buffer, data, (a, b) -> func.accept(b, a));
	}

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

	public static <V> void writeStringMap(FriendlyByteBuf buffer, Map<String, V> elms, BiConsumer<V, FriendlyByteBuf> valuewriter) {
		writeMap(buffer, elms, (p, b) -> b.writeUtf(p), valuewriter);
	}

	private SerializeUtil() {

	}
}
