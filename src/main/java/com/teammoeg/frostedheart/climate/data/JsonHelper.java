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

package com.teammoeg.frostedheart.climate.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

public class JsonHelper {
    public static final Boolean getBoolean(JsonObject jo, String mem) {
        return getBooleanOrDefault(jo, mem, null);
    }

    public static final Boolean getBooleanOrDefault(JsonObject jo, String mem, Boolean def) {
        JsonElement je = jo.get(mem);
        if (je == null) return def;
        return je.getAsBoolean();
    }

    public static final Float getFloat(JsonObject jo, String mem) {
        return getFloatOrDefault(jo, mem, null);
    }

    public static final Float getFloatOrDefault(JsonObject jo, String mem, Float def) {
        JsonElement je = jo.get(mem);
        if (je == null) return def;
        return je.getAsFloat();
    }

    public static final Integer getInt(JsonObject jo, String mem) {
        return getIntOrDefault(jo, mem, null);
    }

    public static final Integer getIntOrDefault(JsonObject jo, String mem, Integer def) {
        JsonElement je = jo.get(mem);
        if (je == null) return def;
        return je.getAsInt();
    }

    public static final ResourceLocation getResourceLocation(JsonObject jo, String mem) {
        return new ResourceLocation(jo.get(mem).getAsString());
    }

    public static final String getString(JsonObject jo, String mem) {
        return getStringOrDefault(jo, mem, null);
    }

    public static final String getStringOrDefault(JsonObject jo, String mem, String def) {
        JsonElement je = jo.get(mem);
        if (je == null) return def;
        return je.getAsString();
    }
}
