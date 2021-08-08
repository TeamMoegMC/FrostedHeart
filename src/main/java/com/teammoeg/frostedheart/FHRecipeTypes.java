package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.recipe.CrucibleRecipe;
import com.teammoeg.frostedheart.recipe.ElectrolyzerRecipe;
import com.teammoeg.frostedheart.recipe.GeneratorRecipe;
import net.minecraft.item.crafting.IRecipeType;

public class FHRecipeTypes {
    public static void registerRecipeTypes() {
        GeneratorRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":generator");
        ElectrolyzerRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":electrolyzer_recipe");
        CrucibleRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":crucible");
    }

}
