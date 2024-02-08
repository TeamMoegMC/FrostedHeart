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

import net.minecraft.network.PacketBuffer;

public class SetFlagValueAction extends SetFlagAction {
    int value;

    public SetFlagValueAction(JsonObject jo) {
        super(jo);
        value = jo.get("value").getAsInt();
    }

    public SetFlagValueAction(PacketBuffer buffer) {
        super(buffer);
        value = buffer.readVarInt();
    }

    public SetFlagValueAction(String name, int value) {
        super(name);
        this.value = value;
    }

    @Override
    public void deal(FHVillagerData data, int num) {
        if (value != 0)
            data.flags.put(name, value);
        else
            data.flags.remove(name);
    }

    @Override
    public JsonObject serialize() {
        JsonObject jo = super.serialize().getAsJsonObject();
        jo.addProperty("value", value);
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        super.write(buffer);
        buffer.writeVarInt(value);
    }


}
