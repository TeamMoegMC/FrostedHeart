package com.teammoeg.frostedheart.content.foods.DailyKitchen;

import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

public interface IWantedFoodCapability extends INBTSerializable<CompoundNBT> {
    Set<Item> getWantedFoods();
    void setWantedFoods(Set<Item> wantedFoods);
}
