package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.jozufozu.flywheel.backend.material.MaterialManager;
import com.jozufozu.flywheel.core.materials.ModelData;
import com.jozufozu.flywheel.util.transform.MatrixTransformStack;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.content.contraptions.base.RotatingData;
import com.simibubi.create.content.contraptions.components.deployer.DeployerActorInstance;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ActorInstance;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.worldWrappers.PlacementSimulationWorld;
import com.teammoeg.frostedheart.util.ISpeedContraption;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

@Mixin(DeployerActorInstance.class)
public abstract class DeployerActorInstanceMixin extends ActorInstance{
	@Shadow(remap=false)
    Direction facing;
	@Shadow(remap=false)
    float yRot;
	@Shadow(remap=false)
    float zRot;
	@Shadow(remap=false)
    float zRotPole;
	@Shadow(remap=false)
    ModelData pole;
	@Shadow(remap=false)
    ModelData hand;
    public DeployerActorInstanceMixin(MaterialManager<?> materialManager, PlacementSimulationWorld world,
			MovementContext context) {
		super(materialManager, world, context);
	}
	/**
	 * @author khjxiaogu
	 * @reason change speed of deployer animation
	 */
	@Overwrite(remap=false)
    public void beginFrame() {
        double factor;
        if (context.contraption.stalled || context.position == null || context.data.contains("StationaryTimer")) {
        	Contraption cont=context.contraption;
        	if(cont instanceof ISpeedContraption) {
        		factor =  MathHelper.sin(AnimationTickHolder.getRenderTime()* .5f) * .05f + .45f;
        	}else
        		factor = MathHelper.sin(AnimationTickHolder.getRenderTime() * .5f) * .25f + .25f;
        } else {
            Vector3d center = VecHelper.getCenterOf(new BlockPos(context.position));
            double distance = context.position.distanceTo(center);
            double nextDistance = context.position.add(context.motion)
                                                  .distanceTo(center);
            factor = .5f - MathHelper.clamp(MathHelper.lerp(AnimationTickHolder.getPartialTicks(), distance, nextDistance), 0, 1);
        }

        Vector3d offset = Vector3d.copy(facing.getDirectionVec()).scale(factor);

        MatrixStack ms = new MatrixStack();
        MatrixTransformStack msr = MatrixTransformStack.of(ms);

        msr.translate(context.localPos)
           .translate(offset);

        transformModel(msr, pole, hand, yRot, zRot, zRotPole);
    }
	@Shadow(remap=false)
	static void transformModel(MatrixTransformStack msr, ModelData pole, ModelData hand, float yRot, float zRot, float zRotPole) {};
}
