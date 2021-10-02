package com.teammoeg.frostedheart.mixin.snow;

import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import snownee.snow.ModUtil;
import snownee.snow.SnowCommonConfig;
import snownee.snow.block.ModSnowBlock;

@Mixin(ModSnowBlock.class)
public abstract class ModSnowBlockMixin extends SnowBlock {
	public ModSnowBlockMixin(Properties properties) {
		super(properties);
	}

	/**
	 * @author yuesha-yc
	 * @reason change snow collision shape to always be empty
	 */
	@Overwrite(remap = false)
	public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		if (!ModUtil.terraforged && SnowCommonConfig.thinnerBoundingBox) {
			return VoxelShapes.empty();
		} else {
			return super.getCollisionShape(state, worldIn, pos, context);
		}
	}

}
