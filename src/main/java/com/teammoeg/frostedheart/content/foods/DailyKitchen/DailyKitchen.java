package com.teammoeg.frostedheart.content.foods.DailyKitchen;

import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.inspire.EnergyCore;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.LazyOptional;
import org.lwjgl.system.CallbackI;
import top.theillusivec4.diet.api.DietCapability;
import top.theillusivec4.diet.api.IDietTracker;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

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

    private static boolean isNotBadFood(Item food){
        Set<ResourceLocation> tags = food.getTags();
        for(ResourceLocation tag : tags){
            String path = tag.getPath();
            if(path.equals("raw_food") || path.equals("bad_food")) return false;
        }
        return true;
    }

    /**
     * This function generates 1-3 foods that player wants to eat.It should be called once every morning(in frostedheart.events.PlayerEvents.sendForecastMessages).
     * It records how many kinds of foods the player have eaten in wantedFoodCapability(It seems that diet mod doesn't record this), eatenFoodsAmount WON'T be changed until this function is called again. So the player will get same effect in one day.
     * @param player
     */
    public static void generateWantedFood(PlayerEntity player){
        LazyOptional<IDietTracker> dietTracker = DietCapability.get(player);
        if(!dietTracker.isPresent()){
            return;
        }
        WantedFoodCapability wantedFoodCapability = (WantedFoodCapability)player.getCapability(WANTED_FOOD_CAPABILITY).orElse(null);
        Set<Item> foodsEaten = DietCapability.get(player).orElse(null).getEaten();
        /*为了避免重复，每日厨房从diet存储的数据中获取foodsEaten。
        但若diet存储的foodsEaten会在死亡时候清空的话，则此处获取的foodsEaten也会是空的。
        在runClient测试中死亡会清空包括foodsEaten在内的所有数据(在TWR环境中不应如此)，此处暂且保留。
        *如果在整合包测试中也存在死亡后清空foodsEaten数据的话，则需要在fh里面保存foodsEaten数据
        艹了，为什么我的build文件夹里没有libs文件夹*/

        int eatenFoodsAmount = foodsEaten.size();

        int wantedFoodsAmount = Math.min(eatenFoodsAmount / 10, 3);
        if(wantedFoodsAmount==0) return;
        wantedFoodCapability.setEatenFoodsAmount(eatenFoodsAmount);

        Random random = new Random();
        HashSet<Item> wantedFoods = new HashSet<>();
        ArrayList<Integer> wantedFoodsNumber = new ArrayList<>();
        for(int i=0; i<wantedFoodsAmount;){
            int randomNumber = random.nextInt(eatenFoodsAmount);
            if(!wantedFoodsNumber.contains(randomNumber)) {
                wantedFoodsNumber.add(randomNumber);
                i++;
            }
        }
        int i = 0;
        TextComponent wantedFoodsText = GuiUtils.translateMessage("wanted_foods");
        for(Item food :foodsEaten) {
            if(wantedFoodsNumber.contains(i) && (isNotBadFood(food)) ){
                wantedFoods.add(food);
                wantedFoodsText.appendSibling(food.getName()).appendSibling(new StringTextComponent(" "));
            }
            i++;
        }

        wantedFoodCapability.setWantedFoods(wantedFoods);
        player.sendStatusMessage(wantedFoodsText, false);
    }

    public static void tryGiveBenefits(ServerPlayerEntity player, Item food){
        Benefits benefits = new Benefits(player);
        benefits.tryGive(food);
    }

     //copy data from old capability to new capability
    public static void copyData(LazyOptional<IWantedFoodCapability> oldCapability, LazyOptional<IWantedFoodCapability> newCapability){
        newCapability.ifPresent((newCap) -> {
            oldCapability.ifPresent((oldCap) -> {
                newCap.deserializeNBT(oldCap.serializeNBT());
            });
        });

    }
}




