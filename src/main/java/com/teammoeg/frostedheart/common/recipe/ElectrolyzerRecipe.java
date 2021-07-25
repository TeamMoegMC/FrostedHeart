package com.teammoeg.frostedheart.common.recipe;

import com.teammoeg.frostedheart.FHMain;
import electrodynamics.common.recipe.categories.fluiditem2fluid.FluidItem2FluidRecipe;
import electrodynamics.common.recipe.recipeutils.CountableIngredient;
import electrodynamics.common.recipe.recipeutils.FluidIngredient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.RegistryObject;

public class ElectrolyzerRecipe extends FluidItem2FluidRecipe {
    public static IRecipeType<ElectrolyzerRecipe> TYPE;
    public static RegistryObject<IRecipeSerializer<?>> SERIALIZER;
    public static final String RECIPE_GROUP = "electrolyzer_recipe";
    public static final ResourceLocation RECIPE_ID = new ResourceLocation(FHMain.MODID, RECIPE_GROUP);

    public ElectrolyzerRecipe(ResourceLocation recipeID, CountableIngredient inputItem, FluidIngredient inputFluid, FluidStack outputFluid, ItemStack output, FluidStack outputFluid2) {
        super(recipeID, inputItem, inputFluid, outputFluid);
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return Registry.RECIPE_TYPE.getOrDefault(RECIPE_ID);
    }


}
