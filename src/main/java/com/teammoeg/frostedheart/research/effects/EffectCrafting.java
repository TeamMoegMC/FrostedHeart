package com.teammoeg.frostedheart.research.effects;

import com.teammoeg.frostedheart.client.util.GuiUtils;

import java.util.ArrayList;

public class EffectCrafting extends Effect {

    public EffectCrafting() {
    }

    @Override
    public void init() {
        name = GuiUtils.translateGui("effect.crafting");
        tooltip = new ArrayList<>();
        tooltip.add(GuiUtils.translateTooltip("effect.crafting.1"));
    }

    @Override
    public void grant() {

    }

    @Override
    public void revoke() {

    }
}
