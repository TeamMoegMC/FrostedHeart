/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.foods.dailykitchen;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.io.NBTSerializable;

import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;


public class WantedFoodCapability implements NBTSerializable{

    private Set<Item> wantedFoods = new HashSet<>();
    private int eatenTimes = 0;
    private int eatenFoodsAmount = 0;
    private final String key_wantedFoods = "wantedFoods";
    private final String key_eatenFoodsAmount = "eatenFoodsAmount";
    private final String key_eatenTimes = "key_eatenTimes";


    public WantedFoodCapability(){
    }

    public WantedFoodCapability(Set<Item> wantedFoods){
        this.wantedFoods = wantedFoods;
        this.eatenTimes = 0;
    }
    public void setWantedFoods(Set<Item> wantedFoods){
        this.wantedFoods = wantedFoods;
        resetEatenTimes();
    }
    public Set<Item> getWantedFoods() {
        return this.wantedFoods;
    }

    public void setEatenFoodsAmount(int amount){
        if(amount >= 0) {
            this.eatenFoodsAmount = amount;
        }
    }

    public int getEatenFoodsAmount(){
        return this.eatenFoodsAmount;
    }

    public void resetEatenTimes(){
        this.eatenTimes = 0;
    }

    public void countEatenTimes(){
        eatenTimes++;
    }

    public int getEatenTimes(){
        return this.eatenTimes;
    }

    private static StringNBT turnItemToStringNBT(Item item){
        return StringNBT.valueOf(Objects.requireNonNull(RegistryUtils.getRegistryName(item)).toString());
    }

    private static Item turnStringNBTToItem(INBT nbt){
        ResourceLocation itemResourceLocation = new ResourceLocation(nbt.getAsString());
        return RegistryUtils.getItem(itemResourceLocation);
    }

	@Override
	public void save(CompoundNBT nbt, boolean isPacket) {
        ListNBT list = new ListNBT();
        for(Item item: this.wantedFoods){
            list.add(turnItemToStringNBT(item));
        }
        nbt.put(key_wantedFoods, list);
        nbt.put(key_eatenFoodsAmount, IntNBT.valueOf(this.eatenFoodsAmount));
        nbt.put(key_eatenTimes, IntNBT.valueOf((this.eatenTimes)));

	}

	@Override
	public void load(CompoundNBT nbt, boolean isPacket) {
        wantedFoods.clear();
        ListNBT list = nbt.getList(key_wantedFoods, Constants.NBT.TAG_STRING/*9*/);
        this.eatenFoodsAmount = nbt.getInt(key_eatenFoodsAmount);
        this.eatenTimes = nbt.getInt(key_eatenTimes);
        for(INBT itemNBT : list){
            wantedFoods.add(turnStringNBTToItem(itemNBT));
        }
	}
}
