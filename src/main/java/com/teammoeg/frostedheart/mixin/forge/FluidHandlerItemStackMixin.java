package com.teammoeg.frostedheart.mixin.forge;

import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FluidHandlerItemStack.class)
public abstract class FluidHandlerItemStackMixin implements IFluidHandlerItem, ICapabilityProvider {
    /**
     * @author khjxiaogu
     * @reason Forge set default return true, causing bugs. So set default check
     */
    @Overwrite(remap = false)
    public boolean canFillFluidType(FluidStack fluid) {
        return isFluidValid(0, fluid);
    }
}
