package com.teammoeg.frostedheart.common.tile;

import com.teammoeg.frostedheart.common.recipe.ElectrolyzerRecipe;
import electrodynamics.common.recipe.recipeutils.CountableIngredient;
import electrodynamics.common.recipe.recipeutils.FluidIngredient;
import electrodynamics.prefab.tile.GenericTile;
import electrodynamics.prefab.tile.components.ComponentType;
import electrodynamics.prefab.tile.components.type.ComponentElectrodynamic;
import electrodynamics.prefab.tile.components.type.ComponentFluidHandler;
import electrodynamics.prefab.tile.components.type.ComponentPacketHandler;
import electrodynamics.prefab.tile.components.type.ComponentProcessor;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;


public class FHComponentProcessor extends ComponentProcessor {
    public FHComponentProcessor(GenericTile source) {
        super(source);
    }

    public <T extends ElectrolyzerRecipe> void processElectrolyzerRecipe(ComponentProcessor pr, Class<T> recipeClass) {
        if (getRecipe() != null) {
            T recipe = recipeClass.cast(getRecipe());

            ComponentFluidHandler fluid = pr.getHolder().getComponent(ComponentType.FluidHandler);
            FluidStack outputFluid = recipe.getFluidRecipeOutput();
            FluidStack outputFluid2 = recipe.getFluidRecipeOutput2();

            FluidStack inputFluid = ((FluidIngredient) recipe.getIngredients().get(1)).getFluidStack();

            FluidTank outputFluidTank = fluid.getTankFromFluid(outputFluid.getFluid());
            FluidTank outputFluidTank2 = fluid.getTankFromFluid(outputFluid2.getFluid());

            if (getOutputCap() >= outputFluid.getAmount() + outputFluidTank.getFluidAmount() && getOutputCap() >= outputFluid2.getAmount() + outputFluidTank2.getFluidAmount()) {
                pr.getInput().setCount(pr.getInput().getCount() - ((CountableIngredient) recipe.getIngredients().get(0)).getStackSize());
                fluid.getStackFromFluid(inputFluid.getFluid()).shrink(inputFluid.getAmount());
                fluid.getStackFromFluid(outputFluid.getFluid()).grow(outputFluid.getAmount());
                fluid.getStackFromFluid(outputFluid2.getFluid()).grow(outputFluid2.getAmount());
                pr.getHolder().<ComponentPacketHandler>getComponent(ComponentType.PacketHandler).sendGuiPacketToTracking();
            }
        }
    }

    public <T extends ElectrolyzerRecipe> boolean canProcessElectrolyzerRecipe(ComponentProcessor pr, Class<T> recipeClass,
                                                                               IRecipeType<?> typeIn) {
        ComponentElectrodynamic electro = getHolder().getComponent(ComponentType.Electrodynamic);
        ComponentFluidHandler fluid = pr.getHolder().getComponent(ComponentType.FluidHandler);
        ElectrolyzerRecipe recipe = pr.getHolder().getFluidItem2FluidRecipe(pr, recipeClass, typeIn);

        int outputCap = 0;
        Fluid outputFluid = null;

        setRecipe(recipe);

        if (recipe != null) {
            outputFluid = recipe.getFluidRecipeOutput().getFluid();
            outputCap = fluid.getTankFromFluid(outputFluid).getCapacity();
        }
        setOutputCap(outputCap);

        return recipe != null && electro.getJoulesStored() >= pr.getUsage()
                && outputCap >= fluid.getTankFromFluid(outputFluid).getFluidAmount() + recipe.getFluidRecipeOutput().getAmount();

    }
}
