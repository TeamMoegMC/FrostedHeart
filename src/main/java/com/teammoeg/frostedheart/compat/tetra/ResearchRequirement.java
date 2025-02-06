package com.teammoeg.frostedheart.compat.tetra;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;

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
