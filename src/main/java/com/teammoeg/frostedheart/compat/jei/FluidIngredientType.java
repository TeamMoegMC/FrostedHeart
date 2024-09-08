package com.teammoeg.frostedheart.compat.jei;

import mezz.jei.api.ingredients.IIngredientType;
import net.minecraftforge.fluids.FluidStack;

public class FluidIngredientType implements IIngredientType<FluidStack> {
    public static final FluidIngredientType INSTANCE = new FluidIngredientType();

    private FluidIngredientType() {
    }

    @Override
    public Class<? extends FluidStack> getIngredientClass() {
        return FluidStack.class;
    }
}
