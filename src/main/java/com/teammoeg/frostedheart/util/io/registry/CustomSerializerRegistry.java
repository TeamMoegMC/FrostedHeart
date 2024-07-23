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

package com.teammoeg.frostedheart.util.io.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;

import net.minecraft.network.FriendlyByteBuf;

public class CustomSerializerRegistry<T, U> {
    protected Map<Class<? extends T>, Pair<Integer, String>> typeInfo = new HashMap<>();
    protected List<Function<FriendlyByteBuf, T>> fromPacket = new ArrayList<>();

    Map<String, U> fromJson = new HashMap<>();

    public CustomSerializerRegistry() {
        super();
    }

    public U getDeserializeOrDefault(JsonElement je, U def) {
        JsonObject jo = je.getAsJsonObject();
        if (!jo.has("type"))
            return def;
        U func = fromJson.get(jo.get("type").getAsString());
        if (func == null)
            return def;
        return func;
    }

    public U getDeserializer(JsonElement je) {
        JsonObject jo = je.getAsJsonObject();
        return fromJson.get(jo.get("type").getAsString());
    }

    public int idOf(T obj) {
        Pair<Integer, String> info = typeInfo.get(obj.getClass());
        if (info == null)
            return -1;
        return info.getFirst();
    }

    protected void putSerializer(String type, U s) {
        fromJson.put(type, s);
    }

    public T read(FriendlyByteBuf pb) {
        int id = pb.readByte();
        if (id < 0 || id >= fromPacket.size())
            throw new IllegalArgumentException("Packet Error");
        return fromPacket.get(id).apply(pb);
    }

    public T readOrDefault(FriendlyByteBuf pb, T def) {
        int id = pb.readByte();
        if (id < 0 || id >= fromPacket.size())
            return def;
        return fromPacket.get(id).apply(pb);
    }

    public void register(Class<? extends T> cls, String type, U json, Function<FriendlyByteBuf, T> packet) {
        putSerializer(type, json);
        int id = fromPacket.size();
        fromPacket.add(packet);
        typeInfo.put(cls, Pair.of(id, type));
    }

    public String typeOf(T obj) {
        Pair<Integer, String> info = typeInfo.get(obj.getClass());
        if (info == null)
            return "";
        return info.getSecond();
    }

    public void writeId(FriendlyByteBuf pb, T obj) {
        pb.writeByte(idOf(obj));
    }

    public void writeType(JsonObject jo, T obj) {
        jo.addProperty("type", typeOf(obj));
    }
}
