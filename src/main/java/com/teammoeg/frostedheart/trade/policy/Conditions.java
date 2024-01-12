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
import com.teammoeg.frostedheart.trade.policy.conditions.*;
import net.minecraft.network.PacketBuffer;

import java.util.function.Function;

public class Conditions {
    private static JsonSerializerRegistry<PolicyCondition> registry = new JsonSerializerRegistry<>();

    static {
        registerType(LevelCondition.class, "level", LevelCondition::new, LevelCondition::new);
        registerType(FlagValueCondition.class, "value", FlagValueCondition::new, FlagValueCondition::new);
        registerType(GreaterFlagCondition.class, "greater", GreaterFlagCondition::new, GreaterFlagCondition::new);
        registerType(NotCondition.class, "not", NotCondition::new, NotCondition::new);
        registerType(TotalTradeCondition.class, "total", TotalTradeCondition::new, TotalTradeCondition::new);
        registerType(WithFlagCondition.class, "has", WithFlagCondition::new, WithFlagCondition::new);
    }

    public static PolicyCondition deserialize(JsonObject jo) {
        return registry.deserialize(jo);
    }

    public static PolicyCondition deserialize(PacketBuffer data) {
        return registry.read(data);
    }

    public static void registerType(Class<? extends PolicyCondition> cls, String type, Function<JsonObject, PolicyCondition> json, Function<PacketBuffer, PolicyCondition> packet) {
        registry.register(cls, type, json, packet);
    }

    public static void writeId(PolicyCondition e, PacketBuffer pb) {
        registry.writeId(pb, e);
    }

    public static void writeType(PolicyCondition e, JsonObject jo) {
        registry.writeType(jo, e);
    }

    private Conditions() {
    }
}
