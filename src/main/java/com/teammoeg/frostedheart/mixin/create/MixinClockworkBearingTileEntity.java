package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.ClockworkBearingTileEntity;

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
			stress+= hourHand.getContraption().getBlocks().size()*0.25F+hourHand.getContraption().getActors().size()*4F;
		}
		if(minuteHand!=null) {
			stress+= minuteHand.getContraption().getBlocks().size()*0.25F+minuteHand.getContraption().getActors().size()*4F;
		}
		this.lastStressApplied =stress;
		return lastStressApplied;
	}
}
