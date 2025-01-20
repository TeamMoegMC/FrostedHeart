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

import java.util.function.Function;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.content.trade.policy.actions.AddFlagValueAction;
import com.teammoeg.frostedheart.content.trade.policy.actions.SetFlagAction;
import com.teammoeg.frostedheart.content.trade.policy.actions.SetFlagValueAction;
import com.teammoeg.chorda.util.io.registry.JsonSerializerRegistry;

import net.minecraft.network.FriendlyByteBuf;

public class Actions {
    private static JsonSerializerRegistry<PolicyAction> registry = new JsonSerializerRegistry<>();

    static {
        registerType(AddFlagValueAction.class, "add", AddFlagValueAction::new, AddFlagValueAction::new);
        registerType(SetFlagValueAction.class, "set", SetFlagValueAction::new, SetFlagValueAction::new);
        registerType(SetFlagAction.class, "flag", SetFlagAction::new, SetFlagAction::new);
    }

    public static PolicyAction deserialize(JsonObject jo) {
        return registry.read(jo);
    }
    public static JsonObject serialize(PolicyAction jo) {
        return registry.write(jo);
    }

    public static PolicyAction deserialize(FriendlyByteBuf data) {
        return registry.read(data);
    }

    public static void registerType(Class<? extends PolicyAction> cls, String type, Function<JsonObject, PolicyAction> json, Function<FriendlyByteBuf, PolicyAction> packet) {
        registry.register(cls, type, json,PolicyAction::serialize, packet);
    }

    public static void write(PolicyAction e, FriendlyByteBuf pb) {
        registry.write(pb, e);
    }


    private Actions() {
    }
}
