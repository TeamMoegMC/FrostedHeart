package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import it.unimi.dsi.fastutil.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidDrainingBehaviour.class, remap = false)
public abstract class FluidDrainingBehaviourMixin_HotSpring {
    @Shadow Fluid fluid;
    @Shadow PriorityQueue<FluidManipulationBehaviour.BlockPosEntry> queue;

    @Inject(method = "pullNext", at = @At("HEAD"), cancellable = true, remap = false)
    private void fh$keepHotSpringInWorld(BlockPos root, boolean simulate,
                                          CallbackInfoReturnable<Boolean> result) {
        if ((fluid != null && fluid.isSame(FHFluids.HOT_SPRING.get()))
                || (!queue.isEmpty() && ((FluidDrainingBehaviour) (Object) this).getWorld()
                .getFluidState(queue.first().pos()).getType().isSame(FHFluids.HOT_SPRING.get()))) {
            result.setReturnValue(false);
        }
    }
}
