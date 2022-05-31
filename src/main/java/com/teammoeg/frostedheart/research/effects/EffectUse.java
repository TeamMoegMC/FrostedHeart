package com.teammoeg.frostedheart.research.effects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows the research team to use certain machines
 */
public class EffectUse extends Effect {

    List<Block> blocksToUse;

    public EffectUse(Block... blocks) {
    	super(GuiUtils.translateGui("effect.use"),new ArrayList<>(),FHIcons.getIcon(blocks));
        blocksToUse = new ArrayList<>();
        for (Block b : blocks) {
            blocksToUse.add(b);
        }
    }

    public List<Block> getBlocksToUse() {
        return blocksToUse;
    }
    public EffectUse(JsonObject jo) {
    	super(jo);
    }
    @Override
    public void init() {

    }

    @Override
    public void grant(TeamResearchData team, PlayerEntity triggerPlayer) {

    }

    @Override
    public void revoke(TeamResearchData team) {

    }


	@Override
	public String getId() {
		return null;
	}
}
