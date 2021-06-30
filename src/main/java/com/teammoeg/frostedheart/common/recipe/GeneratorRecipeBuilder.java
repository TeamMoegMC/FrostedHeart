package com.teammoeg.frostedheart.common.recipe;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.crafting.builders.IEFinishedRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ITag;

public class GeneratorRecipeBuilder extends IEFinishedRecipe<GeneratorRecipeBuilder> {
    private GeneratorRecipeBuilder() {
        super(GeneratorRecipe.SERIALIZER.get());
    }

    public static GeneratorRecipeBuilder builder(Item result) {
        return new GeneratorRecipeBuilder().addResult(result);
    }

    public static GeneratorRecipeBuilder builder(ItemStack result) {
        return new GeneratorRecipeBuilder().addResult(result);
    }

    public static GeneratorRecipeBuilder builder(ITag<Item> result, int count) {
        return new GeneratorRecipeBuilder().addResult(new IngredientWithSize(result, count));
    }
}
