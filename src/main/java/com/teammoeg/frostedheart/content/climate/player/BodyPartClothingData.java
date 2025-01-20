package com.teammoeg.frostedheart.content.climate.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

class BodyPartClothingData implements Container {
    String name;
    final ItemStack[] clothes;
    BodyPartClothingData(String name, int max_count) {
        this.name = name;
        this.clothes = new ItemStack[max_count];
        reset();
    }

    void set(ListTag itemsTag) {
        for (int i = 0; i < itemsTag.size() && i < clothes.length; i++) {
            clothes[i] = ItemStack.of((CompoundTag) itemsTag.get(i));
        }
    }

    void reset() {
        Arrays.fill(clothes, ItemStack.EMPTY);
    }

    float getThermalConductivity(ItemStack equipment) {
        float res=0f;
        float rate=0.4f;
        if(equipment.getItem() instanceof FHBaseClothesItem) {
            res += rate * ((FHBaseClothesItem) equipment.getItem()).getWarmthLevel();
            rate -= 0.1f;
        }
        for(ItemStack it : this.clothes) {
            if(!it.isEmpty()) {
                res += rate * ((FHBaseClothesItem) it.getItem()).getWarmthLevel();
                rate -= 0.1f;
            }
        }
        return 100/(100+res);
    }

    float getWindResistance(ItemStack equipment) {
        float res=0f;
        float rate=0.3f-this.clothes.length*0.1f;
        if(equipment.getItem() instanceof FHBaseClothesItem) {
            rate += 0.1f;
            res += rate * ((FHBaseClothesItem) equipment.getItem()).getWarmthLevel();
        }
        for(ItemStack it : this.clothes) {
            if(!it.isEmpty()) {
                rate += 0.1f;
                res += rate * ((FHBaseClothesItem) it.getItem()).getWindResistance();
            }
        }
        return res;
    }
    @Override
    public int getContainerSize() {
        return this.clothes.length;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : clothes) {
            if (!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        if (index >= 0 && index < clothes.length) {
            return clothes[index];
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index >= 0 && index < clothes.length && !clothes[index].isEmpty() && count > 0) {
            return clothes[index].split(count);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index >= 0 && index < clothes.length) {
            ItemStack item = clothes[index];
            clothes[index] = ItemStack.EMPTY;
            return item;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index >= 0 && index < clothes.length) {
            clothes[index] = stack;
        }
    }

    @Override
    public void setChanged() {
        // Placeholder for marking the container as changed.
    }

    @Override
    public boolean stillValid(Player player) {
        // Placeholder for determining if the player can still interact with the container.
        return true;
    }

    @Override
    public void clearContent() {
        reset();
    }
}
