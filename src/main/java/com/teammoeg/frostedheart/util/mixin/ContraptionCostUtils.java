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

package com.teammoeg.frostedheart.util.mixin;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.BlockStressValues;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class ContraptionCostUtils {
    public static float calculateActorStressApply(Contraption cont) {
        float movecost = 0;
        for (MutablePair<StructureBlockInfo, MovementContext> i : cont.getActors())
            movecost += (float) BlockStressValues.getImpact(i.left.state().getBlock());
        return movecost;
    }

    public static float calculateRotationStressApply(Contraption cont) {
        float movecost = 0;
        for (StructureBlockInfo bi : cont.getBlocks().values()) {
            double dX = bi.pos().getX();
            double dZ = bi.pos().getZ();
            double dY = bi.pos().getY();
            double distance = Math.sqrt((dX * dX) + (dZ * dZ) + (dY * dY));
            try {
                if (bi.state().getBlockSupportShape(cont.getContraptionWorld(), bi.pos()) != Shapes.empty()) {
                    movecost += (float) (0.125F * 2.56F * distance);
                } else
                    movecost += (float) (0.075F * 2.56F * distance);
            } catch (Throwable t) {
                movecost += (float) (0.125F * 2.56F * distance);
            }
        }

        return movecost;
    }

    public static float calculateStressApply(Contraption cont) {
        float movecost = 0;
        for (StructureBlockInfo bi : cont.getBlocks().values()) {
            try {
                if (!bi.state().getBlockSupportShape(cont.getContraptionWorld(), bi.pos()).isEmpty()) {
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
