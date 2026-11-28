package com.teammoeg.frostedheart.mixin.minecraft.agriculture;

import com.teammoeg.frostedheart.content.agriculture.CropFertilization;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge emits Post only for cane extension, not its intermediate age increments. */
@Mixin(SugarCaneBlock.class)
public class SugarCaneFertilizerMixin {
    @Inject(method = "randomTick", at = @At("RETURN"))
    private void fh$ageGrowth(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        CropFertilization.after(new BlockEvent.CropGrowEvent.Post(level, pos, state, level.getBlockState(pos)));
    }
}
