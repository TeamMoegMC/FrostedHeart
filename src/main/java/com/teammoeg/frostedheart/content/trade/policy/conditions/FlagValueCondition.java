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

package com.teammoeg.frostedheart.content.trade.policy.conditions;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.content.trade.FHVillagerData;

import net.minecraft.network.FriendlyByteBuf;

public class FlagValueCondition extends WithFlagCondition {

    public FlagValueCondition(JsonObject jo) {
        super(jo);
    }

    public FlagValueCondition(FriendlyByteBuf buffer) {
        super(buffer);
    }

    public FlagValueCondition(String name) {
        super(name);
    }

    @Override
    public JsonObject serialize() {
        JsonObject jo = super.serialize();
        return jo;
    }

    @Override
    public int test(FHVillagerData ve) {
        return ve.flags.getOrDefault(name, 0);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        super.write(buffer);
    }

}
