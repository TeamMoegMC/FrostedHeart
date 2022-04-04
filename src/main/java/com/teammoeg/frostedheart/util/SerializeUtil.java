/*
 * Copyright (c) 2022 TeamMoeg
 *
 * This file is part of Thermopolium.
 *
 * Thermopolium is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Thermopolium is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermopolium. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;

public class SerializeUtil {
	public static class Deserializer<T extends JsonElement, U extends Writeable> {
		private int id;
		public Function<T, U> fromJson;
		public Function<PacketBuffer, U> fromPacket;

		public Deserializer(Function<T, U> fromJson, Function<PacketBuffer, U> fromPacket) {
			super();
			this.fromJson = fromJson;
			this.fromPacket = fromPacket;
		}

		public U read(T json) {
			return fromJson.apply(json);
		}

		public U read(PacketBuffer packet) {
			return fromPacket.apply(packet);
		}

		public void write(PacketBuffer packet, U obj) {
			packet.writeVarInt(id);
			obj.write(packet);
		}

		public JsonElement serialize(U obj) {
			return obj.serialize();
		}
	}


	private SerializeUtil() {

	}


	public static <T> Optional<T> readOptional(PacketBuffer buffer, Function<PacketBuffer, T> func) {
		if (buffer.readBoolean())
			return Optional.ofNullable(func.apply(buffer));
		return Optional.empty();
	}

	public static <T> void writeOptional(PacketBuffer buffer, T data, BiConsumer<T, PacketBuffer> func) {
		writeOptional(buffer, Optional.ofNullable(data), func);
	}

	public static <T> void writeOptional(PacketBuffer buffer, Optional<T> data, BiConsumer<T, PacketBuffer> func) {
		if (data.isPresent()) {
			buffer.writeBoolean(true);
			func.accept(data.get(), buffer);
			return;
		}
		buffer.writeBoolean(false);
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

	public static <T> List<T> parseJsonList(JsonElement elm, Function<JsonObject, T> mapper) {
		if (elm == null)
			return Lists.newArrayList();
		if (elm.isJsonArray())
			return StreamSupport.stream(elm.getAsJsonArray().spliterator(), false).map(JsonElement::getAsJsonObject)
					.map(mapper).collect(Collectors.toList());
		return Lists.newArrayList(mapper.apply(elm.getAsJsonObject()));
	}

	public static <T> List<T> parseJsonElmList(JsonElement elm, Function<JsonElement, T> mapper) {
		if (elm == null)
			return Lists.newArrayList();
		if (elm.isJsonArray())
			return StreamSupport.stream(elm.getAsJsonArray().spliterator(), false).map(mapper)
					.collect(Collectors.toList());
		return Lists.newArrayList(mapper.apply(elm.getAsJsonObject()));
	}

	public static <T> JsonArray toJsonList(Collection<T> li, Function<T, JsonElement> mapper) {
		JsonArray ja = new JsonArray();
		li.stream().map(mapper).forEach(ja::add);
		return ja;
	}

	public static <T> ListNBT toNBTList(Collection<T> stacks, Function<T, INBT> mapper) {
		ListNBT nbt = new ListNBT();
		stacks.stream().map(mapper).forEach(nbt::add);
		return nbt;
	}
}
