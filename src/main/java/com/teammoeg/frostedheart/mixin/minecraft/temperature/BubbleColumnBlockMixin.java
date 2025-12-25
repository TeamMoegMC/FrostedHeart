package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BubbleColumnBlock.class)
public class BubbleColumnBlockMixin {

    @Inject(method = "getColumnState", at = @At("HEAD"), cancellable = true)
    private static void modifyGetColumnState(BlockState pBlockState, CallbackInfoReturnable<BlockState> cir) {
        // Check if the block is our cooled magma block
        if (pBlockState.getBlock() == FHBlocks.COOLED_MAGMA_BLOCK.get()) {
            // Return the same state as if it were a regular magma block
            cir.setReturnValue(Blocks.BUBBLE_COLUMN.defaultBlockState()
                    .setValue(BubbleColumnBlock.DRAG_DOWN, Boolean.valueOf(true)));
            cir.cancel();
        }
    }
}
