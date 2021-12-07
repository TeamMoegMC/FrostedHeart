package com.teammoeg.frostedheart.mixin.rankine;

import com.cannolicatfish.rankine.world.trees.PinyonPineTree;
import com.teammoeg.frostedheart.util.FHUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.trees.Tree;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Random;

@Mixin(PinyonPineTree.class)
public abstract class MixinPPTree extends Tree {
	@Override
	public boolean attemptGrowTree(ServerWorld world, ChunkGenerator chunkGenerator, BlockPos pos, BlockState state,
			Random rand) {
		if (FHUtils.canBigTreeGenerate(world, pos, rand))
			return super.attemptGrowTree(world, chunkGenerator, pos, state, rand);
		return false;
	}

}
