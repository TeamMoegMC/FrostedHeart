package com.teammoeg.frostedheart.recipe;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.utils.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

import java.util.Collections;
import java.util.Map;

public class CrucibleRecipe extends IESerializableRecipe {
    public static IRecipeType<CrucibleRecipe> TYPE;
    public static RegistryObject<IERecipeSerializer<CrucibleRecipe>> SERIALIZER;

    public final IngredientWithSize input;
    public final ItemStack output;
    public final int time;

    public CrucibleRecipe(ResourceLocation id, ItemStack output, IngredientWithSize input, int time) {
        super(output, TYPE, id);
        this.output = output;
        this.input = input;
        this.time = time;
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
    public static Map<ResourceLocation, CrucibleRecipe> recipeList = Collections.emptyMap();

    public static CrucibleRecipe findRecipe(ItemStack input) {
        for (CrucibleRecipe recipe : recipeList.values())
            if (ItemUtils.stackMatchesObject(input, recipe.input))
                return recipe;
        return null;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> nonnulllist = NonNullList.create();
        nonnulllist.add(this.input.getBaseIngredient());
        return nonnulllist;
    }
}
