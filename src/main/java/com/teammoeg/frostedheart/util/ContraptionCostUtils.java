package com.teammoeg.frostedheart.util;

import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.foundation.block.BlockStressValues;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;
import org.apache.commons.lang3.tuple.MutablePair;

public class ContraptionCostUtils {
    public static float calculateStressApply(Contraption cont) {
        float movecost = 0;
        for (BlockInfo bi : cont.getBlocks().values()) {
            try {
                if (!bi.state.getCollisionShape(cont.getContraptionWorld(), bi.pos).isEmpty()) {
                    movecost += 0.125F;
                } else
                    movecost += 0.075F;
            } catch (Throwable t) {
                movecost += 0.125F;
            }
        }
        return movecost;
    }

    public static float calculateActorStressApply(Contraption cont) {
        float movecost = 0;
        for (MutablePair<BlockInfo, MovementContext> i : cont.getActors())
            movecost += BlockStressValues.getImpact(i.left.state.getBlock());
        return movecost;
    }

    public static float calculateRotationStressApply(Contraption cont) {
        float movecost = 0;
        for (BlockInfo bi : cont.getBlocks().values()) {
            double dX = bi.pos.getX();
            double dY = bi.pos.getY();
            double distance = Math.sqrt((dX * dX) + (dY * dY));
            try {
                if (bi.state.getCollisionShape(cont.getContraptionWorld(), bi.pos) != VoxelShapes.empty()) {
                    movecost += 0.125F * 2.56F * distance;
                } else
                    movecost += 0.075F * 2.56F * distance;
            } catch (Throwable t) {
                movecost += 0.125F * 2.56F * distance;
            }
        }

        return movecost;
    }

    public static void setSpeedAndCollect(AbstractContraptionEntity ace, float speed) {
        Contraption c = ace.getContraption();
        if (c instanceof ISpeedContraption) {
            ISpeedContraption isc = (ISpeedContraption) c;
            isc.setSpeed(speed);
            isc.contributeSpeed(speed);
        }
    }

    public static float getCost(AbstractContraptionEntity ace) {
        try {
            if (ace instanceof IStressContraption)
                return ((IStressContraption) ace).getStressCost();
        } catch (Throwable e) {//may we ignore and just calculate?
            if (ace.isAlive())
                return calculateStressApply(ace.getContraption()) + calculateActorStressApply(ace.getContraption());
        }
        return 0;
    }

    public static float getRotationCost(AbstractContraptionEntity ace) {
        try {
            if (ace instanceof IStressContraption)
                return ((IStressContraption) ace).getRotationStressCost();
        } catch (Throwable e) {//may we ignore and just calculate?
            if (ace.isAlive())
                return calculateRotationStressApply(ace.getContraption()) + calculateActorStressApply(ace.getContraption());
        }
        return 0;
    }

    public static float getActorCost(AbstractContraptionEntity ace) {
        try {
            if (ace instanceof IStressContraption)
                return ((IStressContraption) ace).getActorCost();
        } catch (Throwable e) {//may we ignore and just calculate?
            if (ace.isAlive())
                return calculateActorStressApply(ace.getContraption());
        }
        return 0;
    }
}
