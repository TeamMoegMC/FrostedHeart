package com.teammoeg.frostedheart.content.incubator;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.teammoeg.frostedheart.content.generator.GeneratorRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.RegistryObject;

import java.util.Collections;
import java.util.Map;

public class IncubateRecipe extends IESerializableRecipe {
    public static IRecipeType<GeneratorRecipe> TYPE;
    public static RegistryObject<IncubateRecipeSerializer> SERIALIZER;

    public IngredientWithSize input;
    public IngredientWithSize catalyst;
    public ItemStack output;
    public FluidStack output_fluid;
    public boolean consume_catalyst;
    public int water;
    public int time;

    public IncubateRecipe(ResourceLocation id, IngredientWithSize input,
                          ItemStack output, FluidStack output_fluid, IngredientWithSize seed, float seed_conserve, int water,
                          int time) {
        super(ItemStack.EMPTY, TYPE, id);
        this.input = input;
        this.output = output;
        this.output_fluid = output_fluid;
        this.water = water;
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

    public static Map<ResourceLocation, IncubateRecipe> recipeList = Collections.emptyMap();

}
