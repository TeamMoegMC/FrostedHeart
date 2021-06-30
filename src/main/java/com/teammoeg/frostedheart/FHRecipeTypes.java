package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.common.recipe.GeneratorRecipe;
import net.minecraft.item.crafting.IRecipeType;

public class FHRecipeTypes {
    public static void registerRecipeTypes() {
        GeneratorRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":generator");
    }
}
