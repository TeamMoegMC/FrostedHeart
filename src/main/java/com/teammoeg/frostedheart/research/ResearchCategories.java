package com.teammoeg.frostedheart.research;

import java.util.ArrayList;

public class ResearchCategories {
    public static ArrayList<ResearchCategory> ALL = new ArrayList<>();
    public static ResearchCategory HEATING, LIVING, RESOURCE, INDUSTRY, EXPLORATION;

    public static void init() {
        HEATING = new ResearchCategory("heating");
        LIVING = new ResearchCategory("living");
        RESOURCE = new ResearchCategory("resource");
        INDUSTRY = new ResearchCategory("industry");
        EXPLORATION = new ResearchCategory("exploration");

        ALL.add(HEATING);
        ALL.add(LIVING);
        ALL.add(RESOURCE);
        ALL.add(INDUSTRY);
        ALL.add(EXPLORATION);
    }
}
