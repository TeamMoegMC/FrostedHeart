package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.content.contraptions.components.structureMovement.piston.LinearActuatorTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.pulley.PulleyTileEntity;
import com.teammoeg.frostedheart.util.ContraptionCostUtils;
import net.minecraft.tileentity.TileEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PulleyTileEntity.class)
public abstract class MixinPulleyTileEntity extends LinearActuatorTileEntity {

	public MixinPulleyTileEntity(TileEntityType<?> typeIn) {
		super(typeIn);
	}
	@Override
	public float calculateStressApplied() {
		
		if(super.movedContraption!=null) {
			ContraptionCostUtils.setSpeed(movedContraption, (int) speed);
			if(this.getMotionVector().getY()<0) {
				this.lastStressApplied = ContraptionCostUtils.getActorCost(super.movedContraption)+0.5F;
				return lastStressApplied;
			}
			this.lastStressApplied = ContraptionCostUtils.getCost(super.movedContraption)+1;
			return lastStressApplied;
		}
		return 1;
	}
	@Inject(at=@At("TAIL"),method="tick")
	public void FH_MICR_tick(CallbackInfo cbi) {
		if((!world.isRemote)&&super.hasNetwork())
			getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
	}
}
