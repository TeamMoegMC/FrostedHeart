package com.teammoeg.frostedheart.research.effects;

import com.teammoeg.frostedheart.client.util.GuiUtils;

import java.util.ArrayList;

/**
 * Allows forming multiblock
 */
public class EffectBuilding extends Effect {

    public EffectBuilding() {
    }

    @Override
    public void init() {
        name = GuiUtils.translateGui("effect.building");
        tooltip = new ArrayList<>();
        tooltip.add(GuiUtils.translateTooltip("effect.building.1"));
    }

    @Override
    public void grant() {

    }

    @Override
    public void revoke() {

    }
}
