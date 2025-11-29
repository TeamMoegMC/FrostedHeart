package com.teammoeg.frostedheart.content.water.item;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

public abstract class SingleUseSwapDrinkContainerItem extends SingleUseDrinkContainerItem{



	public SingleUseSwapDrinkContainerItem(Properties properties, int capacity) {
		super(properties, capacity);

	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
		return new FluidHandlerItemStack.SwapEmpty(stack,getContainerItem(),super.capacity) {
			@Override
            public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
                return isValidFluid(stack);
            }
			@Override
			protected void setFluid(FluidStack fluid) {
				if(!fluid.isEmpty())
					super.container=getDrinkItem();
				super.setFluid(fluid);
			}

			@Override
			public int fill(FluidStack resource, FluidAction doFill) {
				if(resource.getAmount()<this.capacity)return 0;
				return super.fill(resource, doFill);
			}

			@Override
			public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
				if(resource.getAmount()<this.capacity)return FluidStack.EMPTY;
				return super.drain(resource, action);
			}

			@Override
			public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
				if(maxDrain<this.capacity)return FluidStack.EMPTY;
				return super.drain(maxDrain, action);
			}
			
			
		};
	}

	
	public abstract ItemStack getContainerItem() ;

	
	public abstract ItemStack getDrinkItem() ;
}
