package com.teammoeg.frostedheart.mixin.supplementaries;

import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The faucet reads source fluids directly, including fluids without buckets. */
@Pseudo
@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.block.faucet.LiquidBlockInteraction", remap = false)
public abstract class LiquidBlockInteractionMixin_HotSpring {
    @Inject(method = "getProvidedFluid(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)Lnet/mehvahdjukaar/supplementaries/common/block/faucet/FluidOffer;",
            at = @At("HEAD"), cancellable = true)
    private void fh$leaveSpringInWorld(Level level, BlockPos pos, Direction side, FluidState fluid,
                                      CallbackInfoReturnable<Object> result) {
        if (fluid.getType().isSame(FHFluids.HOT_SPRING.get())) result.setReturnValue(null);
    }
}
