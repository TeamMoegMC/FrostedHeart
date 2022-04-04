package com.teammoeg.frostedheart.research.effects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;

import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;

public class EffectCrafting extends Effect{

    public EffectCrafting() {
        name = GuiUtils.translateGui("effect.crafting");
        tooltip = new ArrayList<>();
        tooltip.add(GuiUtils.translateTooltip("effect.crafting.1"));
    }
    public EffectCrafting(JsonObject jo) {}
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
