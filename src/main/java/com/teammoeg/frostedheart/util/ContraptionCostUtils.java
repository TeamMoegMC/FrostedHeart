package com.teammoeg.frostedheart.util;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.foundation.block.BlockStressValues;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;

public class ContraptionCostUtils {
	public static float calculateStressApply(Contraption cont) {
		float movecost = 0;
		for(BlockInfo bi:cont.getBlocks().values()) {
			if(bi.state.getBlock().getCollisionShape(bi.state,cont.getContraptionWorld(),bi.pos)==VoxelShapes.fullCube()) {
				movecost+=0.125F;
			}else
				movecost+=0.075F;
		}
		return movecost;
	}
	public static float calculateActorStressApply(Contraption cont) {
		float movecost = 0;
		for(MutablePair<BlockInfo, MovementContext> i:cont.getActors())
			movecost+=BlockStressValues.getImpact(i.left.state.getBlock());
		return movecost;
	}
	public static float calculateRotationStressApply(Contraption cont) {
		float movecost = 0;
		for(BlockInfo bi:cont.getBlocks().values()) {
			double dX=bi.pos.getX();
			double dY=bi.pos.getY();
			double distance=Math.sqrt((dX*dX)+(dY*dY));
			if(bi.state.getBlock().getCollisionShape(bi.state,cont.getContraptionWorld(),bi.pos)==VoxelShapes.fullCube()) {
				movecost+=0.125F*2.5F*distance;
			}else
				movecost+=0.075F*2.5F*distance;
		}

		return movecost;
	}
	public static float getCost(AbstractContraptionEntity ace) {
		try {
			return (float) ace.getClass().getMethod("getStressCost").invoke(ace);
		} catch (Exception e) {//may we ignore and just calculate?
			if(ace.isAlive())
				return calculateStressApply(ace.getContraption())+ calculateActorStressApply(ace.getContraption());
			return 0;
		}
	}
	public static float getRotationCost(AbstractContraptionEntity ace) {
		try {
			return (float) ace.getClass().getMethod("getRotationStressCost").invoke(ace);
		} catch (Exception e) {//may we ignore and just calculate?
			if(ace.isAlive())
				return calculateRotationStressApply(ace.getContraption())+ calculateActorStressApply(ace.getContraption());
			return 0;
		}
	}
	public static float getActorCost(AbstractContraptionEntity ace) {
		try {
			return (float) ace.getClass().getMethod("getActorCost").invoke(ace);
		} catch (Exception e) {//may we ignore and just calculate?
			if(ace.isAlive())
				return calculateRotationStressApply(ace.getContraption())+ calculateActorStressApply(ace.getContraption());
			return 0;
		}
	}
}
