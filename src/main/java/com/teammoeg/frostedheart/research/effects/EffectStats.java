package com.teammoeg.frostedheart.research.effects;

import com.teammoeg.frostedheart.client.util.GuiUtils;

import java.util.ArrayList;

/**
 * Effect on numerical stats of the team's machines or abilities
 */
public class EffectStats extends Effect {

    String upgradeInfo;

    public EffectStats(String info) {
        name = GuiUtils.translateGui("effect.stats");
        upgradeInfo = info;
    }

    public String getUpgradeInfo() {
        return upgradeInfo;
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
