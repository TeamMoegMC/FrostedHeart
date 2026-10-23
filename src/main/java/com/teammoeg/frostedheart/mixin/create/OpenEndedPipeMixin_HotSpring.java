package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.content.fluids.OpenEndedPipe;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = OpenEndedPipe.class, remap = false)
public abstract class OpenEndedPipeMixin_HotSpring {
    @Shadow private Level world;
    @Shadow private BlockPos outputPos;

    @Inject(method = "removeFluidFromSpace", at = @At("HEAD"), cancellable = true, remap = false)
    private void fh$keepHotSpringInWorld(boolean simulate, CallbackInfoReturnable<FluidStack> result) {
        if (world != null && world.isLoaded(outputPos)
                && world.getFluidState(outputPos).getType().isSame(FHFluids.HOT_SPRING.get())) {
            result.setReturnValue(FluidStack.EMPTY);
        }
    }
}
