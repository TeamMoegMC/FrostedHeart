package com.teammoeg.frostedheart.research.effects;

import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.base.multiblock.FHBaseMultiblock;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;

/**
 * Allows forming multiblock
 */
public class EffectBuilding extends Effect {

    IETemplateMultiblock multiblock;
    Block block;


    public EffectBuilding(IETemplateMultiblock s, Block b) {
        multiblock = s;
        block = b;
        name = GuiUtils.translateGui("effect.building");
        icon = new ItemStack(FHContent.FHItems.copper_core_spade);
        tooltip = new ArrayList<>();
        tooltip.add(GuiUtils.translateTooltip(multiblock.getUniqueName().toString()));
    }

    public IETemplateMultiblock getMultiblock() {
        return multiblock;
    }

    public Block getBlock() {
        return block;
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
