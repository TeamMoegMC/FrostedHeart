package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.gantry.GantryContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.gantry.GantryContraptionEntity;
import com.simibubi.create.content.contraptions.relays.advanced.GantryShaftTileEntity;

import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.entity.EntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
@Mixin(GantryContraptionEntity.class)
public abstract class MixinGantryContraptionEntity extends AbstractContraptionEntity {

	public MixinGantryContraptionEntity(EntityType<?> entityTypeIn, World worldIn) {
		super(entityTypeIn, worldIn);
	}
	@Inject(at=@At("HEAD"),method="checkPinionShaft",remap=false)
	protected void checkPinionShaft(CallbackInfo cbi) {
		Direction facing = ((GantryContraption) contraption).getFacing();
		Vector3d currentPosition = getAnchorVec().add(.5, .5, .5);
		BlockPos gantryShaftPos = new BlockPos(currentPosition).offset(facing.getOpposite());

		TileEntity te = world.getTileEntity(gantryShaftPos);
		if (te instanceof GantryShaftTileEntity) {
			if (world.isRemote) {
				GantryShaftTileEntity gte=(GantryShaftTileEntity) te;
				try {
					GantryShaftTileEntity.class.getField("currentComp").set(gte,this);
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
						| SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return;
		}
	}

}
