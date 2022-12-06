package com.teammoeg.frostedheart.mixin.rankine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.cannolicatfish.rankine.blocks.BlastingPowderBlock;

import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
@Mixin(BlastingPowderBlock.class)
public class MixinBlastingPowderBlock extends FallingBlock {

	public MixinBlastingPowderBlock(Properties properties) {
		super(properties);
	}
	/**
	 * 
	 * @author khjxiaogu
	 * @reason Fix dupe 
	 * */
	@Override
	@Overwrite(remap=false)
	public void catchFire(BlockState state, World world, BlockPos pos, net.minecraft.util.Direction face,
			LivingEntity igniter) {
		world.removeBlock(pos, false);
		world.createExplosion(igniter, pos.getX(), pos.getY() + 16 * .0625D, pos.getZ(), 2.4F, Explosion.Mode.BREAK);
	}

}
