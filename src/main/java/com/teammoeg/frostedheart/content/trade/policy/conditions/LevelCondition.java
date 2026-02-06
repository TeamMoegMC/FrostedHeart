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
import com.teammoeg.frostedheart.content.trade.policy.PolicyCondition;

import net.minecraft.network.FriendlyByteBuf;

public class LevelCondition implements PolicyCondition {
    int level;

    public LevelCondition(int level) {
        super();
        this.level = level;
    }

    public LevelCondition(JsonObject jo) {
        this(jo.get("level").getAsInt());
    }

    public LevelCondition(FriendlyByteBuf buffer) {
        this(buffer.readVarInt());
    }

    @Override
    public JsonObject serialize() {
        JsonObject jo = new JsonObject();
        jo.addProperty("level", level);
        return jo;
    }

    @Override
    public int test(FHVillagerData ve) {
        return ve.getTradeLevel() >= level?Integer.MAX_VALUE:0;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(level);
    }

}
