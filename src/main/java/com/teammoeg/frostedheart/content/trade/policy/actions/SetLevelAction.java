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

package com.teammoeg.frostedheart.content.trade.policy.actions;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.content.trade.FHVillagerData;

import net.minecraft.network.PacketBuffer;

public class SetLevelAction extends AbstractAction {
    int value;

    public SetLevelAction(int value) {
        this.value = value;
    }

    public SetLevelAction(JsonObject jo) {
        value = jo.get("level").getAsInt();
    }

    public SetLevelAction(PacketBuffer buffer) {
        value = buffer.readVarInt();
    }

    @Override
    public void deal(FHVillagerData data, int num) {
        data.setTradelevel(value);
    }

    @Override
    public JsonObject serialize() {
        JsonObject jo = super.serialize().getAsJsonObject();
        jo.addProperty("level", value);
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        super.write(buffer);
        buffer.writeVarInt(value);
    }


}
