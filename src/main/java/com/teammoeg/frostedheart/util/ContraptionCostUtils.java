/*
 * Copyright (c) 2022-2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.util;

import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.foundation.block.BlockStressValues;
import com.teammoeg.frostedheart.util.mixin.ISpeedContraption;
import com.teammoeg.frostedheart.util.mixin.IStressContraption;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;
import org.apache.commons.lang3.tuple.MutablePair;

public class ContraptionCostUtils {
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
            double dZ = bi.pos.getZ();
            double dY = bi.pos.getY();
            double distance = Math.sqrt((dX * dX) + (dZ * dZ) + (dY * dY));
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

    public static void setSpeedAndCollect(AbstractContraptionEntity ace, float speed) {
        Contraption c = ace.getContraption();
        if (c instanceof ISpeedContraption) {
            ISpeedContraption isc = (ISpeedContraption) c;
            isc.setSpeed(speed);
            isc.contributeSpeed(speed);
        }
    }
}
