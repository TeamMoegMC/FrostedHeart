package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;

import com.simibubi.create.content.contraptions.components.structureMovement.piston.LinearActuatorTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.pulley.PulleyTileEntity;
import com.teammoeg.frostedheart.util.ContraptionCostUtils;

import net.minecraft.tileentity.TileEntityType;
@Mixin(PulleyTileEntity.class)
public abstract class MixinPulleyTileEntity extends LinearActuatorTileEntity {

	public MixinPulleyTileEntity(TileEntityType<?> typeIn) {
		super(typeIn);
	}
	@Override
	public float calculateStressApplied() {
		if(super.movedContraption!=null) {
			this.lastStressApplied = ContraptionCostUtils.getCost(super.movedContraption);
			
			return lastStressApplied;
		}
		return 1;
	}
}
