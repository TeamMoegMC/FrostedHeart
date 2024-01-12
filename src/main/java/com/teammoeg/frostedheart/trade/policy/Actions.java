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

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.research.JsonSerializerRegistry;
import com.teammoeg.frostedheart.trade.policy.actions.AddFlagValueAction;
import com.teammoeg.frostedheart.trade.policy.actions.SetFlagAction;
import com.teammoeg.frostedheart.trade.policy.actions.SetFlagValueAction;
import net.minecraft.network.PacketBuffer;

import java.util.function.Function;

public class Actions {
    private static JsonSerializerRegistry<PolicyAction> registry = new JsonSerializerRegistry<>();

    static {
        registerType(AddFlagValueAction.class, "add", AddFlagValueAction::new, AddFlagValueAction::new);
        registerType(SetFlagValueAction.class, "set", SetFlagValueAction::new, SetFlagValueAction::new);
        registerType(SetFlagAction.class, "flag", SetFlagAction::new, SetFlagAction::new);
    }

    public static void registerType(Class<? extends PolicyAction> cls, String type, Function<JsonObject, PolicyAction> json, Function<PacketBuffer, PolicyAction> packet) {
        registry.register(cls, type, json, packet);
    }

    private Actions() {
    }

    public static void writeId(PolicyAction e, PacketBuffer pb) {
        registry.writeId(pb, e);
    }

    public static PolicyAction deserialize(JsonObject jo) {
        return registry.deserialize(jo);
    }

    public static PolicyAction deserialize(PacketBuffer data) {
        return registry.read(data);
    }

    public static void writeType(PolicyAction e, JsonObject jo) {
        registry.writeType(jo, e);
    }
}
