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

package com.teammoeg.frostedresearch.compat.tetra;

import com.google.gson.JsonObject;
import com.teammoeg.frostedresearch.api.ResearchDataAPI;

import se.mickelus.tetra.module.schematic.CraftingContext;
import se.mickelus.tetra.module.schematic.requirement.CraftingRequirement;

public class ResearchRequirement implements CraftingRequirement {
	String research;
	public ResearchRequirement(JsonObject from) {
		research=from.get("research").getAsString();
	}
	
	@Override
	public boolean test(CraftingContext arg0) {
		return ResearchDataAPI.isResearchComplete(arg0.player,research);
	}

}
