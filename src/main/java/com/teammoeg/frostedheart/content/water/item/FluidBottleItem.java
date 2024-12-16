package com.teammoeg.frostedheart.content.water.item;

import com.teammoeg.frostedheart.bootstrap.common.FHItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class FluidBottleItem extends DrinkContainerItem{
    public FluidBottleItem(Properties properties) {
        super(properties, 250);
    }
//    @Override
//    public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
//        List<Fluid> fluids = new ArrayList<>();
//        fluids.add(FHFluids.PURIFIED_WATER.get());
//        if (this.allowdedIn(tab) && this == ItemRegistry.FLUID_BOTTLE.get()) {
//            for (Fluid fluid : fluids) {
//                ItemStack itemStack = new ItemStack(ItemRegistry.FLUID_BOTTLE.get());
//                items.add(FluidHelper.fillContainer(itemStack, fluid));
//            }
//        }
//    }

    @Override
    public Component getName(ItemStack stack) {
        IFluidHandlerItem fluidHandlerItem =  stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        FluidStack fluidStack = fluidHandlerItem.getFluidInTank(0);
        if (fluidStack.isEmpty()) return super.getName(stack);

        Component component = fluidStack.getDisplayName();
        return component.copy().append(Component.translatable("item.frostedheart.fluid_bottle"));
    }

    @Override
    public ItemStack getContainerItem(ItemStack itemStack) {
        return new ItemStack(FHItems.fluid_bottle.get());
    }

}
