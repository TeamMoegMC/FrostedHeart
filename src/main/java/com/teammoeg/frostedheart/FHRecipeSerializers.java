package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.common.recipe.ElectrolyzerRecipe;
import com.teammoeg.frostedheart.common.recipe.GeneratorRecipe;
import com.teammoeg.frostedheart.common.recipe.GeneratorRecipeSerializer;
import electrodynamics.common.recipe.categories.fluiditem2fluid.FluidItem2FluidRecipeSerializer;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHRecipeSerializers {
    public static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
            ForgeRegistries.RECIPE_SERIALIZERS, FHMain.MODID
    );
    public static final IRecipeSerializer<ElectrolyzerRecipe> Electrolyzer_JSON_SERIALIZER = new FluidItem2FluidRecipeSerializer<>(
            ElectrolyzerRecipe.class);

    static {
        GeneratorRecipe.SERIALIZER = RECIPE_SERIALIZERS.register(
                "generator", GeneratorRecipeSerializer::new
        );
        ElectrolyzerRecipe.SERIALIZER = RECIPE_SERIALIZERS.register(ElectrolyzerRecipe.RECIPE_GROUP,
                () -> Electrolyzer_JSON_SERIALIZER);
    }
}
