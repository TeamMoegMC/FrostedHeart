package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.gantry.GantryContraption;
import com.simibubi.create.content.contraptions.relays.advanced.GantryShaftTileEntity;
import com.teammoeg.frostedheart.util.ContraptionCostUtils;

import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
@Mixin(GantryShaftTileEntity.class)
public abstract class MixinGantryShaftTileEntity extends KineticTileEntity{
	public MixinGantryShaftTileEntity(TileEntityType<?> typeIn) {
		super(typeIn);
	}
	public AbstractContraptionEntity currentComp;
	@Override
	public float calculateStressApplied() {
		if(currentComp!=null) {
			//float impact = currentComp.getContraption().getBlocks().size()*4;
			Direction facing = ((GantryContraption)currentComp.getContraption()).getFacing();
			Vector3d currentPosition = currentComp.getAnchorVec().add(.5, .5, .5);
			BlockPos gantryShaftPos = new BlockPos(currentPosition).offset(facing.getOpposite());
			if(gantryShaftPos.equals(this.pos)) {
				this.lastStressApplied = ContraptionCostUtils.getCost(currentComp);
				return lastStressApplied;
			}
		}
		return 0;
	}
}
