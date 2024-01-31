package com.teammoeg.frostedheart.content.foods.DailyKitchen;


import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.diet.api.DietCapability;
import top.theillusivec4.diet.api.IDietTracker;
import top.theillusivec4.diet.common.capability.DietTrackerCapability;

public class DailyKitchen {
    @CapabilityInject(IWantedFoodCapability.class)
    public static Capability<IWantedFoodCapability> WANTED_FOOD_CAPABILITY;

    public static void setupWantedFoodCapability(){
        CapabilityManager.INSTANCE.register(IWantedFoodCapability.class,
                new Capability.IStorage<IWantedFoodCapability>() {
                    @Nullable
                    @Override
                    public INBT writeNBT(Capability<IWantedFoodCapability> capability, IWantedFoodCapability instance, Direction side) {
                        return instance.serializeNBT();
                    }

                    @Override
                    public void readNBT(Capability<IWantedFoodCapability> capability, IWantedFoodCapability instance, Direction side, INBT nbt) {
                        instance.deserializeNBT((CompoundNBT)nbt);
                    }
                },
                () -> null
        );
    }



    /**
     * This function generates 1-3 foods that player wants to eat.It should be called once every morning(in frostedheart.events.PlayerEvents.sendForecastMessages).
     * It records how many kinds of foods the player have eaten in wantedFoodCapability(It seems that diet mod doesn't record this), eatenFoodsAmount WON'T be changed until this function is called again. So the player will get same effect in one day.
     */
    public static void generateWantedFood(PlayerEntity player){
        LazyOptional<IDietTracker> dietTracker = DietCapability.get(player);
        if(!dietTracker.isPresent()){
            return;
        }
        WantedFoodCapability wantedFoodCapability = (WantedFoodCapability)player.getCapability(WANTED_FOOD_CAPABILITY).orElse(new WantedFoodCapability());
        Set<Item> foodsEaten = DietCapability.get(player).orElse(new DietTrackerCapability.EmptyDietTracker()).getEaten();
        /*为了避免重复，每日厨房从diet存储的数据中获取foodsEaten。
        但若diet存储的foodsEaten会在死亡时候清空的话，则此处获取的foodsEaten也会是空的。
        在runClient测试中死亡会清空包括foodsEaten在内的所有数据(在TWR环境中不应如此)，此处暂且保留。
        *如果在整合包测试中也存在死亡后清空foodsEaten数据的话，则需要在fh里面保存foodsEaten数据
        艹了，为什么我的build文件夹里没有libs文件夹*/

        int eatenFoodsAmount = foodsEaten.size();
        int wantedFoodsAmount = Math.min(eatenFoodsAmount / 10, 3);
        if(wantedFoodsAmount==0) return;

        wantedFoodCapability.setEatenFoodsAmount(eatenFoodsAmount);

        WantedFoodsGenerator generator = new WantedFoodsGenerator(foodsEaten, eatenFoodsAmount);

        wantedFoodCapability.setWantedFoods(generator.generate());
        player.sendStatusMessage(generator.getWantedFoodsText(), false);
    }


    public static void tryGiveBenefits(ServerPlayerEntity player, Item food){
        Benefits benefits = new Benefits(player);
        benefits.tryGive(food);
    }

     //copy data from old capability to new capability
    public static void copyData(LazyOptional<IWantedFoodCapability> oldCapability, LazyOptional<IWantedFoodCapability> newCapability){
        newCapability.ifPresent((newCap) -> oldCapability.ifPresent((oldCap) -> newCap.deserializeNBT(oldCap.serializeNBT())));
    }
}




