package com.teammoeg.frostedheart.research.effects;

import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.ResearchGlobals;
import com.teammoeg.frostedheart.research.TeamResearchData;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;

/**
 * Allows forming multiblock
 */
public class EffectBuilding extends Effect {

    IETemplateMultiblock multiblock;
    Block block;


    public EffectBuilding(IETemplateMultiblock s, Block b) {
    	super(GuiUtils.translateGui("effect.building"),new ArrayList<>(),b);
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
    	ResearchGlobals.multiblock.unlock(multiblock.getUniqueName());//This list treat as blacklist, so unlock is lock,
																	  //THIS IS NOT AN ERROR
    }

    @Override
    public void grant(TeamResearchData team, PlayerEntity triggerPlayer) {
    	team.building.unlock(multiblock.getUniqueName());
    	
    }

    @Override
    public void revoke(TeamResearchData team) {
    	team.building.lock(multiblock.getUniqueName());
    }

	@Override
	public String getId() {
		return "multiblock";
	}

}
