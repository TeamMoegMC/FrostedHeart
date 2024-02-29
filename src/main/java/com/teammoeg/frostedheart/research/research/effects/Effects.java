/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.research.research.effects;

import java.util.function.Function;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.util.io.JsonSerializerRegistry;

import net.minecraft.network.PacketBuffer;

public class Effects {
    private static JsonSerializerRegistry<Effect> registry = new JsonSerializerRegistry<>();

    static {
    	registerEffectType(EffectBuilding.class, "multiblock", EffectBuilding::new, EffectBuilding::new);
        registerEffectType(EffectCrafting.class, "recipe", EffectCrafting::new, EffectCrafting::new);
        registerEffectType(EffectItemReward.class, "item", EffectItemReward::new, EffectItemReward::new);
        registerEffectType(EffectStats.class, "stats", EffectStats::new, EffectStats::new);
        registerEffectType(EffectUse.class, "use", EffectUse::new, EffectUse::new);
        registerEffectType(EffectShowCategory.class, "category", EffectShowCategory::new, EffectShowCategory::new);
        registerEffectType(EffectCommand.class, "command", EffectCommand::new, EffectCommand::new);
        registerEffectType(EffectExperience.class, "experience", EffectExperience::new, EffectExperience::new);
    }

    public static Effect deserialize(JsonObject jo) {
        return registry.read(jo);
    }

    public static JsonObject write(Effect fromObj) {
		return registry.write(fromObj);
	}

	public static Effect read(PacketBuffer data) {
        return registry.read(data);
    }

    public static void registerEffectType(Class<? extends Effect> cls, String type, Function<JsonObject, Effect> json, Function<PacketBuffer, Effect> packet) {
        registry.register(cls, type, json, Effect::serialize, packet);
    }

    public static void write(Effect e, PacketBuffer pb) {
        registry.write(pb, e);
    }

	public static Effect read(JsonObject jo) {
        return registry.read(jo);
    }

    private Effects() {
    }
}
