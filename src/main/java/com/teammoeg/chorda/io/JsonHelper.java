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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

/**
 * JSON解析辅助类，提供从JsonObject中安全提取各种类型值的方法。
 * <p>
 * JSON parsing helper class providing methods to safely extract various typed values from JsonObject.
 */
public class JsonHelper {
    /**
     * 从JsonObject获取Boolean值。
     * <p>
     * Gets a Boolean value from a JsonObject.
     *
     * @param jo JSON对象 / the JSON object
     * @param mem 成员键名 / the member key name
     * @return Boolean值，不存在时返回null / the Boolean value, or null if not present
     */
    public static Boolean getBoolean(JsonObject jo, String mem) {
        return getBooleanOrDefault(jo, mem, null);
    }

    /**
     * 从JsonObject获取Boolean值，不存在时返回默认值。
     * <p>
     * Gets a Boolean value from a JsonObject, returning a default value if not present.
     *
     * @param jo JSON对象 / the JSON object
     * @param mem 成员键名 / the member key name
     * @param def 默认值 / the default value
     * @return Boolean值或默认值 / the Boolean value or the default
     */
    public static Boolean getBooleanOrDefault(JsonObject jo, String mem, Boolean def) {
        JsonElement je = jo.get(mem);
        if (je == null) return def;
        return je.getAsBoolean();
    }

    /**
     * 从JsonObject获取Float值。
     * <p>
     * Gets a Float value from a JsonObject.
     *
     * @param jo JSON对象 / the JSON object
     * @param mem 成员键名 / the member key name
     * @return Float值，不存在时返回null / the Float value, or null if not present
     */
    public static Float getFloat(JsonObject jo, String mem) {
        return getFloatOrDefault(jo, mem, null);
    }

    /**
     * 从JsonObject获取Float值，不存在时返回默认值。
     * <p>
     * Gets a Float value from a JsonObject, returning a default value if not present.
     *
     * @param jo JSON对象 / the JSON object
     * @param mem 成员键名 / the member key name
     * @param def 默认值 / the default value
     * @return Float值或默认值 / the Float value or the default
     */
    public static Float getFloatOrDefault(JsonObject jo, String mem, Float def) {
        JsonElement je = jo.get(mem);
        if (je == null) return def;
        return je.getAsFloat();
    }

    /**
     * 从JsonObject获取Integer值。
     * <p>
     * Gets an Integer value from a JsonObject.
     *
     * @param jo JSON对象 / the JSON object
     * @param mem 成员键名 / the member key name
     * @return Integer值，不存在时返回null / the Integer value, or null if not present
     */
    public static Integer getInt(JsonObject jo, String mem) {
        return getIntOrDefault(jo, mem, null);
    }

    /**
     * 从JsonObject获取Integer值，不存在时返回默认值。
     * <p>
     * Gets an Integer value from a JsonObject, returning a default value if not present.
     *
     * @param jo JSON对象 / the JSON object
     * @param mem 成员键名 / the member key name
     * @param def 默认值 / the default value
     * @return Integer值或默认值 / the Integer value or the default
     */
    public static Integer getIntOrDefault(JsonObject jo, String mem, Integer def) {
        JsonElement je = jo.get(mem);
        if (je == null) return def;
        return je.getAsInt();
    }

    /**
     * 从JsonObject获取ResourceLocation值。
     * <p>
     * Gets a ResourceLocation value from a JsonObject.
     *
     * @param jo JSON对象 / the JSON object
     * @param mem 成员键名 / the member key name
     * @return 资源位置 / the resource location
     */
    public static ResourceLocation getResourceLocation(JsonObject jo, String mem) {
        return new ResourceLocation(jo.get(mem).getAsString());
    }

    /**
     * 从JsonObject获取String值。
     * <p>
     * Gets a String value from a JsonObject.
     *
     * @param jo JSON对象 / the JSON object
     * @param mem 成员键名 / the member key name
     * @return String值，不存在时返回null / the String value, or null if not present
     */
    public static String getString(JsonObject jo, String mem) {
        return getStringOrDefault(jo, mem, null);
    }

    /**
     * 从JsonObject获取String值，不存在时返回默认值。
     * <p>
     * Gets a String value from a JsonObject, returning a default value if not present.
     *
     * @param jo JSON对象 / the JSON object
     * @param mem 成员键名 / the member key name
     * @param def 默认值 / the default value
     * @return String值或默认值 / the String value or the default
     */
    public static String getStringOrDefault(JsonObject jo, String mem, String def) {
        JsonElement je = jo.get(mem);
        if (je == null) return def;
        return je.getAsString();
    }
}
