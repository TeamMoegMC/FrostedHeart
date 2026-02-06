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

package com.teammoeg.frostedresearch.number;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.teammoeg.chorda.io.registry.JsonSerializerRegistry;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Function;

public class Numbers {
    static JsonSerializerRegistry<IResearchNumber> registry = new JsonSerializerRegistry<>();

    static {
        registry.register(ConstResearchNumber.class, "value", ConstResearchNumber::valueOf, Numbers::nopserializer, ConstResearchNumber::valueOf);
        registry.register(ExpResearchNumber.class, "exp", ExpResearchNumber::new, Numbers::nopserializer, ExpResearchNumber::new);
    }

    private Numbers() {
    }

    private static JsonObject nopserializer(IResearchNumber obj) {
        JsonObject jo = new JsonObject();
        jo.add("exp", obj.serialize());
        return jo;
    }

    public static IResearchNumber deserialize(JsonElement je) {
        if (je.isJsonPrimitive()) {
            JsonPrimitive jp = (JsonPrimitive) je;
            if (jp.isString())
                return new ExpResearchNumber(jp.getAsString());
            else if (jp.isNumber())
                return new ConstResearchNumber(jp.getAsNumber());
        }
        return registry.read(je.getAsJsonObject());
    }

    public static IResearchNumber deserialize(FriendlyByteBuf data) {
        return registry.read(data);
    }

    public static void registerNumberType(Class<? extends IResearchNumber> cls, String type, Function<JsonObject, IResearchNumber> json, Function<IResearchNumber, JsonObject> obj, Function<FriendlyByteBuf, IResearchNumber> packet) {
        registry.register(cls, type, json, obj, packet);
    }

    public static void write(IResearchNumber e, FriendlyByteBuf pb) {
        registry.write(pb, e);
    }
}
