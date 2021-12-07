package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.ClockworkBearingTileEntity;
import com.teammoeg.frostedheart.util.ContraptionCostUtils;
import net.minecraft.tileentity.TileEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
			ContraptionCostUtils.setSpeed(hourHand,speed/4F);
			stress+= ContraptionCostUtils.getRotationCost(hourHand);
		}
		if(minuteHand!=null) {
			ContraptionCostUtils.setSpeed(minuteHand,speed/4F);
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
