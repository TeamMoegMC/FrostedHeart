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

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.content.trade.FHVillagerData;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.PolicySnapshot;
import com.teammoeg.frostedheart.util.io.SerializeUtil;
import com.teammoeg.frostedheart.util.io.Writeable;

import net.minecraft.network.PacketBuffer;

public abstract class PolicyGroup implements Writeable {
    List<PolicyCondition> conditions;

    public static PolicyGroup read(JsonObject jo) {
        if (jo.has("parent"))
            return new ExtendPolicyGroup(jo);
        return new BasicPolicyGroup(jo);
    }

    public static PolicyGroup read(PacketBuffer pb) {
        if (pb.readBoolean())
            return new ExtendPolicyGroup(pb);
        return new BasicPolicyGroup(pb);
    }

    public PolicyGroup(JsonObject jo) {
        super();
        if (jo.has("conditions"))
            conditions = SerializeUtil.parseJsonList(jo.get("conditions"), Conditions::deserialize);
        else
            conditions = ImmutableList.of();
    }

    public PolicyGroup(List<PolicyCondition> conditions) {
        super();
        this.conditions = conditions;
    }

    public PolicyGroup(PacketBuffer pb) {
        super();
        conditions = SerializeUtil.readList(pb, Conditions::deserialize);
    }

    public void CollectPolicies(PolicySnapshot policy, FHVillagerData ve) {
        if (conditions.stream().allMatch(t -> t.test(ve)))
            CollectPoliciesNoCheck(policy, ve);

    }

    public abstract void CollectPoliciesNoCheck(PolicySnapshot policy, FHVillagerData ve);

    @Override
    public JsonElement serialize() {
        JsonObject jo = new JsonObject();
        jo.add("conditions", SerializeUtil.toJsonList(conditions, Conditions::serialize));
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        SerializeUtil.writeList(buffer, conditions, Conditions::write);
    }
}
