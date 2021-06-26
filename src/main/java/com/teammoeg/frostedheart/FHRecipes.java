package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.common.GeneratorRecipe;
import net.minecraft.item.crafting.IRecipeType;

public class FHRecipes {
    public static void registerRecipeTypes() {
        GeneratorRecipe.TYPE = IRecipeType.register(FHMain.MODID+":generator");
    }
}
