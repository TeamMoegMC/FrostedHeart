package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.piston.LinearActuatorTileEntity;
import com.teammoeg.frostedheart.util.ContraptionCostUtils;

import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.vector.Vector3d;
@Mixin({LinearActuatorTileEntity.class})
public abstract class MixinPulleyTileEntity extends KineticTileEntity {

	public MixinPulleyTileEntity(TileEntityType<?> typeIn) {
		super(typeIn);
	}
	@Shadow(remap=false)
	public AbstractContraptionEntity movedContraption;
	@Shadow(remap=false)
	public abstract Vector3d getMotionVector();
	@Override
	public float calculateStressApplied() {
		
		if(movedContraption!=null) {
			ContraptionCostUtils.setSpeedAndCollect(movedContraption, (int) speed);
			if(getMotionVector().getY()<0) {
				this.lastStressApplied = ContraptionCostUtils.getActorCost(movedContraption)+0.5F;
				return lastStressApplied;
			}
			this.lastStressApplied = ContraptionCostUtils.getCost(movedContraption)+1;
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
