package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.IBearingTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.MechanicalBearingTileEntity;
import com.teammoeg.frostedheart.util.ContraptionCostUtils;

import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
@Mixin(MechanicalBearingTileEntity.class)
public abstract class MixinMechanicalBearingTileEntity extends GeneratingKineticTileEntity
		implements IBearingTileEntity {
	public MixinMechanicalBearingTileEntity(TileEntityType<?> typeIn) {
		super(typeIn);
	}

	@Shadow(remap=false)
	protected ControlledContraptionEntity movedContraption;

	@Override
	public float calculateStressApplied() {
		if(movedContraption!=null) {
			this.lastStressApplied =ContraptionCostUtils.getRotationCost(movedContraption)+1;
		}else
			this.lastStressApplied=0.5F;
		return lastStressApplied;
	}
	@Inject(at=@At("TAIL"),method="tick")
	public void FH_MICR_tick(CallbackInfo cbi) {
		if((!world.isRemote)&&super.hasNetwork())
			getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
	}
}
