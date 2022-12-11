package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.block.BlockState;
import net.minecraft.block.FungusBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

@Mixin(FungusBlock.class)
public class FungisBlockMixin {
	@Inject(at=@At("HEAD"),method="canGrow",remap=true,cancellable=true)
	public void canGrow(IBlockReader worldIn, BlockPos pos, BlockState state, boolean isClient,
			CallbackInfoReturnable<Boolean> cbi) {
		if(!FHUtils.canNetherTreeGrow(worldIn,pos)) {
			
		}
	}

}
