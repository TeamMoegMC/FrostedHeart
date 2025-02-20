package com.teammoeg.chorda.util;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.frostedheart.FHMain;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class RecipeUtils {

	private RecipeUtils() {
	}
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
            	FHMain.LOGGER.error("cost item can not be consumed successfully, this is unusual, consider cheat or data issues");
                for (ItemStack it : ret)
                    CUtils.giveItem(player, it);
                return false;
            }
        }
        return true;
    }


}
