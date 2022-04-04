package com.teammoeg.frostedheart.research.effects;

import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.base.multiblock.FHBaseMultiblock;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;

/**
 * Allows forming multiblock
 */
public class EffectBuilding extends Effect {

    IETemplateMultiblock multiblock;

    public EffectBuilding(IETemplateMultiblock s) {
        multiblock = s;
        name = GuiUtils.translateGui("effect.building");
        icon = new ItemStack(FHContent.FHItems.copper_core_spade);
        tooltip = new ArrayList<>();
        tooltip.add(GuiUtils.translateTooltip(multiblock.getUniqueName().toString()));
    }
    public EffectBuilding(JsonObject jo) {}
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
