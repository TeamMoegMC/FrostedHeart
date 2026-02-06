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

package com.teammoeg.frostedheart.content.trade.policy.conditions;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.content.trade.FHVillagerData;

import net.minecraft.network.FriendlyByteBuf;

public class GreaterFlagCondition extends WithFlagCondition {
	int value;
    public GreaterFlagCondition(JsonObject jo) {
        super(jo);
        value=jo.get("value").getAsInt();
    }

    public GreaterFlagCondition(FriendlyByteBuf buffer) {
        super(buffer);
        value=buffer.readVarInt();
    }

    public GreaterFlagCondition(String name, int val) {
        super(name);
        this.value=val;
    }

    @Override
	public JsonObject serialize() {
    	JsonObject jo= super.serialize();
    	jo.addProperty("value", value);
    	return jo;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		super.write(buffer);
		buffer.writeVarInt(value);
	}

	@Override
    public int test(FHVillagerData ve) {
        return ve.flags.getOrDefault(name, 0) - value;
    }
}
