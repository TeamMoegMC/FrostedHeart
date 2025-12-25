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

package com.teammoeg.chorda.util;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.teammoeg.chorda.Chorda;

public class IERecipeUtils {
    public static BitSet checkItemList(Player player, List<IngredientWithSize> costList) {
    	BitSet bs=new BitSet(costList.size());
    	int i=0;
        for (IngredientWithSize iws : costList) {
            int count = iws.getCount();
            for (ItemStack it : player.getInventory().items) {
                if (iws.testIgnoringSize(it)) {
                    count -= it.getCount();
                    if (count <= 0)
                        break;
                }
            }
            if (count > 0) {
            	bs.set(i++,false);
            } else {
            	bs.set(i++, true);
            }
        }
        return bs;
    }

    public static boolean costItems(Player player, List<IngredientWithSize> costList) {
        // first do simple verify
        for (IngredientWithSize iws : costList) {
            int count = iws.getCount();
            for (ItemStack it : player.getInventory().items) {
                if (iws.testIgnoringSize(it)) {
                    count -= it.getCount();
                    if (count <= 0)
                        break;
                }
            }
            if (count > 0)
                return false;
        }
        //System.out.println("test");
        // then really consume item
        List<ItemStack> ret = new ArrayList<>();
        for (IngredientWithSize iws : costList) {
            int count = iws.getCount();
           // System.out.println("require "+iws.getBaseIngredient().getItems()[0].getHoverName().getString()+" x "+count);
            for (int i=0;i<player.getInventory().items.size();i++) {
            	ItemStack it=player.getInventory().items.get(i);
                if (iws.testIgnoringSize(it)) {
                    int redcount = Math.min(count, it.getCount());
                    ret.add(it.split(redcount));
                   // System.out.println(splited);
                   // System.out.println(it);
                    count -= redcount;
                    if (count <= 0)
                        break;
                }
            }
            if (count > 0) {// wrong, revert.
            	Chorda.LOGGER.error("cost item can not be consumed successfully, this is unusual, consider cheat or data issues");
                for (ItemStack it : ret)
                    CUtils.giveItem(player, it);
                return false;
            }
        }
        return true;
    }

    public static IngredientWithSize createIngredientWithSize(ItemStack is) {
        return new IngredientWithSize(CUtils.createIngredient(is), is.getCount());
    }

    public static IngredientWithSize createIngredientWithSize(ResourceLocation tag, int count) {
        return new IngredientWithSize(CUtils.createIngredient(tag), count);
    }

    public static boolean hasItems(Player player,List<IngredientWithSize> costList) {
    	int i=0;
        for (IngredientWithSize iws : costList) {
            int count = iws.getCount();
            for (ItemStack it : player.getInventory().items) {
                if (iws.testIgnoringSize(it)) {
                    count -= it.getCount();
                    if (count <= 0)
                        break;
                }
            }
            if (count > 0) {
            	return false;
            }
        }
        return true;
    }
}
