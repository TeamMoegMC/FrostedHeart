/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.town.citizen.client;

import net.minecraft.resources.ResourceLocation;

/** Shared deterministic selection of Minecraft's built-in wide player skins. */
public final class CitizenSkins {

	private static final ResourceLocation[] WIDE_SKINS = {
			new ResourceLocation("textures/entity/player/wide/makena.png"),
			new ResourceLocation("textures/entity/player/wide/efe.png"),
			new ResourceLocation("textures/entity/player/wide/noor.png"),
			new ResourceLocation("textures/entity/player/wide/kai.png"),
			new ResourceLocation("textures/entity/player/wide/ari.png"),
			new ResourceLocation("textures/entity/player/wide/zuri.png"),
			new ResourceLocation("textures/entity/player/wide/sunny.png")
	};

	private CitizenSkins() {
	}

	public static int indexFor(int citizenId) {
		int mixed = citizenId;
		mixed = (mixed ^ (mixed >>> 16)) * 0x7FEB352D;
		mixed = (mixed ^ (mixed >>> 15)) * 0x846CA68B;
		mixed ^= mixed >>> 16;
		return Math.floorMod(mixed, WIDE_SKINS.length);
	}

	public static ResourceLocation textureFor(int citizenId) {
		return WIDE_SKINS[indexFor(citizenId)];
	}

	static ResourceLocation textureAt(int index) {
		return WIDE_SKINS[index];
	}

	static int count() {
		return WIDE_SKINS.length;
	}
}
