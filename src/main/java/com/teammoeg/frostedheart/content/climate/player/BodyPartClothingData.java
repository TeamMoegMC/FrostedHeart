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

package com.teammoeg.frostedheart.content.climate.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Collection;

import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;

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

    float getThermalConductivity(EquipmentSlot slot,ItemStack equipment) {
        float res=0f;
        float rate=0.4f;
        //if(equipment.getItem() instanceof FHBaseClothesItem) {
            res += rate * sumAttributes(equipment.getAttributeModifiers(slot).get(FHAttributes.INSULATION.get()));
            rate -= 0.1f;
        //}
        for(ItemStack it : this.clothes) {
            if(!it.isEmpty()) {
                res += rate * sumAttributes(it.getAttributeModifiers(slot).get(FHAttributes.INSULATION.get()));
                rate -= 0.1f;
            }
        }
        return 100/(100+res);
    }

    float getWindResistance(EquipmentSlot slot,ItemStack equipment) {
    	double res=0f;
        float rate=0.3f-this.clothes.length*0.1f;
        //if(equipment.getItem() instanceof FHBaseClothesItem) {
            rate += 0.1f;
            res += rate * sumAttributes(equipment.getAttributeModifiers(slot).get(FHAttributes.WIND_PROOF.get()));
        //}
        for(ItemStack it : this.clothes) {
            if(!it.isEmpty()) {
                rate += 0.1f;
                res += rate * sumAttributes(it.getAttributeModifiers(slot).get(FHAttributes.WIND_PROOF.get()));
            }
        }
        return (float) res;
    }
    public double sumAttributes(Collection<AttributeModifier> attribute) {
    	double base=0;
    	double mbase=1;
    	double mtotal=1;
    	for(AttributeModifier attrib:attribute) {
    		if(attrib.getOperation()==Operation.ADDITION)
    			base+=attrib.getAmount();
    		if(attrib.getOperation()==Operation.MULTIPLY_BASE)
    			mbase*=attrib.getAmount();
    		if(attrib.getOperation()==Operation.MULTIPLY_TOTAL)
    			mtotal*=attrib.getAmount();
    	}
    	return base*mbase*mtotal;
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
