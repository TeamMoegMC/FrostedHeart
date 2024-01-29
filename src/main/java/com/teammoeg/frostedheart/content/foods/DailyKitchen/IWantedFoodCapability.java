package com.teammoeg.frostedheart.content.foods.DailyKitchen;

import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Set;

public interface IWantedFoodCapability extends INBTSerializable<CompoundNBT> {
    Set<Item> getWantedFoods();
    void setWantedFoods(Set<Item> wantedFoods);
}
