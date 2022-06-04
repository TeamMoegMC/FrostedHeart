package com.teammoeg.frostedheart.research.effects;

import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.TeamResearchData;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

/**
 * Allows forming multiblock
 */
public class EffectBuilding extends Effect {

    IMultiblock multiblock;
    public EffectBuilding(IETemplateMultiblock s, Block b) {
    	super("@gui." + FHMain.MODID + ".effect.building",new ArrayList<>(),b);
        multiblock = s;
        
        tooltip.add("@"+b.getTranslationKey());
    }
    public EffectBuilding(JsonObject jo) {
    	super(jo);
    	multiblock = MultiblockHandler.getByUniqueName(new ResourceLocation(jo.get("multiblock").getAsString()));
    }
    public EffectBuilding(PacketBuffer pb) {
    	super(pb);
    	multiblock=MultiblockHandler.getByUniqueName(pb.readResourceLocation());
    }

    public IMultiblock getMultiblock() {
        return multiblock;
    }

    @Override
    public void init() {
    	ResearchListeners.multiblock.add(multiblock);
    }

    @Override
    public boolean grant(TeamResearchData team, PlayerEntity triggerPlayer) {
    	team.building.add(multiblock);
		return true;
    	
    }

    @Override
    public void revoke(TeamResearchData team) {
    	team.building.remove(multiblock);
    }

	@Override
	public String getId() {
		return "multiblock";
	}
	@Override
	public JsonObject serialize() {
		JsonObject jo=super.serialize();
		jo.addProperty("multiblock", multiblock.getUniqueName().toString());
		return jo;
	}
	@Override
	public void write(PacketBuffer buffer) {
		super.write(buffer);
		buffer.writeResourceLocation(multiblock.getUniqueName());
	}
	@Override
	public int getIntID() {
		return 1;
	}

}
