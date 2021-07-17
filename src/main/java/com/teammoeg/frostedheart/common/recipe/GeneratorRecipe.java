package com.teammoeg.frostedheart.common.recipe;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
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

    public final ItemStack input;
    public final ItemStack output;
    public final int time;

    public GeneratorRecipe(ResourceLocation id, ItemStack output, ItemStack input, int time) {
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
    public static Map<ResourceLocation, GeneratorRecipe> recipeList = Collections.emptyMap();

    public static GeneratorRecipe findRecipe(ItemStack input) {
        for (GeneratorRecipe recipe : recipeList.values())
            if (ItemUtils.stackMatchesObject(input, recipe.input))
                return recipe;
        return null;
    }
}
