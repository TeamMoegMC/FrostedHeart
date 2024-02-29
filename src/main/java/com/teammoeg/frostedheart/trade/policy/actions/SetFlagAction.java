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

package com.teammoeg.frostedheart.trade.policy.actions;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.FHVillagerData;
import com.teammoeg.frostedheart.trade.policy.Actions;
import com.teammoeg.frostedheart.trade.policy.PolicyAction;

import net.minecraft.network.PacketBuffer;

public class SetFlagAction implements PolicyAction {
    String name;

    public SetFlagAction(JsonObject jo) {
        name = jo.get("name").getAsString();
    }

    public SetFlagAction(PacketBuffer buffer) {
        name = buffer.readString();
    }

    public SetFlagAction(String name) {
        this.name = name;
    }

    @Override
    public void deal(FHVillagerData data, int num) {
        data.flags.computeIfAbsent(name, k -> 1);
    }

    @Override
    public JsonObject serialize() {
        JsonObject jo = new JsonObject();
        jo.addProperty("name", name);
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeString(name);
    }

}
