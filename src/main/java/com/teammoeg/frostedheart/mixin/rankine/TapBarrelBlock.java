package com.teammoeg.frostedheart.mixin.rankine;

import com.cannolicatfish.rankine.blocks.tap.TapBarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TapBarrelBlock.class)
class TapBarrelBlockMixin {
    /**
     * @author
     */
    @Overwrite
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        Item handItem = player.getHeldItemMainhand().getItem();
        if (handItem == Items.BUCKET) {
            Item bucket = null;
            if (bucket != null && state.get(TapBarrelBlock.LEVEL) > 0) {
                player.getHeldItemMainhand().shrink(1);
                player.addItemStackToInventory(new ItemStack(bucket, 1));
                if (state.get(TapBarrelBlock.LEVEL) == 1) {
                    worldIn.setBlockState(pos, state.with(com.cannolicatfish.rankine.blocks.tap.TapBarrelBlock.FLUID, state.get(TapBarrelBlock.FLUID)).with(TapBarrelBlock.LEVEL, 0), 3);
                } else {
                    worldIn.setBlockState(pos, state.with(com.cannolicatfish.rankine.blocks.tap.TapBarrelBlock.FLUID, state.get(TapBarrelBlock.FLUID)).with(TapBarrelBlock.LEVEL, state.get(TapBarrelBlock.LEVEL) - 1), 3);
                }

                return ActionResultType.SUCCESS;
            } else {
                return ActionResultType.FAIL;
            }
        } else return ActionResultType.FAIL;
    }
}
