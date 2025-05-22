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

package com.teammoeg.frostedheart.content.trade.policy;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.content.trade.FHVillagerData;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.BuyData;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.PolicySnapshot;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.network.FriendlyByteBuf;

public class DemandData extends BaseData {
    public Ingredient item;

    public DemandData(JsonObject jo) {
        super(jo);
        item = Ingredient.fromJson(jo.get("demand"));
    }

    public DemandData(FriendlyByteBuf pb) {
        super(pb);
        item = Ingredient.fromNetwork(pb);
    }

    public DemandData(String id, int maxstore, float recover, int price, Ingredient item) {
        super(id, maxstore, recover, price);
        this.item = item;
    }

    @Override
    public void fetch(PolicySnapshot ps,FHVillagerData vd, Map<String, Float> data) {
        int num = Math.min((int) (float) data.getOrDefault(getId(), 0f), this.sellconditions.stream().mapToInt(c->c.test(vd)).reduce(Integer.MAX_VALUE,Math::min));
        if (!hideStockout || num > 0)
            ps.registerBuy(new BuyData(vd,getId(), num, this));
    }

    @Override
    public String getType() {
        return "b";
    }

    @Override
    public JsonElement serialize() {
        JsonObject jo = super.serialize().getAsJsonObject();
        jo.add("demand", item.toJson());
        return jo;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(2);
        super.write(buffer);
        item.toNetwork(buffer);
    }

}
