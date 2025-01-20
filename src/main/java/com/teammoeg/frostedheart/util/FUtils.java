package com.teammoeg.frostedheart.util;

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.world.item.ItemStack;

public class FUtils {
    public static ItemStack ArmorLiningNBT(ItemStack stack) {
        stack.getOrCreateTag().putString("inner_cover", FHMain.MODID + ":straw_lining");
        stack.getTag().putBoolean("inner_bounded", true);//bound lining to arm or
        return CUtils.ArmorNBT(stack, 107, 6);
    }
}
