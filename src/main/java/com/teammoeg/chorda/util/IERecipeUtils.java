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

package com.teammoeg.chorda.util;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.teammoeg.chorda.Chorda;

/**
 * Immersive Engineering配方相关的工具类，提供检查玩家背包物品、扣除物品消耗、
 * 创建带数量配料等功能。
 * <p>
 * Immersive Engineering recipe utility class providing player inventory item checking,
 * item cost deduction, and ingredient-with-size creation.
 */
public class IERecipeUtils {
    /**
     * 检查玩家背包中各配料的满足情况，返回一个BitSet表示每个配料是否足够。
     * <p>
     * Check the fulfillment status of each ingredient in the player's inventory,
     * returning a BitSet indicating whether each ingredient is satisfied.
     *
     * @param player 玩家 / the player
     * @param costList 消耗列表 / the cost list
     * @return 表示每个配料是否足够的BitSet / a BitSet indicating whether each ingredient is sufficient
     */
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

    /**
     * 从玩家背包中扣除指定的物品消耗，先验证后扣除。如果扣除过程中出错则回滚。
     * <p>
     * Deduct the specified item costs from the player's inventory. Validates first,
     * then deducts. Rolls back if an error occurs during deduction.
     *
     * @param player 玩家 / the player
     * @param costList 消耗列表 / the cost list
     * @return 是否成功扣除所有物品 / whether all items were successfully deducted
     */
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

    /**
     * 从物品堆创建带数量的配料。
     * <p>
     * Create an IngredientWithSize from an ItemStack.
     *
     * @param is 物品堆 / the item stack
     * @return 带数量的配料 / the ingredient with size
     */
    public static IngredientWithSize createIngredientWithSize(ItemStack is) {
        return new IngredientWithSize(CUtils.createIngredient(is), is.getCount());
    }

    /**
     * 从标签和数量创建带数量的配料。
     * <p>
     * Create an IngredientWithSize from a tag and count.
     *
     * @param tag 标签资源位置 / the tag resource location
     * @param count 数量 / the count
     * @return 带数量的配料 / the ingredient with size
     */
    public static IngredientWithSize createIngredientWithSize(ResourceLocation tag, int count) {
        return new IngredientWithSize(CUtils.createIngredient(tag), count);
    }

    /**
     * 检查玩家是否拥有指定消耗列表中的所有物品。
     * <p>
     * Check if the player has all items in the specified cost list.
     *
     * @param player 玩家 / the player
     * @param costList 消耗列表 / the cost list
     * @return 是否拥有所有物品 / whether the player has all required items
     */
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
