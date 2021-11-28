package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.teammoeg.frostedheart.util.ISpeedContraption;

import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

@Mixin(MovementContext.class)
public class MixinMovementContext {
	@Shadow(remap=false)
	public Vector3d motion;
	@Shadow(remap=false)
	public World world;
	@Shadow(remap=false)
	public Contraption contraption;
	@Overwrite(remap=false)
	public float getAnimationSpeed() {
		int modifier = 1000;
		double length = -motion.length();
		if (world.isRemote && contraption.stalled) {
			if(contraption instanceof ISpeedContraption)
				return ((ISpeedContraption) contraption).getSpeed()/5;
			return 700;
		}
		if (Math.abs(length) < 1 / 512f)
			return 0;
		if(contraption instanceof ISpeedContraption)
			return ((ISpeedContraption) contraption).getSpeed()/5;
		return (((int) (length * modifier + 100 * Math.signum(length))) / 100) * 100;
	}
}
