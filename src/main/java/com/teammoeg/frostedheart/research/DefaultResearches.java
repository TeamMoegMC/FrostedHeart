package com.teammoeg.frostedheart.research;

import com.teammoeg.frostedheart.FHMain;

public class DefaultResearches {
    public static ResearchCategory HEATING;

    public static void init() {
        HEATING = new ResearchCategory(FHMain.rl("heating"),
                FHMain.rl("textures/gui/research/category/heating.png"),
                FHMain.rl("textures/gui/research/category/heating.png"));
    }
}
