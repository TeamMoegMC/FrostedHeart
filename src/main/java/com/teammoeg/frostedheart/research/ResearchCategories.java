package com.teammoeg.frostedheart.research;

import java.util.ArrayList;

public class ResearchCategories {
    public static ArrayList<ResearchCategory> ALL = new ArrayList<>();
    public static ResearchCategory RESCUE, LIVING, PRODUCTION, ARS, EXPLORATION;

    public static void init() {
        RESCUE = new ResearchCategory("rescue");
        LIVING = new ResearchCategory("living");
        PRODUCTION = new ResearchCategory("production");
        ARS = new ResearchCategory("ars");
        EXPLORATION = new ResearchCategory("exploration");

        ALL.add(RESCUE);
        ALL.add(LIVING);
        ALL.add(PRODUCTION);
        ALL.add(ARS);
        ALL.add(EXPLORATION);
    }
}
