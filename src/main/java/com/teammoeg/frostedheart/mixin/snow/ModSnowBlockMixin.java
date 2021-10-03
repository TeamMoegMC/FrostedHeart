package com.teammoeg.frostedheart.mixin.snow;

import net.minecraft.block.Block;
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

import static snownee.snow.block.ModSnowBlock.SNOW_SHAPES_MAGIC;

@Mixin(ModSnowBlock.class)
public abstract class ModSnowBlockMixin extends SnowBlock {
	public ModSnowBlockMixin(Properties properties) {
		super(properties);
	}

	private static final VoxelShape[] SHAPES = new VoxelShape[]{VoxelShapes.empty(), Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D), Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D), Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D), Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D), Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D), Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D), Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};

	/**
	 * @author yuesha-yc
	 * @reason change snow collision shape to always be empty
	 */
	@Overwrite(remap = false)
	public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		if (!ModUtil.terraforged && SnowCommonConfig.thinnerBoundingBox) {
			int layers = state.get(LAYERS);
			if (layers == 8) {
				return VoxelShapes.fullCube();
			} else if (layers > 5) {
				BlockState upState = worldIn.getBlockState(pos.up());
				return !upState.getBlock().isAir(upState, worldIn, pos) ? VoxelShapes.fullCube() : SHAPES[layers - 5];
			} else {
				return VoxelShapes.empty();
			}
		} else {
			return super.getCollisionShape(state, worldIn, pos, context);
		}
	}

}
