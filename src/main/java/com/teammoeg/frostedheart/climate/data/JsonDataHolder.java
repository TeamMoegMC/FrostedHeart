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

import com.google.gson.JsonObject;

import net.minecraft.util.ResourceLocation;

public class JsonDataHolder {
    protected JsonObject data;

    public JsonDataHolder(JsonObject data) {
        this.data = data;
    }

    public Boolean getBoolean(String mem) {
        return JsonHelper.getBoolean(data, mem);
    }

    public Boolean getBooleanOrDefault(String mem, Boolean def) {
        return JsonHelper.getBooleanOrDefault(data, mem, def);
    }

    public JsonObject getData() {
        return data;
    }

    public Float getFloat(String mem) {
        return JsonHelper.getFloat(data, mem);
    }

    public Float getFloatOrDefault(String mem, Float def) {
        return JsonHelper.getFloatOrDefault(data, mem, def);
    }

    public ResourceLocation getId() {
        return this.getResourceLocation("id");
    }

    public Integer getInt(String mem) {
        return JsonHelper.getInt(data, mem);
    }

    public Integer getIntOrDefault(String mem, Integer def) {
        return JsonHelper.getIntOrDefault(data, mem, def);
    }

    public ResourceLocation getResourceLocation(String mem) {
        return JsonHelper.getResourceLocation(data, mem);
    }

    public String getString(String mem) {
        return JsonHelper.getString(data, mem);
    }

    public String getStringOrDefault(String mem, String def) {
        return JsonHelper.getStringOrDefault(data, mem, def);
    }
}
