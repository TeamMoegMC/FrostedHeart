package com.teammoeg.frostedheart.research.effects;

import com.teammoeg.frostedheart.client.util.GuiUtils;

import java.util.ArrayList;

/**
 * Allows the research team to use certain machines
 */
public class EffectUse extends Effect {

    public EffectUse() {
    }

    @Override
    public void init() {
        name = GuiUtils.translateGui("effect.use");
        tooltip = new ArrayList<>();
        tooltip.add(GuiUtils.translateTooltip("effect.use.1"));
    }

    @Override
    public void grant() {

    }

    @Override
    public void revoke() {

    }
}
