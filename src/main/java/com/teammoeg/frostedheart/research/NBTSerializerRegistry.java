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

package com.teammoeg.frostedheart.research;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonObject;

import net.minecraft.nbt.CompoundNBT;

public class NBTSerializerRegistry<U> extends SerializerRegistry<U, CompoundNBT> {

    Map<String, Function<CompoundNBT, U>> fromJson = new HashMap<>();

    public NBTSerializerRegistry() {
        super();
    }

    public U deserialize(CompoundNBT je) {
        Function<CompoundNBT, U> func = fromJson.get(je.getString("type"));
        if (func == null)
            return null;
        return func.apply(je);
    }

    public U deserializeOrDefault(CompoundNBT je, U def) {
        Function<CompoundNBT, U> func = fromJson.get(je.getString("type"));
        if (func == null)
            return def;
        return func.apply(je);
    }

    @Override
    protected void putSerializer(String type, Function<CompoundNBT, U> s) {
        fromJson.put(type, s);
    }

    public void writeType(JsonObject jo, U obj) {
        jo.addProperty("type", typeOf(obj));
    }
}
