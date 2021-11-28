package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.ClockworkBearingTileEntity;
import com.teammoeg.frostedheart.util.ContraptionCostUtils;

import net.minecraft.tileentity.TileEntityType;
@Mixin(ClockworkBearingTileEntity.class)
public abstract class MixinClockworkBearingTileEntity extends KineticTileEntity {

	public MixinClockworkBearingTileEntity(TileEntityType<?> typeIn) {
		super(typeIn);
	}
	@Shadow(remap=false)
	protected ControlledContraptionEntity hourHand;
	@Shadow(remap=false)
	protected ControlledContraptionEntity minuteHand;
	@Override
	public float calculateStressApplied() {
		float stress=1;
		if(hourHand!=null) {
			ContraptionCostUtils.setSpeed(hourHand, (int) speed);
			stress+= ContraptionCostUtils.getRotationCost(hourHand);
		}
		if(minuteHand!=null) {
			ContraptionCostUtils.setSpeed(minuteHand, (int) speed);
			stress+= ContraptionCostUtils.getRotationCost(minuteHand);
		}
		
		this.lastStressApplied =stress;
		return lastStressApplied;
	}
	@Inject(at=@At("TAIL"),method="tick")
	public void FH_MICR_tick(CallbackInfo cbi) {
		if((!world.isRemote)&&super.hasNetwork())
			getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
	}
}
