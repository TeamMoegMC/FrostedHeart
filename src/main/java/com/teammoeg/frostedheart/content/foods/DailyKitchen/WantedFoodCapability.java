package com.teammoeg.frostedheart.content.foods.DailyKitchen;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.registries.ForgeRegistries;

public class WantedFoodCapability implements IWantedFoodCapability{

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

    @Override
    public void setWantedFoods(Set<Item> wantedFoods){
        this.wantedFoods = wantedFoods;
        resetEatenTimes();
    }

    @Override
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
        return StringNBT.valueOf(Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)).toString());
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        ListNBT list = new ListNBT();
        for(Item item: this.wantedFoods){
            list.add(turnItemToStringNBT(item));
        }
        nbt.put(key_wantedFoods, list);
        nbt.put(key_eatenFoodsAmount, IntNBT.valueOf(this.eatenFoodsAmount));
        nbt.put(key_eatenTimes, IntNBT.valueOf((this.eatenTimes)));

        //FHMain.LOGGER.info("WantedFoodCapability serialized!");
        return nbt;
    }

    private static Item turnStringNBTToItem(INBT nbt){
        ResourceLocation itemResourceLocation = new ResourceLocation(nbt.getString());
        return ForgeRegistries.ITEMS.getValue(itemResourceLocation);
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        wantedFoods.clear();
        ListNBT list = nbt.getList(key_wantedFoods, Constants.NBT.TAG_STRING/*9*/);
        this.eatenFoodsAmount = nbt.getInt(key_eatenFoodsAmount);
        this.eatenTimes = nbt.getInt(key_eatenTimes);
        for(INBT itemNBT : list){
            wantedFoods.add(turnStringNBTToItem(itemNBT));
        }
        //FHMain.LOGGER.info("WantedFoodCapability deserialized!");
    }
}
