package com.teammoeg.frostedheart.research.effects;

import com.teammoeg.frostedheart.client.util.GuiUtils;

import java.util.ArrayList;

/**
 * Effect on numerical stats of the team's machines or abilities
 */
public class EffectStats extends Effect {

    public EffectStats() {
        name = GuiUtils.translateGui("effect.stats");
        tooltip = new ArrayList<>();
        tooltip.add(GuiUtils.translateTooltip("effect.stats.1"));
    }

    @Override
    public void init() {

    }

    @Override
    public void grant() {

    }

    @Override
    public void revoke() {

    }
}
