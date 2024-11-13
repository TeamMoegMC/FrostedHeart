package com.teammoeg.frostedheart.base.item.rankine.init;

import com.teammoeg.frostedheart.base.item.rankine.KnifeItem;
import com.teammoeg.frostedheart.base.item.rankine.SpearItem;
import com.teammoeg.frostedheart.base.item.rankine.alloys.IAlloyTool;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class RankineEnchantmentTypes {
    public static EnchantmentCategory KNIFE = EnchantmentCategory.create("knife", (itemIn) -> {
        return itemIn instanceof KnifeItem; });

    public static EnchantmentCategory ENDER_AMALGAM_SPEAR = EnchantmentCategory.create("ender_spear", (itemIn) -> {
        return itemIn == RankineItems.ENDER_AMALGAM_SPEAR.get(); });

    public static EnchantmentCategory SPEAR = EnchantmentCategory.create("spear", (itemIn) -> {
        return itemIn instanceof SpearItem; });

    public static EnchantmentCategory ALLOYTOOL = EnchantmentCategory.create("alloytool", (itemIn) -> {
        return itemIn instanceof IAlloyTool && itemIn.canBeDepleted(); });
}
