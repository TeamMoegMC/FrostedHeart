package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;

import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.teammoeg.frostedheart.util.ISpeedContraption;
@Mixin(Contraption.class)
public abstract class MixinContraption implements ISpeedContraption{
	int speed;
	@Override
	public int getSpeed() {
		return speed;
	}

	@Override
	public void setSpeed(int spd) {
		speed=spd;
	}


}
