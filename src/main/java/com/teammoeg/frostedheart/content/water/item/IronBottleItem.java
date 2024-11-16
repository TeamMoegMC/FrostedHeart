package com.teammoeg.frostedheart.content.water.item;


import com.teammoeg.frostedheart.FHItems;
import net.minecraft.world.item.ItemStack;

public class IronBottleItem extends DurableDrinkContainerItem{
    public IronBottleItem(Properties properties, int capacity) {
        super(properties, capacity);
    }

    @Override
    public ItemStack getContainerItem(ItemStack itemStack) {
        return new ItemStack(FHItems.IRON_BOTTLE.get());
    }

    @Override
    public ItemStack getDrinkItem() {
        return new ItemStack(FHItems.IRON_BOTTLE.get());
    }
}
