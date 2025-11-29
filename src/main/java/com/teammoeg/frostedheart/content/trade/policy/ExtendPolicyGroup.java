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
import com.teammoeg.frostedheart.content.trade.FHVillagerData;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.PolicySnapshot;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class ExtendPolicyGroup extends PolicyGroup {
    ResourceLocation ref;

    public ExtendPolicyGroup(JsonObject jo) {
        super(jo);
        ref = new ResourceLocation(jo.get("parent").getAsString());
    }

    public ExtendPolicyGroup(List<PolicyCondition> conditions, ResourceLocation ref) {
        super(conditions);
        this.ref = ref;
    }

    public ExtendPolicyGroup(FriendlyByteBuf pb) {
        super(pb);
        ref = pb.readResourceLocation();
    }

    @Override
    public void CollectPoliciesNoCheck(PolicySnapshot policy, FHVillagerData ve,int num) {
        TradePolicy.policies.get(ref).CollectPolicies(policy, ve,num);
    }

    @Override
    public JsonElement serialize() {
        JsonObject jo = super.serialize().getAsJsonObject();
        jo.addProperty("parent", ref.toString());
        return jo;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(true);
        super.write(buffer);
        buffer.writeResourceLocation(ref);
    }
}
