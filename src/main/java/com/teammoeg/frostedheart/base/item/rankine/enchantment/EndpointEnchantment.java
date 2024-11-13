package com.teammoeg.frostedheart.base.item.rankine.enchantment;

import com.teammoeg.frostedheart.base.item.rankine.alloys.AlloySpearItem;
import com.teammoeg.frostedheart.base.item.rankine.alloys.IAlloyItem;
import com.teammoeg.frostedheart.base.item.rankine.init.RankineEnchantmentTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class EndpointEnchantment extends Enchantment {
    public EndpointEnchantment(Rarity p_i46721_1_, EquipmentSlot... p_i46721_2_) {
        super(p_i46721_1_, RankineEnchantmentTypes.ENDER_AMALGAM_SPEAR, p_i46721_2_);
    }

    public int getMinCost(int p_77321_1_) {
        return 10 + 7 * (p_77321_1_ - 1);
    }

    public int getMaxCost(int p_223551_1_) {
        return super.getMinCost(p_223551_1_) + 50;
    }

    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        ResourceLocation rs = IAlloyItem.getAlloyRecipe(stack);
        if (rs != null) {
            return rs.equals(new ResourceLocation("rankine:alloying/ender_amalgam_alloying")) && stack.getItem() instanceof AlloySpearItem;
        }
        return RankineEnchantmentTypes.ENDER_AMALGAM_SPEAR.canEnchant(stack.getItem());
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        ResourceLocation rs = IAlloyItem.getAlloyRecipe(stack);
        if (rs != null) {
            return rs.equals(new ResourceLocation("rankine:alloying/ender_amalgam_alloying")) && stack.getItem() instanceof AlloySpearItem;
        }
        return false;
    }

    @Override
    public boolean isDiscoverable() {
        return false;
    }

    @Override
    protected boolean checkCompatibility(Enchantment ench) {
        if (ench == Enchantments.LOYALTY) {
            return false;
        }
        return super.checkCompatibility(ench);
    }
}
