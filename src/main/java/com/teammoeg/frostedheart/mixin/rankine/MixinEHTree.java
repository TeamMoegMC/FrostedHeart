package com.teammoeg.frostedheart.mixin.rankine;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;

import com.cannolicatfish.rankine.world.trees.EasternHemlockTree;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.block.BlockState;
import net.minecraft.block.trees.Tree;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.server.ServerWorld;

@Mixin(EasternHemlockTree.class)
public abstract class MixinEHTree extends Tree {
	@Override
	public boolean attemptGrowTree(ServerWorld world, ChunkGenerator chunkGenerator, BlockPos pos, BlockState state,
			Random rand) {
		if(FHUtils.canBigTreeGenerate(world, pos, rand))
			return super.attemptGrowTree(world, chunkGenerator, pos, state, rand);
		return false;
	}

}
