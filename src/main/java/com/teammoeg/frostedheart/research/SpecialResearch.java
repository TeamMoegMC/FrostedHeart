/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.research;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.research.research.Research;
import com.teammoeg.frostedheart.util.io.CustomSerializerRegistry;

import net.minecraft.network.PacketBuffer;

public class SpecialResearch {
    private static CustomSerializerRegistry<Research, BiFunction<String, JsonObject, Research>> registry = new CustomSerializerRegistry<>();

    static {
        SpecialResearch.register(Research.class, "default", Research::new, Research::new);
    }

    public static Research deserialize(PacketBuffer data) {
        return registry.read(data);
    }

    public static Research deserialize(String id, JsonObject jo) {
        BiFunction<String, JsonObject, Research> i = registry.getDeserializeOrDefault(jo, Research::new);

        return i.apply(id, jo);
    }

    public static void register(Class<? extends Research> cls, String type, BiFunction<String, JsonObject, Research> json, Function<PacketBuffer, Research> packet) {
        registry.register(cls, type, json, packet);
    }

    public static void writeId(Research e, PacketBuffer pb) {
        registry.writeId(pb, e);
    }

    public static void writeType(Research e, JsonObject jo) {
        if (e.getClass() != Research.class)
            registry.writeType(jo, e);
    }

    private SpecialResearch() {
    }
}
