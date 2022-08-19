package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.block.BlockState;
import net.minecraft.block.BushBlock;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SaplingBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
@Mixin(SaplingBlock.class)
public abstract class SaplingBlockMixin extends BushBlock implements IGrowable {
	
	public SaplingBlockMixin(Properties properties) {
		super(properties);
	}

	@Inject(at=@At("HEAD"),method="randomTick",remap=true,cancellable=true)
	public void fh$randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random,CallbackInfo cbi) {
		if(!FHUtils.canTreeGrow(worldIn, pos, random))
			cbi.cancel();
	}

}
