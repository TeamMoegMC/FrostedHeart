package com.teammoeg.chorda.util.ie;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.teammoeg.chorda.util.CUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class IngredientUtils {
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
            for (ItemStack it : player.getInventory().items) {
                if (iws.testIgnoringSize(it)) {
                    int redcount = Math.min(count, it.getCount());
                    ret.add(it.split(redcount));
                    count -= redcount;
                    if (count <= 0)
                        break;
                }
            }
            if (count > 0) {// wrong, revert.
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
