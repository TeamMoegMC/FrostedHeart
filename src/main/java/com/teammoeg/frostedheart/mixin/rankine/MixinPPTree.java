package com.teammoeg.frostedheart.mixin.rankine;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cannolicatfish.rankine.world.trees.PinyonPineTree;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.block.BlockState;
import net.minecraft.block.trees.Tree;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.server.ServerWorld;
@Mixin(PinyonPineTree.class)
public abstract class MixinPPTree extends Tree{
	@Inject(at=@At("HEAD"),method="attemptGrowTree",cancellable=true)
	   public void FH$attemptGrowTree(ServerWorld world, ChunkGenerator chunkGenerator, BlockPos pos, BlockState state, Random rand,CallbackInfoReturnable<Boolean> cr) {
			FHUtils.canBigTreeGenerate(world,pos, rand, cr);
	  }

}
