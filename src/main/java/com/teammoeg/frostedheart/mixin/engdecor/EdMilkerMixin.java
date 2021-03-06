package com.teammoeg.frostedheart.mixin.engdecor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.teammoeg.frostedheart.util.IMilkable;

import net.minecraft.entity.passive.CowEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3d;
import wile.engineersdecor.blocks.EdMilker.MilkerTileEntity;

@Mixin(MilkerTileEntity.class)
public class EdMilkerMixin {
	@Inject(at=@At(value="INVOKE",target="Lnet/minecraft/entity/passive/CowEntity;isAlive()Z",ordinal=0,remap=true),method="milking_process",cancellable=true,require=1,locals=LocalCapture.CAPTURE_FAILHARD)
	private void fh$milkingPending(CallbackInfoReturnable<Boolean> cir,Direction facing,Vector3d target_pos,CowEntity cow) {
		IMilkable im=(IMilkable) cow;
		if(im.getMilk()<=0)cir.setReturnValue(false);
	}
	@Inject(at=@At(value="INVOKE",target="Lnet/minecraft/entity/passive/CowEntity;setNoAI(Z)V",ordinal=2,remap=true),method="milking_process",cancellable=true,locals=LocalCapture.CAPTURE_FAILHARD)
	private void fh$milked(CallbackInfoReturnable<Boolean> cir,Direction facing,Vector3d target_pos,CowEntity cow) {
		IMilkable im=(IMilkable) cow;
		im.setMilk((byte) (im.getMilk()-1));
	}
}
