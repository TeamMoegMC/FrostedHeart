package com.teammoeg.frostedheart.mixin.immersiveengineering;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Utils.class, remap = false)
public abstract class UtilsMixin_HotSpring {
    @Inject(method = "drainFluidBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void fh$keepHotSpringInWorld(Level world, BlockPos pos, FluidAction action,
                                                 CallbackInfoReturnable<FluidStack> result) {
        if (world.getFluidState(pos).getType().isSame(FHFluids.HOT_SPRING.get())) {
            result.setReturnValue(FluidStack.EMPTY);
        }
    }
}
