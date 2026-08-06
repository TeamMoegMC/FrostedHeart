/*
 * Copyright (c) 2026 TeamMoeg
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

import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.chorda.util.CRegistryHelper;

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
    /** 上次生成想吃的菜的世界日索引（-1 表示从未生成过）/ world day index of the last wanted foods generation (-1 = never generated) */
    private int lastGeneratedDay = -1;
    private final String key_wantedFoods = "wantedFoods";
    private final String key_foodsEaten = "foodsEaten";
    private final String key_eatenFoodsAmount = "eatenFoodsAmount";
    private final String key_eatenTimes = "key_eatenTimes";
    private final String key_lastGeneratedDay = "lastGeneratedDay";


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
        ListTag eatenList = new ListTag();
        for(Item item: this.foodsEaten){
            eatenList.add(turnItemToStringNBT(item));
        }
        nbt.put(key_foodsEaten, eatenList);
        nbt.put(key_eatenFoodsAmount, IntTag.valueOf(this.eatenFoodsAmount));
        nbt.put(key_eatenTimes, IntTag.valueOf((this.eatenTimes)));
        nbt.putInt(key_lastGeneratedDay, this.lastGeneratedDay);

	}

	@Override
	public void load(CompoundTag nbt, boolean isPacket) {
        wantedFoods.clear();
        ListTag list = nbt.getList(key_wantedFoods, Tag.TAG_STRING/*9*/);
        this.eatenFoodsAmount = nbt.getInt(key_eatenFoodsAmount);
        this.eatenTimes = nbt.getInt(key_eatenTimes);
        this.lastGeneratedDay = nbt.getInt(key_lastGeneratedDay);
        for(Tag itemNBT : list){
            wantedFoods.add(turnStringNBTToItem(itemNBT));
        }
        foodsEaten.clear();
        ListTag eatenList = nbt.getList(key_foodsEaten, Tag.TAG_STRING);
        for(Tag itemNBT : eatenList){
            Item item = turnStringNBTToItem(itemNBT);
            if (item != null) {//物品已不存在时跳过，避免空项污染候选池 / skip vanished items to avoid polluting the candidate pool
                foodsEaten.add(item);
            }
        }
	}

    public Set<Item> getFoodsEaten() {
		return foodsEaten;
	}

	/**
	 * 记录玩家吃过的一种食物（仅可食用的物品，且自动按物品去重）。
	 * <p>
	 * Records one kind of food the player has eaten. Only edible items are recorded
	 * and duplicates (same Item) are naturally eliminated by the underlying Set,
	 * so the collection is a set of distinct eaten food kinds.
	 *
	 * @param food 吃下的物品 / the item that was eaten
	 */
	public void addEatenFood(Item food) {
		if (food != null && food.isEdible()) {
			foodsEaten.add(food);
		}
	}

	/**
	 * 从另一个能力实例复制全部数据（用于玩家复活后保留每日厨房记录）。
	 * <p>
	 * Copies all data from another capability instance (used to preserve
	 * daily kitchen records after the player respawns).
	 *
	 * @param other 数据来源的能力实例 / the capability instance to copy from
	 */
	public void copyFrom(WantedFoodCapability other) {
		if (other == null) return;
		this.wantedFoods.clear();
		this.wantedFoods.addAll(other.wantedFoods);
		this.foodsEaten.clear();
		this.foodsEaten.addAll(other.foodsEaten);
		this.eatenTimes = other.eatenTimes;
		this.eatenFoodsAmount = other.eatenFoodsAmount;
		this.lastGeneratedDay = other.lastGeneratedDay;
	}

	/**
	 * 获取上次生成想吃的菜的世界日索引。
	 * <p>
	 * Gets the world day index of the last wanted foods generation.
	 *
	 * @return 世界日索引；-1 表示从未生成过 / the world day index, or -1 if never generated
	 */
	public int getLastGeneratedDay() {
		return lastGeneratedDay;
	}

	/**
	 * 记录上次生成想吃的菜的世界日索引。
	 * <p>
	 * Records the world day index of the last wanted foods generation.
	 *
	 * @param day 世界日索引 / the world day index
	 */
	public void setLastGeneratedDay(int day) {
		this.lastGeneratedDay = day;
	}
}
