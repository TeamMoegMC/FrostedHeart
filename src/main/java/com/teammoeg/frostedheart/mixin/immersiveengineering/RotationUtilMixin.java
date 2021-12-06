package com.teammoeg.frostedheart.mixin.immersiveengineering;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import blusunrize.immersiveengineering.common.util.RotationUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
@Mixin(RotationUtil.class)
public class RotationUtilMixin {
	private final static ResourceLocation tag=new ResourceLocation("immersiveengineering","no_rotation");
    /**
     * @author khjxiaogu
     * @reason fix some rotation bug
     */
	@Overwrite(remap=false)
	public static boolean rotateBlock(World world, BlockPos pos, boolean inverse)
	{
		if(!world.getBlockState(pos).getBlock().getTags().contains(tag))
			return RotationUtil.rotateBlock(world, pos, inverse?Rotation.COUNTERCLOCKWISE_90: Rotation.CLOCKWISE_90);
		return false;
	}
	
}
