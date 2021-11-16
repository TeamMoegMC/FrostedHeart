package com.teammoeg.frostedheart.mixin.watersource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import gloridifice.watersource.WaterSource;
import gloridifice.watersource.common.item.WaterBagItem;
import gloridifice.watersource.registry.ItemRegistry;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.ItemFluidContainer;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
@Mixin(WaterBagItem.class)
public class WaterBagItemMixin extends ItemFluidContainer {

	public WaterBagItemMixin(Properties properties, int capacity) {
		super(properties, capacity);
	}
	@Overwrite(remap=false)
    public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable CompoundNBT nbt)
    {
        return new FluidHandlerItemStack(stack, capacity)
        {
			@Override
			public boolean canFillFluidType(FluidStack fluid) {
				return isFluidValid(0,fluid);
			}
            @Nonnull
            @Override
            @SuppressWarnings("deprecation")
            public ItemStack getContainer()
            {
                return getFluid().isEmpty() ? new ItemStack(ItemRegistry.itemLeatherWaterBag) : this.container;
            }
            @Override
            public boolean isFluidValid(int tank, @Nonnull FluidStack stack)
            {
            	if(stack.getFluid().getAttributes().getTemperature()>427)return false;
                for (Fluid fluid : FluidTags.getCollection().get(new ResourceLocation(WaterSource.MODID,"drink")).getAllElements()){
                    if (fluid == stack.getFluid()){
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
