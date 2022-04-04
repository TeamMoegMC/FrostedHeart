package com.teammoeg.frostedheart.research.effects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;

import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;

/**
 * Allows the research team to use certain machines
 */
public class EffectUse extends Effect {

    public EffectUse() {
        name = GuiUtils.translateGui("effect.use");
        tooltip = new ArrayList<>();
        tooltip.add(GuiUtils.translateTooltip("effect.use.1"));
    }
    public EffectUse(JsonObject jo) {}
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
