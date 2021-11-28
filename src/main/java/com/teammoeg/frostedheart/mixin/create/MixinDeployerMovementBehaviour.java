package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.simibubi.create.content.contraptions.components.deployer.DeployerMovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.teammoeg.frostedheart.util.ISpeedContraption;

import net.minecraft.util.math.MathHelper;
@Mixin(DeployerMovementBehaviour.class)
public abstract class MixinDeployerMovementBehaviour extends MovementBehaviour {
	@ModifyConstant(method="tick",remap=false,constant=@Constant(intValue=20,ordinal=0))
	public int getTimerTick(int in) {
		return 1000;
	}
	
	@Inject(method="tick",remap=false,
			at=@At(value = "INVOKE",
			target="Lnet/minecraft/nbt/CompoundNBT;putInt(Ljava/lang/String;I)V",
			ordinal=0
			),cancellable=true)
	
	public void doTimer(MovementContext m,CallbackInfo cbi) {
		Contraption c=m.contraption;
		if(c instanceof ISpeedContraption) {
			int timer=m.data.getInt("Timer");
			timer+=MathHelper.clamp(Math.abs(((ISpeedContraption) c).getSpeed()),4,256);
			m.data.putInt("Timer", timer);
			cbi.cancel();
		}
		
	}
}
