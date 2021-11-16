package com.teammoeg.frostedheart.mixin.forge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStackSimple;
@Mixin(FluidHandlerItemStackSimple.class)
public abstract class FluidHandlerItemStackSimpleMixin implements IFluidHandlerItem, ICapabilityProvider {
    /**
     * @author khjxiaogu
     * @reason Forge set default return true and some stupid mod does not check isFluidValue. So set default check
     */
	@Overwrite(remap=false)
    public boolean canFillFluidType(FluidStack fluid)
    {
        return isFluidValid(0,fluid);
    }
}
