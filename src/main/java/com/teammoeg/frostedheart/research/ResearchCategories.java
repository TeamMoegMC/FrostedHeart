package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

public class ResearchCategories {
    public static Map<ResourceLocation,ResearchCategory> ALL = new HashMap<>();
    public static ResearchCategory RESCUE, LIVING, PRODUCTION, ARS, EXPLORATION;

    public static void add(ResearchCategory c) {
    	ALL.put(c.getId(),c);
    }
    public static void init() {
        RESCUE = new ResearchCategory("rescue");
        LIVING = new ResearchCategory("living");
        PRODUCTION = new ResearchCategory("production");
        ARS = new ResearchCategory("ars");
        EXPLORATION = new ResearchCategory("exploration");

        add(RESCUE);
        add(LIVING);
        add(PRODUCTION);
        add(ARS);
        add(EXPLORATION);
    }
}
