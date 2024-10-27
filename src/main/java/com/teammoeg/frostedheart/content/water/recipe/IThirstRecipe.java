package com.teammoeg.frostedheart.content.water.recipe;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public interface IThirstRecipe extends Recipe<Inventory> {
    boolean conform(ItemStack stack);

    float getProbability();

    int getDuration();

    int getAmplifier();

    Ingredient getIngredient();
}
