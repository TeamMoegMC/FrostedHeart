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

package com.teammoeg.frostedheart.trade.policy;

import java.util.Map;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.policy.snapshot.PolicySnapshot;

import net.minecraft.network.PacketBuffer;

public class NopData extends BaseData {

    public NopData(JsonObject jo) {
        super(jo);
    }

    public NopData(PacketBuffer pb) {
        super(pb);
    }

    public NopData(String id, int maxstore, float recover, int price, PolicyAction... restock) {
        super(id, maxstore, recover, price, restock);
    }

    @Override
    public void fetch(PolicySnapshot shot, Map<String, Float> data) {
    }

    @Override
    public String getType() {
        return "n";
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeVarInt(3);
        super.write(buffer);
    }

}
