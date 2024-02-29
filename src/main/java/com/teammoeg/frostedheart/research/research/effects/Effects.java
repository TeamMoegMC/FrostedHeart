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
import com.teammoeg.frostedheart.research.research.clues.Clue;
import com.teammoeg.frostedheart.util.io.JsonSerializerRegistry;

import net.minecraft.network.PacketBuffer;

public class Effects {
    private static JsonSerializerRegistry<Effect> registry = new JsonSerializerRegistry<>();

    static {
        registry.register(EffectBuilding.class, "multiblock", EffectBuilding::new,Effect::serialize, EffectBuilding::new);
        registry.register(EffectCrafting.class, "recipe", EffectCrafting::new,Effect::serialize, EffectCrafting::new);
        registry.register(EffectItemReward.class, "item", EffectItemReward::new,Effect::serialize, EffectItemReward::new);
        registry.register(EffectStats.class, "stats", EffectStats::new,Effect::serialize, EffectStats::new);
        registry.register(EffectUse.class, "use", EffectUse::new,Effect::serialize, EffectUse::new);
        registry.register(EffectShowCategory.class, "category", EffectShowCategory::new,Effect::serialize, EffectShowCategory::new);
        registry.register(EffectCommand.class, "command", EffectCommand::new,Effect::serialize, EffectCommand::new);
        registry.register(EffectExperience.class, "experience", EffectExperience::new,Effect::serialize, EffectExperience::new);
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

    public static void registerEffectType(Class<? extends Effect> cls, String type, Function<JsonObject, Effect> json, Function<Effect,JsonObject> o, Function<PacketBuffer, Effect> packet) {
        registry.register(cls, type, json, o, packet);
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
