package com.teammoeg.frostedheart.research.effects;

import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.base.multiblock.FHBaseMultiblock;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;

/**
 * Allows forming multiblock
 */
public class EffectBuilding extends Effect {

    IETemplateMultiblock multiblock;

    public EffectBuilding(IETemplateMultiblock s) {
        multiblock = s;
    }

    @Override
    public void init() {
        name = GuiUtils.translateGui("effect.building");
        icon = new ItemStack(FHContent.FHItems.copper_core_spade);
        tooltip = new ArrayList<>();
        tooltip.add(GuiUtils.translateTooltip(multiblock.getUniqueName().toString()));
    }

    @Override
    public void grant() {

    }

    @Override
    public void revoke() {

    }
}
