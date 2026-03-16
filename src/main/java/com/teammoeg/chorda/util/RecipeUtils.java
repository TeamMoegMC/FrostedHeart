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

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.Chorda;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 原版配方相关的工具类，提供使用Ingredient配料来检查和扣除玩家背包物品的功能。
 * <p>
 * Vanilla recipe utility class providing player inventory item checking
 * and deduction using standard Ingredient.
 */
public class RecipeUtils {

	private RecipeUtils() {
	}
    /**
     * 从玩家背包中扣除指定的物品消耗列表，先验证后扣除。如果扣除过程中出错则回滚。
     * <p>
     * Deduct the specified item costs from the player's inventory.
     * Validates first, then deducts. Rolls back if an error occurs during deduction.
     *
     * @param player 玩家 / the player
     * @param costList 配料和数量的配对列表 / the list of ingredient-count pairs
     * @return 是否成功扣除所有物品 / whether all items were successfully deducted
     */
    public static boolean costItems(Player player, List<Pair<Ingredient,Integer>> costList) {
        // first do simple verify
        for (Pair<Ingredient, Integer> iws : costList) {
            int count = iws.getSecond();
            for (ItemStack it : player.getInventory().items) {
                if (iws.getFirst().test(it)) {
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
        for (Pair<Ingredient, Integer> iws : costList) {
            int count = iws.getSecond();
           // System.out.println("require "+iws.getBaseIngredient().getItems()[0].getHoverName().getString()+" x "+count);
            for (int i=0;i<player.getInventory().items.size();i++) {
            	ItemStack it=player.getInventory().items.get(i);
                if (iws.getFirst().test(it)) {
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


}
