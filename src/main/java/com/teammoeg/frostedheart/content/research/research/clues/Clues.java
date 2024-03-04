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

package com.teammoeg.frostedheart.content.research.research.clues;

import java.util.function.Function;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.util.io.JsonSerializerRegistry;

import net.minecraft.network.PacketBuffer;

public class Clues {
    private static JsonSerializerRegistry<Clue> registry = new JsonSerializerRegistry<>();

    static {
        register(CustomClue.class, "custom", CustomClue::new, CustomClue::new);
        register(AdvancementClue.class, "advancement", AdvancementClue::new,AdvancementClue::new);
        register(ItemClue.class, "item", ItemClue::new,ItemClue::new);
        register(KillClue.class, "kill", KillClue::new,KillClue::new);
        register(MinigameClue.class, "game", MinigameClue::new,MinigameClue::new);
    }

    public static void write(PacketBuffer pb,Clue fromObj) {
		registry.write(pb, fromObj);
	}
    public static JsonObject write(Clue fromObj) {
		return registry.write(fromObj);
	}

	public static Clue read(JsonObject jo) {
        return registry.read(jo);
    }

    public static Clue read(PacketBuffer pb) {
        return registry.read(pb);
    }

    public static void register(Class<? extends Clue> cls, String id, Function<JsonObject, Clue> j, Function<PacketBuffer, Clue> p) {
        registry.register(cls, id, j,Clue::serialize, p);
    }

    private Clues() {
    }
}
