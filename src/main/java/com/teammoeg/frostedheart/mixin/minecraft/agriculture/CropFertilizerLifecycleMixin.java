package com.teammoeg.frostedheart.mixin.minecraft.agriculture;

import com.teammoeg.frostedheart.content.agriculture.CropFertilization;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Clears growth credit when crops are replaced, reset or removed. */
@Mixin(LevelChunk.class)
public class CropFertilizerLifecycleMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void fh$changed(BlockPos pos, BlockState state, boolean moving, CallbackInfoReturnable<BlockState> cir) {
        BlockState previous = cir.getReturnValue();
        if (previous != null && previous != state && ((LevelChunk)(Object)this).getLevel() instanceof ServerLevel level)
            CropFertilization.changed(level, pos, previous, state);
    }
}
