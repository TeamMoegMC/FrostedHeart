package com.teammoeg.frostedheart.content.water.item;

import com.teammoeg.frostedheart.bootstrap.common.FHItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class WoodenCupItem extends DrinkContainerItem {
    public WoodenCupItem(Properties properties, int capacity) {
        super(properties, capacity);
    }
//    @Override
//    public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
//        List<Fluid> fluids = new ArrayList<>();
//        fluids.add(FHFluids.PURIFIED_WATER.get());
//        fluids.add(Fluids.WATER);
//        if (this.allowdedIn(tab) && this == ItemRegistry.WOODEN_CUP_DRINK.get()) {
//            for (Fluid fluid : fluids) {
//                ItemStack itemStack = new ItemStack(ItemRegistry.WOODEN_CUP_DRINK.get());
//                items.add(FluidHelper.fillContainer(itemStack, fluid));
//            }
//            items.add(new ItemStack(ItemRegistry.WOODEN_CUP.get()));
//        }
//    }
    @Override
    public Component getName(ItemStack stack) {
        IFluidHandlerItem fluidHandlerItem =  stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        FluidStack fluidStack = fluidHandlerItem.getFluidInTank(0);
        if (fluidStack.isEmpty()) return super.getName(stack);

        Component component = fluidStack.getDisplayName();
        return component.copy().append(Component.translatable("item.frostedheart.wooden_cup_drink"));
    }

    @Override
    public ItemStack getContainerItem(ItemStack itemStack) {
        return new ItemStack(FHItems.wooden_cup.get());
    }

    @Override
    public ItemStack getDrinkItem() {
        return new ItemStack(FHItems.wooden_cup_drink.get());
    }
}
