package com.teammoeg.frostedheart.common.recipe;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.utils.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

import java.util.Collections;
import java.util.Map;

public class GeneratorRecipe extends IESerializableRecipe {
    public static IRecipeType<GeneratorRecipe> TYPE;
    public static RegistryObject<IERecipeSerializer<GeneratorRecipe>> SERIALIZER;

    public IngredientWithSize input;
    public ItemStack output;
    public int time;

    public GeneratorRecipe(ResourceLocation id, ItemStack output, IngredientWithSize input, int time) {
        super(output, TYPE, id);
        this.output = output;
        this.input = input;
        this.time = time;
    }

    public void setOverdriveMode() {
        int originalOutput = this.output.getCount();
        int originalInput = this.input.getCount();
        this.output.setCount(originalOutput * 2);
        this.input = this.input.withSize(originalInput * 2);
        this.time *= 2;
    }

    @Override
    protected IERecipeSerializer getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return this.output;
    }

    // Initialized by reload listener
    public static Map<ResourceLocation, GeneratorRecipe> recipeList = Collections.emptyMap();

    public static GeneratorRecipe findRecipe(ItemStack input) {
        for (GeneratorRecipe recipe : recipeList.values())
            if (ItemUtils.stackMatchesObject(input, recipe.input))
                return recipe;
        return null;
    }
}
