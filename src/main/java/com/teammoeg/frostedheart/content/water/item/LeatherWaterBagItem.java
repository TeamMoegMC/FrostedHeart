package com.teammoeg.frostedheart.content.water.item;

import com.teammoeg.frostedheart.FHItems;
import net.minecraft.world.item.ItemStack;

public class LeatherWaterBagItem extends DurableDrinkContainerItem{
    public LeatherWaterBagItem( Properties properties, int capacity) {
        super(properties, capacity);
    }

    @Override
    public ItemStack getContainerItem(ItemStack itemStack) {
        return new ItemStack(FHItems.LEATHER_WATER_BAG.get());
    }

    @Override
    public ItemStack getDrinkItem() {
        return new ItemStack(FHItems.LEATHER_WATER_BAG.get());
    }
}
