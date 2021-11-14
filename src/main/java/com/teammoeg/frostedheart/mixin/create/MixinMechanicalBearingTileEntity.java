package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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
		this.lastStressApplied =ContraptionCostUtils.getRotationCost(movedContraption)+1;
		return lastStressApplied;
	}
}
