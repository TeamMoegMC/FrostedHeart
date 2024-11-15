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
import java.util.Set;

import com.teammoeg.frostedheart.FHCapabilities;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

public class DailyKitchen {
    /**
     * This function generates 1-3 foods that player wants to eat.It should be called once every morning(in frostedheart.events.PlayerEvents.sendForecastMessages).
     * It records how many kinds of foods the player have eaten in wantedFoodCapability(It seems that diet mod doesn't record this), eatenFoodsAmount WON'T be changed until this function is called again. So the player will get same effect in one day.
     */
    public static void generateWantedFood(Player player){
    	WantedFoodCapability wantedFoodCapability = FHCapabilities.WANTED_FOOD.getCapability(player).orElse(null);
       /* LazyOptional<IDietTracker> dietTracker = DietCapability.get(player);
        if(!dietTracker.isPresent()){
            return;
        }
        
        if(wantedFoodCapability==null)return;
        Set<Item> foodsEaten = DietCapability.get(player).map(IDietTracker::getEaten).orElseGet(HashSet::new);*/
        /*为了避免重复，每日厨房从diet存储的数据中获取foodsEaten。
        但若diet存储的foodsEaten会在死亡时候清空的话，则此处获取的foodsEaten也会是空的。
        在runClient测试中死亡会清空包括foodsEaten在内的所有数据(在TWR环境中不应如此)，此处暂且保留。
        *如果在整合包测试中也存在死亡后清空foodsEaten数据的话，则需要在fh里面保存foodsEaten数据
        艹了，为什么我的build文件夹里没有libs文件夹*/
    	//TODO 增加食物食用记录
        int eatenFoodsAmount = wantedFoodCapability.getFoodsEaten().size();
        int wantedFoodsAmount = Math.min(eatenFoodsAmount / 10, 3);
        if(wantedFoodsAmount==0) return;
        
        wantedFoodCapability.setEatenFoodsAmount(eatenFoodsAmount);

        WantedFoodsGenerator generator = new WantedFoodsGenerator(wantedFoodCapability.getFoodsEaten(), eatenFoodsAmount);

        wantedFoodCapability.setWantedFoods(generator.generate());
        player.displayClientMessage(generator.getWantedFoodsText(), false);
    }


    public static void tryGiveBenefits(LivingEntityUseItemEvent.Finish event){
        if (event.getEntity() != null && !event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer) {
            Benefits benefits = new Benefits((ServerPlayer) event.getEntity());
            benefits.tryGive(event.getItem());
        }

    }
}




