package com.teammoeg.frostedheart.research;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

public class ResearchCategories {
	public static Map<ResourceLocation,ResearchCategory> ALL = new HashMap<>();
	private ResearchCategories() {
	}
	static {
		ResearchCategory.values();
	}

}
