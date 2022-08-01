package com.teammoeg.frostedheart.research;

import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ResearchCategories {
    public static Map<ResourceLocation, ResearchCategory> ALL = new HashMap<>();

    private ResearchCategories() {
    }

    static {
        ResearchCategory.values();
    }

    public static void init() {
    }

}
