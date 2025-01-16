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

package com.teammoeg.chorda.util.io.registry;

import java.util.function.Function;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.util.io.Writeable;

import net.minecraft.network.FriendlyByteBuf;

public class JsonSerializerRegistry<U extends Writeable> extends PacketBufferSerializerRegistry<U, Object, JsonObject> {



    public JsonSerializerRegistry() {
        super();
    }

	@Override
	protected void writeType(Pair<Integer, String> type, JsonObject obj) {
		obj.addProperty("type", type.getSecond());
	}

	@Override
	protected String readType(JsonObject obj) {
		return obj.get("type").getAsString();
	}

	public void register(Class<? extends U> cls, String type, Function<JsonObject, U> json, Function<U, JsonObject> obj, Function<FriendlyByteBuf, U> packet) {
		super.register(cls, type, json, (t,c)->obj.apply(t), packet);
	}

	@Override
	public U read(JsonObject fromObj) {
		return super.read(fromObj);
	}

	public JsonObject write(U fromObj) {
		return super.write(fromObj, null);
	}


}
