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

package com.teammoeg.chorda.io.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 自定义序列化器注册表。管理类型的JSON反序列化器和数据包缓冲区序列化，
 * 支持基于类型字符串的分发式序列化。
 * <p>
 * Custom serializer registry. Manages JSON deserializers and packet buffer serialization for types,
 * supporting dispatch-style serialization based on type strings.
 *
 * @param <T> 要序列化的基础类型 / the base type to serialize
 * @param <U> JSON反序列化器的类型 / the type of JSON deserializer
 */
public class CustomSerializerRegistry<T, U> {
    /** 类到类型信息（ID和名称）的映射 / Map from class to type info (ID and name) */
    protected Map<Class<? extends T>, Pair<Integer, String>> typeInfo = new HashMap<>();
    /** 按ID索引的数据包反序列化函数列表 / List of packet deserialization functions indexed by ID */
    protected List<Function<FriendlyByteBuf, T>> fromPacket = new ArrayList<>();
    /** 类型名称到JSON反序列化器的映射 / Map from type name to JSON deserializer */
    Map<String, U> fromJson = new HashMap<>();

    /**
     * 构造一个自定义序列化器注册表。
     * <p>
     * Constructs a custom serializer registry.
     */
    public CustomSerializerRegistry() {
        super();
    }

    /**
     * 根据JSON元素中的"type"字段获取对应的反序列化器，如果找不到则返回默认值。
     * <p>
     * Gets the deserializer based on the "type" field in the JSON element, or returns the default if not found.
     *
     * @param je JSON元素 / the JSON element
     * @param def 默认的反序列化器 / the default deserializer
     * @return 对应的反序列化器或默认值 / the corresponding deserializer or the default
     */
    public U getDeserializeOrDefault(JsonElement je, U def) {
        JsonObject jo = je.getAsJsonObject();
        if (!jo.has("type"))
            return def;
        U func = fromJson.get(jo.get("type").getAsString());
        if (func == null)
            return def;
        return func;
    }

    /**
     * 根据JSON元素中的"type"字段获取对应的反序列化器。
     * <p>
     * Gets the deserializer based on the "type" field in the JSON element.
     *
     * @param je JSON元素 / the JSON element
     * @return 对应的反序列化器，如果未找到则返回null / the corresponding deserializer, or null if not found
     */
    public U getDeserializer(JsonElement je) {
        JsonObject jo = je.getAsJsonObject();
        return fromJson.get(jo.get("type").getAsString());
    }

    /**
     * 获取对象的注册数字ID。
     * <p>
     * Gets the registered numeric ID of an object.
     *
     * @param obj 要查询ID的对象 / the object to query the ID for
     * @return 对象的数字ID，如果未注册则返回-1 / the numeric ID of the object, or -1 if not registered
     */
    public int idOf(T obj) {
        Pair<Integer, String> info = typeInfo.get(obj.getClass());
        if (info == null)
            return -1;
        return info.getFirst();
    }

    /**
     * 将反序列化器存入JSON反序列化器映射中。
     * <p>
     * Puts a deserializer into the JSON deserializer map.
     *
     * @param type 类型标识符 / the type identifier
     * @param s 反序列化器 / the deserializer
     */
    protected  void  putSerializer(String type, U s) {
        fromJson.put(type, s);
    }

    /**
     * 从数据包缓冲区读取对象。先读取一个字节作为类型ID，再调用对应的反序列化函数。
     * <p>
     * Reads an object from a packet buffer. Reads a byte as the type ID first,
     * then calls the corresponding deserialization function.
     *
     * @param pb 数据包缓冲区 / the packet buffer
     * @return 反序列化后的对象 / the deserialized object
     * @throws IllegalArgumentException 如果类型ID无效 / if the type ID is invalid
     */
    public T read(FriendlyByteBuf pb) {
        int id = pb.readByte();
        if (id < 0 || id >= fromPacket.size())
            throw new IllegalArgumentException("Packet Error");
        return fromPacket.get(id).apply(pb);
    }

    /**
     * 从数据包缓冲区读取对象，如果类型ID无效则返回默认值。
     * <p>
     * Reads an object from a packet buffer, returning the default if the type ID is invalid.
     *
     * @param pb 数据包缓冲区 / the packet buffer
     * @param def 默认值 / the default value
     * @return 反序列化后的对象或默认值 / the deserialized object or the default value
     */
    public T readOrDefault(FriendlyByteBuf pb, T def) {
        int id = pb.readByte();
        if (id < 0 || id >= fromPacket.size())
            return def;
        return fromPacket.get(id).apply(pb);
    }

    /**
     * 注册一个类型及其JSON反序列化器和数据包反序列化函数。此方法是线程安全的。
     * <p>
     * Registers a type with its JSON deserializer and packet deserialization function. This method is thread-safe.
     *
     * @param cls 要注册的类 / the class to register
     * @param type 类型标识符字符串 / the type identifier string
     * @param json JSON反序列化器 / the JSON deserializer
     * @param packet 从数据包缓冲区读取对象的函数 / function to read the object from a packet buffer
     */
    public synchronized void register(Class<? extends T> cls, String type, U json, Function<FriendlyByteBuf, T> packet) {
        putSerializer(type, json);
        int id = fromPacket.size();
        fromPacket.add(packet);
        typeInfo.put(cls, Pair.of(id, type));
    }

    /**
     * 获取对象的注册类型标识符字符串。
     * <p>
     * Gets the registered type identifier string of an object.
     *
     * @param obj 要查询类型的对象 / the object to query the type for
     * @return 类型标识符字符串，如果未注册则返回空字符串 / the type identifier string, or empty string if not registered
     */
    public String typeOf(T obj) {
        Pair<Integer, String> info = typeInfo.get(obj.getClass());
        if (info == null)
            return "";
        return info.getSecond();
    }

    /**
     * 将对象的类型ID写入数据包缓冲区。
     * <p>
     * Writes the object's type ID to a packet buffer.
     *
     * @param pb 数据包缓冲区 / the packet buffer
     * @param obj 要写入ID的对象 / the object whose ID to write
     */
    public void writeId(FriendlyByteBuf pb, T obj) {
        pb.writeByte(idOf(obj));
    }

    /**
     * 将对象的类型标识符写入JSON对象的"type"属性。
     * <p>
     * Writes the object's type identifier to the "type" property of a JSON object.
     *
     * @param jo JSON对象 / the JSON object
     * @param obj 要写入类型的对象 / the object whose type to write
     */
    public void writeType(JsonObject jo, T obj) {
        jo.addProperty("type", typeOf(obj));
    }
}
