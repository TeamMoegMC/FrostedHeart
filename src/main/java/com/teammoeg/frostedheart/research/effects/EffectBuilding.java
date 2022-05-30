package com.teammoeg.frostedheart.research.effects;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IEMultiblocks;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;

/**
 * Allows forming multiblock
 */
public class EffectBuilding extends Effect {

    IETemplateMultiblock multiblock;
    Block block;


    public EffectBuilding(IETemplateMultiblock s, Block b) {
    	super(GuiUtils.translateGui("effect.building"),new ArrayList<>(),new ItemStack(FHContent.FHItems.copper_core_spade));
        multiblock = s;
        block = b;
        
        tooltip.add(GuiUtils.translateTooltip(multiblock.getUniqueName().toString()));
    }
    public EffectBuilding(JsonObject jo) {
    	//this(MultiblockHandler.getByUniqueName(getId()),);
    	super(jo);
    }
    public EffectBuilding(PacketBuffer pb) {
    	super(pb);
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
	@Override
	public ResourceLocation getId() {
		return null;
	}
}
