package com.teammoeg.frostedheart.util;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.foundation.block.BlockStressValues;

import net.minecraft.world.gen.feature.template.Template.BlockInfo;

public class ContraptionCostUtils {
	public static float calculateStressApply(Contraption cont) {
		float movecost=cont.getBlocks().size()*0.125F;
		for(MutablePair<BlockInfo, MovementContext> i:cont.getActors())
			movecost+=BlockStressValues.getImpact(i.left.state.getBlock());
		return movecost;
	}
	public static float getCost(AbstractContraptionEntity ace) {
		try {
			return (float) ace.getClass().getMethod("getStressCost").invoke(ace);
		} catch (Exception e) {//may we ignore and just calculate?
			if(ace.isAlive())
				return calculateStressApply(ace.getContraption());
			return 0;
		}
	}
}
