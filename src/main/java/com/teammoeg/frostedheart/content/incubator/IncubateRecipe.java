package com.teammoeg.frostedheart.content.incubator;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.RegistryObject;

import java.util.Collections;
import java.util.Map;

public class IncubateRecipe extends IESerializableRecipe {
    public static IRecipeType<IncubateRecipe> TYPE;
    public static RegistryObject<IncubateRecipeSerializer> SERIALIZER;

    public IngredientWithSize input;
    public IngredientWithSize catalyst;
    public ItemStack output;
    public FluidStack output_fluid;
    public boolean consume_catalyst;
    public int water;
    public int time;



    public IncubateRecipe(ResourceLocation id, IngredientWithSize input,
			IngredientWithSize catalyst, ItemStack output, FluidStack output_fluid, boolean consume_catalyst, int water,
			int time) {
		super(output,TYPE, id);
		this.input = input;
		this.catalyst = catalyst;
		this.output = output;
		this.output_fluid = output_fluid;
		this.consume_catalyst = consume_catalyst;
		this.water = water;
		this.time = time;
	}

	@Override
    protected IERecipeSerializer<IncubateRecipe> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return this.output;
    }

    public static Map<ResourceLocation, IncubateRecipe> recipeList = Collections.emptyMap();
    public static IncubateRecipe findRecipe(ItemStack in,ItemStack catalyst) {
    	return recipeList.values().stream().filter(t->t.input.test(in)).filter(t->t.catalyst==null||t.catalyst.test(catalyst)).findAny().orElse(null);
    }
}
