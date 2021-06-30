package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.common.recipe.GeneratorRecipe;
import com.teammoeg.frostedheart.common.recipe.GeneratorRecipeSerializer;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHRecipeSerializers {
    public static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
            ForgeRegistries.RECIPE_SERIALIZERS, FHMain.MODID
    );

    static {
        GeneratorRecipe.SERIALIZER = RECIPE_SERIALIZERS.register(
                "generator", GeneratorRecipeSerializer::new
        );
    }
}
