package com.teammoeg.frostedheart.research;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

public enum ResearchLevel {
    DRAWING_DESK("drawing_desk"),
    MAPPING_MACHINE("mapping_machine"),
    MECHANICAL_CALCULATOR("mechanical_calculator"),
    DIFFERENTIAL_ENGINE("differential_engine"),
    COMPUTING_MATRIX("computing_matrix");

    ResourceLocation icon;
    TranslationTextComponent name;

    ResearchLevel(String levelName) {
        icon = FHMain.rl("textures/gui/research/level/" + levelName);
        name = GuiUtils.translateResearchLevel(levelName);
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public TranslationTextComponent getName() {
        return name;
    }
}
