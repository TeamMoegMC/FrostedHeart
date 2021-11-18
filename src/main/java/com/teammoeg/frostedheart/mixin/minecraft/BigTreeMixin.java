package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.block.trees.BigTree;
import net.minecraft.block.trees.Tree;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.server.ServerWorld;
@Mixin(BigTree.class)
public abstract class BigTreeMixin extends Tree {
	@Inject(at=@At("HEAD"),method="growBigTree",cancellable=true)
	public void placeMega(ServerWorld p_235678_1_, ChunkGenerator p_235678_2_, BlockPos p_235678_3_, BlockState p_235678_4_, Random p_235678_5_, int p_235678_6_, int p_235678_7_,CallbackInfoReturnable<Boolean> cr) {
		if(p_235678_5_.nextInt(24)!=0)
			cr.setReturnValue(false);
	}
}
