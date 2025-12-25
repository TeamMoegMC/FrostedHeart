package com.teammoeg.frostedresearch.compat.tetra;

import com.teammoeg.frostedheart.FHMain;

import se.mickelus.tetra.module.schematic.requirement.CraftingRequirementDeserializer;

public class TetraCompat {

	private TetraCompat() {
	}
    public static void init() {
    	CraftingRequirementDeserializer.registerSupplier(FHMain.rl("research").toString(), ResearchRequirement::new);
    }
}
