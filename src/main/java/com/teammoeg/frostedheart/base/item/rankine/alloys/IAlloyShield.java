package com.teammoeg.frostedheart.base.item.rankine.alloys;

import com.teammoeg.frostedheart.base.item.rankine.init.Config;
import com.teammoeg.frostedheart.base.item.rankine.init.RankineEnchantments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface IAlloyShield extends IAlloyItem {
    @Override
    default void createAlloyNBT(ItemStack stack, Level worldIn, String composition, @Nullable ResourceLocation alloyRecipe, @Nullable String nameOverride) {
        if (stack.getTag() != null && stack.getTag().getBoolean("RegenerateAlloy")) {
            stack.getTag().remove("RegenerateAlloy");
        }
        ListTag alloyData = IAlloyItem.getAlloyNBT(stack);
        List<ElementRecipe> elements = this.getElementRecipes(composition,worldIn);
        List<Integer> percents = this.getPercents(composition);

        CompoundTag listnbt = new CompoundTag();
        int dur = 0;
        int ench = 0;
        float cr = 0;
        float hr = 0;
        float tough = 0;

        for (int i = 0; i < elements.size(); i++) {
            ElementRecipe element = elements.get(i);
            int percentage = percents.get(i);

            dur += element.getDurability(percentage);
            ench += element.getEnchantability(percentage);
            cr += element.getCorrosionResistance(percentage);
            hr += element.getHeatResistance(percentage);
            tough += element.getToughness(percentage);
        }



        if (alloyRecipe != null) {
            Optional<? extends Recipe<?>> opt = worldIn.getRecipeManager().byKey(alloyRecipe);
            if (opt.isPresent()) {
                OldAlloyingRecipe recipe = (OldAlloyingRecipe) opt.get();
                dur += recipe.getBonusDurability();
                ench += recipe.getBonusEnchantability();
                cr += recipe.getBonusCorrosionResistance();
                hr += recipe.getBonusHeatResistance();
                tough += recipe.getBonusToughness();
            }
        }

        dur = Math.max(1,dur);
        ench = Math.max(0,ench);
        cr = Math.min(Math.max(0,cr),1);
        hr = Math.min(Math.max(0,hr),1);
        tough = Math.min(Math.max(-1,tough),1);
        listnbt.putString("comp",composition);
        if (alloyRecipe != null) {
            listnbt.putString("recipe",alloyRecipe.toString());
        }
        listnbt.putInt("durability",dur);
        listnbt.putInt("enchantability",ench);
        listnbt.putFloat("corrResist",Math.round(cr*100)/100f);
        listnbt.putFloat("heatResist",Math.round(hr*100)/100f);
        listnbt.putFloat("toughness",Math.round(tough*100)/100f);
        alloyData.add(listnbt);
        stack.getOrCreateTag().put("StoredAlloy", listnbt);

        if (nameOverride != null && stack.getTag() != null) {
            stack.getTag().putString("nameOverride",nameOverride);
        }
    }

    default void applyAlloyEnchantments(ItemStack stack, Level worldIn) {
        int start = 10;
        int interval = 5;
        int maxLvl = 5;
        ResourceLocation rs = IAlloyItem.getAlloyRecipe(stack);
        if (rs != null && worldIn.getRecipeManager().byKey(rs).isPresent()) {
            OldAlloyingRecipe recipe = (OldAlloyingRecipe) worldIn.getRecipeManager().byKey(rs).get();
            start = recipe.getMinEnchantability();
            interval = recipe.getEnchantInterval();
            maxLvl = recipe.getMaxEnchantLevelIn();
            for (Enchantment e: AlloyEnchantmentUtils.getAlloyEnchantments(recipe,stack,worldIn))
            {
                int enchLvl = Math.min(Math.floorDiv(Math.max(getAlloyEnchantability(stack) - start + interval,0),interval),maxLvl);
                if (enchLvl > 0 && EnchantmentHelper.getItemEnchantmentLevel(e,stack) == 0) {
                    stack.enchant(e,Math.min(e.getMaxLevel(),enchLvl));
                }
            }
        }
        for (Enchantment e: AlloyEnchantmentUtils.getElementEnchantments(getElementRecipes(IAlloyItem.getAlloyComposition(stack),worldIn),getPercents(IAlloyItem.getAlloyComposition(stack)),stack,worldIn))
        {
            int enchLvl = Math.min(Math.floorDiv(Math.max(getAlloyEnchantability(stack) - start + interval,0),interval),maxLvl);
            if (enchLvl > 0 && EnchantmentHelper.getItemEnchantmentLevel(e,stack) == 0) {
                stack.enchant(e,Math.min(e.getMaxLevel(),enchLvl));
            }
        }

    }

    default int getAlloyDurability(ItemStack stack)
    {
        if (stack.getTag() != null) {
            return stack.getTag().getCompound("StoredAlloy").getInt("durability");
        } else {
            return 1;
        }

    }

    default int getAlloyEnchantability(ItemStack stack) {
        if (stack.getTag() != null) {
            return stack.getTag().getCompound("StoredAlloy").getInt("enchantability");
        } else {
            return 1;
        }
    }

    default float getCorrResist(ItemStack stack)
    {
        if (!Config.ALLOYS.ALLOY_CORROSION.get())
        {
            return 100;
        }
        if (stack.getTag() != null) {
            return stack.getTag().getCompound("StoredAlloy").getFloat("corrResist");
        } else {
            return 0;
        }

    }


    default float getHeatResist(ItemStack stack)
    {
        if (!Config.ALLOYS.ALLOY_HEAT.get())
        {
            return 100;
        }
        if (stack.getTag() != null) {
            return stack.getTag().getCompound("StoredAlloy").getFloat("heatResist");
        } else {
            return 0;
        }
    }

    default float getToughness(ItemStack stack)
    {
        if (!Config.ALLOYS.ALLOY_TOUGHNESS.get())
        {
            return 0;
        }
        if (stack.getTag() != null) {
            return stack.getTag().getCompound("StoredAlloy").getFloat("toughness");
        } else {
            return 0;
        }
    }

    default int calcDurabilityLoss(ItemStack stack, Level worldIn, LivingEntity entityLiving, boolean isEfficient)
    {
        boolean memory = false;
        RandomSource rand = worldIn.getRandom();
        int i = 1;
        if (rand.nextFloat() > getHeatResist(stack) && (entityLiving.isInLava() || entityLiving.getRemainingFireTicks() > 0 || worldIn.dimension() == Level.NETHER)) {
            i += Config.ALLOYS.ALLOY_HEAT_AMT.get();
            memory = true;
        }
        if ((rand.nextFloat() > getCorrResist(stack) && entityLiving.isInWaterOrRain()))
        {
            i += Config.ALLOYS.ALLOY_CORROSION_AMT.get();
        }
        if (!isEfficient)
        {
            i *= 2;
        }

        if (memory && EnchantmentHelper.getItemEnchantmentLevel(RankineEnchantments.SHAPE_MEMORY.get(),stack) >= 1) {
            stack.setDamageValue(Math.max(stack.getDamageValue() - i,0));
            i = 0;
        }
        return i;
    }
}
