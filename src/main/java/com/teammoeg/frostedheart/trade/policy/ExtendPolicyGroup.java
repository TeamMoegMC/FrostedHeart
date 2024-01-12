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
import com.teammoeg.frostedheart.trade.FHVillagerData;
import com.teammoeg.frostedheart.trade.policy.snapshot.PolicySnapshot;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import java.util.List;

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

    public ExtendPolicyGroup(PacketBuffer pb) {
        super(pb);
        ref = pb.readResourceLocation();
    }

    @Override
    public void CollectPoliciesNoCheck(PolicySnapshot policy, FHVillagerData ve) {
        TradePolicy.policies.get(ref).CollectPolicies(policy, ve);
    }

    @Override
    public JsonElement serialize() {
        JsonObject jo = super.serialize().getAsJsonObject();
        jo.addProperty("parent", ref.toString());
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeBoolean(true);
        super.write(buffer);
        buffer.writeResourceLocation(ref);
    }
}
