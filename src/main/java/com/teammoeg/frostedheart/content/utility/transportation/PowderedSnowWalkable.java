package com.teammoeg.frostedheart.content.utility.transportation;

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;


public class PowderedSnowWalkable extends ArmorItem {
    public PowderedSnowWalkable(ArmorMaterial materialIn, Type slot, Properties builderIn) {
        super(materialIn, slot, builderIn);
    }

    @Override
    public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer) {
        return super.canWalkOnPowderedSnow(stack, wearer)
                || stack.is(FHTags.Items.POWDERED_SNOW_WALKABLE.tag);
    }
}
