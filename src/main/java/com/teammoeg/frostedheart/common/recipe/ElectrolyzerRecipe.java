package com.teammoeg.frostedheart.common.recipe;

import com.teammoeg.frostedheart.FHMain;
import electrodynamics.common.inventory.invutils.FluidRecipeWrapper;
import electrodynamics.common.recipe.ElectrodynamicsRecipe;
import electrodynamics.common.recipe.recipeutils.CountableIngredient;
import electrodynamics.common.recipe.recipeutils.FluidIngredient;
import electrodynamics.common.recipe.recipeutils.IFluidRecipe;
import electrodynamics.prefab.tile.components.ComponentType;
import electrodynamics.prefab.tile.components.type.ComponentFluidHandler;
import electrodynamics.prefab.tile.components.type.ComponentProcessor;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.RegistryObject;

import java.util.ArrayList;

public class ElectrolyzerRecipe extends ElectrodynamicsRecipe implements IFluidRecipe {
    public static IRecipeType<ElectrolyzerRecipe> TYPE;
    public static RegistryObject<IRecipeSerializer<?>> SERIALIZER;
    public static final String RECIPE_GROUP = "electrolyzer_recipe";
    public static final ResourceLocation RECIPE_ID = new ResourceLocation(FHMain.MODID, RECIPE_GROUP);
    private FluidIngredient INPUT_FLUID;
    private CountableIngredient INPUT_ITEM;
    private FluidStack OUTPUT_FLUID;
    private FluidStack OUTPUT_FLUID2;

    public ElectrolyzerRecipe(ResourceLocation recipeID, CountableIngredient inputItem, FluidIngredient inputFluid, FluidStack outputFluid, FluidStack outputFluid2) {
        super(recipeID);
        INPUT_ITEM = inputItem;
        INPUT_FLUID = inputFluid;
        OUTPUT_FLUID = outputFluid;
        OUTPUT_FLUID2 = outputFluid2;
    }

    @Override
    public FluidStack getFluidCraftingResult(FluidRecipeWrapper inv) {
        return OUTPUT_FLUID;
    }

    @Override
    public FluidStack getFluidRecipeOutput() {
        return OUTPUT_FLUID;
    }

    public FluidStack getFluidRecipeOutput2() {
        return OUTPUT_FLUID2;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }

    @Override
    public IRecipeType<?> getType() {
        return Registry.RECIPE_TYPE.getOrDefault(RECIPE_ID);
    }

    @Override
    public boolean matchesRecipe(ComponentProcessor pr) {

        if (INPUT_ITEM.testStack(pr.getInput())) {
            ComponentFluidHandler fluid = pr.getHolder().getComponent(ComponentType.FluidHandler);
            ArrayList<Fluid> inputFluids = fluid.getInputFluids();
            for (int i = 0; i < inputFluids.size(); i++) {
                FluidTank tank = fluid.getTankFromFluid(inputFluids.get(i));
                if (tank != null && tank.getFluid().getFluid().isEquivalentTo(INPUT_FLUID.getFluidStack().getFluid())
                        && tank.getFluidAmount() >= INPUT_FLUID.getFluidStack().getAmount()) {
                    return true;
                }
            }

        }

        return false;
    }
}
