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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.policy.snapshot.BuyData;
import com.teammoeg.frostedheart.trade.policy.snapshot.PolicySnapshot;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;

import java.util.Map;

public class DemandData extends BaseData {
    public Ingredient item;

    public DemandData(String id, int maxstore, float recover, int price, Ingredient item) {
        super(id, maxstore, recover, price);
        this.item = item;
    }

    public DemandData(JsonObject jo) {
        super(jo);
        item = Ingredient.deserialize(jo.get("demand"));
    }

    public DemandData(PacketBuffer pb) {
        super(pb);
        item = Ingredient.read(pb);
    }

    @Override
    public void fetch(PolicySnapshot ps, Map<String, Float> data) {
        int num = (int) (float) data.getOrDefault(getId(), 0f);
        if (!hideStockout || num > 0)
            ps.registerBuy(new BuyData(getId(), num, this));
    }

    @Override
    public JsonElement serialize() {
        JsonObject jo = super.serialize().getAsJsonObject();
        jo.add("demand", item.serialize());
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeVarInt(2);
        super.write(buffer);
        item.write(buffer);
    }

    @Override
    public String getType() {
        return "b";
    }

}
