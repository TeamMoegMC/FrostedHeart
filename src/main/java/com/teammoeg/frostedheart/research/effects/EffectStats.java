package com.teammoeg.frostedheart.research.effects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;

import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;

/**
 * Effect on numerical stats of the team's machines or abilities
 */
public class EffectStats extends Effect {

    public EffectStats() {
        name = GuiUtils.translateGui("effect.use");
        tooltip = new ArrayList<>();
        tooltip.add(GuiUtils.translateTooltip("effect.use.1"));
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
	public JsonElement serialize() {
		return null;
	}

	@Override
	public void write(PacketBuffer buffer) {
	}
}
