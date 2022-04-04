package com.teammoeg.frostedheart.research.effects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

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
    public EffectStats(JsonObject jo) {}
    @Override
    public void init() {

    }

    @Override
    public void grant() {

    }

    @Override
    public void revoke() {

    }

	@Override
	public ResourceLocation getId() {
		return null;
	}

}
