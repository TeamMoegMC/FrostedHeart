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

package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.jozufozu.flywheel.backend.material.MaterialManager;
import com.jozufozu.flywheel.core.materials.ModelData;
import com.jozufozu.flywheel.util.transform.MatrixTransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.components.deployer.DeployerActorInstance;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ActorInstance;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.worldWrappers.PlacementSimulationWorld;
import com.teammoeg.frostedheart.util.mixin.ISpeedContraption;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@Mixin(DeployerActorInstance.class)
public abstract class DeployerActorInstanceMixin extends ActorInstance {
    @Shadow(remap = false)
    Direction facing;
    @Shadow(remap = false)
    float yRot;
    @Shadow(remap = false)
    float zRot;
    @Shadow(remap = false)
    float zRotPole;
    @Shadow(remap = false)
    ModelData pole;
    @Shadow(remap = false)
    ModelData hand;

    @Shadow(remap = false)
    static void transformModel(MatrixTransformStack msr, ModelData pole, ModelData hand, float yRot, float zRot, float zRotPole) {
    }

    public DeployerActorInstanceMixin(MaterialManager<?> materialManager, PlacementSimulationWorld world,
                                      MovementContext context) {
        super(materialManager, world, context);
    }

    /**
     * @author khjxiaogu
     * @reason change speed of deployer animation
     */
    @Overwrite(remap = false)
    public void beginFrame() {
        double factor;
        if (context.contraption.stalled || context.position == null || context.data.contains("StationaryTimer")) {
            Contraption cont = context.contraption;
            //TODO: change to ModifyConstant
            if (cont instanceof ISpeedContraption) {
                factor = Mth.sin(AnimationTickHolder.getRenderTime() * .5f) * .05f + .45f;
            } else
                factor = Mth.sin(AnimationTickHolder.getRenderTime() * .5f) * .25f + .25f;
        } else {
            Vec3 center = VecHelper.getCenterOf(new BlockPos(context.position));
            double distance = context.position.distanceTo(center);
            double nextDistance = context.position.add(context.motion)
                    .distanceTo(center);
            factor = .5f - Mth.clamp(Mth.lerp(AnimationTickHolder.getPartialTicks(), distance, nextDistance), 0, 1);
        }

        Vec3 offset = Vec3.atLowerCornerOf(facing.getNormal()).scale(factor);

        PoseStack ms = new PoseStack();
        MatrixTransformStack msr = MatrixTransformStack.of(ms);

        msr.translate(context.localPos)
                .translate(offset);

        transformModel(msr, pole, hand, yRot, zRot, zRotPole);
    }

}
