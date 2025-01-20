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

package com.teammoeg.frostedheart.content.health.dailykitchen;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.chorda.util.io.NBTSerializable;

import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;


public class WantedFoodCapability implements NBTSerializable{

    private Set<Item> wantedFoods = new HashSet<>();
    private Set<Item> foodsEaten= new HashSet<>();
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

    private static StringTag turnItemToStringNBT(Item item){
        return StringTag.valueOf(Objects.requireNonNull(CRegistryHelper.getRegistryName(item)).toString());
    }

    private static Item turnStringNBTToItem(Tag nbt){
        ResourceLocation itemResourceLocation = new ResourceLocation(nbt.getAsString());
        return CRegistryHelper.getItem(itemResourceLocation);
    }

	@Override
	public void save(CompoundTag nbt, boolean isPacket) {
        ListTag list = new ListTag();
        for(Item item: this.wantedFoods){
            list.add(turnItemToStringNBT(item));
        }
        nbt.put(key_wantedFoods, list);
        nbt.put(key_eatenFoodsAmount, IntTag.valueOf(this.eatenFoodsAmount));
        nbt.put(key_eatenTimes, IntTag.valueOf((this.eatenTimes)));

	}

	@Override
	public void load(CompoundTag nbt, boolean isPacket) {
        wantedFoods.clear();
        ListTag list = nbt.getList(key_wantedFoods, Tag.TAG_STRING/*9*/);
        this.eatenFoodsAmount = nbt.getInt(key_eatenFoodsAmount);
        this.eatenTimes = nbt.getInt(key_eatenTimes);
        for(Tag itemNBT : list){
            wantedFoods.add(turnStringNBTToItem(itemNBT));
        }
	}

	public Set<Item> getFoodsEaten() {
		return foodsEaten;
	}
}
