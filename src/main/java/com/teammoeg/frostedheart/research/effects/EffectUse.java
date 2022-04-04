package com.teammoeg.frostedheart.research.effects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.block.Block;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows the research team to use certain machines
 */
public class EffectUse extends Effect {

    List<Block> blocksToUse;

    public EffectUse(Block... blocks) {
        name = GuiUtils.translateGui("effect.use");
        blocksToUse = new ArrayList<>();
        for (Block b : blocks) {
            blocksToUse.add(b);
        }
    }

    public List<Block> getBlocksToUse() {
        return blocksToUse;
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
	public ResourceLocation getId() {
		return null;
	}
}
