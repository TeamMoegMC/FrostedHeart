package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.teammoeg.frostedheart.util.ISpeedContraption;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
@Mixin(Contraption.class)
public abstract class MixinContraption implements ISpeedContraption{
	float speed;
	float sc=0;
	@Override
	public float getSpeed() {
		return speed;
	}

	@Override
	public void contributeSpeed(float s) {
		if(sc<20480)
			sc+=Math.abs(s);
	}

	@Override
	public void setSpeed(float spd) {
		speed=spd;
	}/**
	
	*@author khjxiaogu
	*@reason no more instabreak
	*/
	@Overwrite(remap=false)
	protected boolean customBlockPlacement(IWorld world, BlockPos targetPos, BlockState state) {
		BlockState blockState = world.getBlockState(targetPos);
		
		if(sc<20480)
			if (!blockState.getCollisionShape(world, targetPos).isEmpty()) {
				if (targetPos.getY() == 0)
					targetPos = targetPos.up();
				world.playEvent(2001, targetPos, Block.getStateId(state));
				Block.spawnDrops(state, world, targetPos, null);
				return true;
			}
		return false;
	}
	@Inject(at=@At("RETURN"),method="writeNBT",remap=false,locals=LocalCapture.CAPTURE_FAILHARD)
	public void fh$writeNBT(boolean spawnPacket,CallbackInfoReturnable<CompoundNBT> cbi,CompoundNBT cnbt) {
		cnbt.putFloat("speedCollected",sc);
	}
	@Inject(at=@At("RETURN"),method="readNBT",remap=false)
	public void fh$readNBT(World world, CompoundNBT nbt, boolean spawnData,CallbackInfo cbi) {
		sc=nbt.getFloat("speedCollected");
	}
}
