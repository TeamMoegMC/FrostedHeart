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

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.frostedheart.content.trade.FHVillagerData;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.PolicySnapshot;

import net.minecraft.network.FriendlyByteBuf;

public class BasicPolicyGroup extends PolicyGroup {
    List<BaseData> bdata;

    public BasicPolicyGroup(JsonObject jo) {
        super(jo);
        bdata = SerializeUtil.parseJsonList(jo.get("data"), BaseData::read);
    }

    public BasicPolicyGroup(List<PolicyCondition> conditions, List<BaseData> bdata) {
        super(conditions);
        this.bdata = bdata;
    }

    public BasicPolicyGroup(FriendlyByteBuf pb) {
        super(pb);
        bdata = SerializeUtil.readList(pb, BaseData::read);
    }

    @Override
    public void CollectPoliciesNoCheck(PolicySnapshot policy, FHVillagerData ve) {
        bdata.forEach(policy::register);
    }

    @Override
    public JsonElement serialize() {
        JsonObject jo = super.serialize().getAsJsonObject();
        jo.add("data", SerializeUtil.toJsonList(bdata, BaseData::serialize));
        return jo;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(false);
        super.write(buffer);
        SerializeUtil.writeList(buffer, bdata, BaseData::write);
    }
}
