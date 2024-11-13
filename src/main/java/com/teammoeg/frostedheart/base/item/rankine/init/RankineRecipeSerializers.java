package com.teammoeg.frostedheart.base.item.rankine.init;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.item.rankine.alloys.AlloyModifierRecipe;
import com.teammoeg.frostedheart.base.item.rankine.alloys.ElementRecipe;
import com.teammoeg.frostedheart.base.item.rankine.alloys.OldAlloyingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RankineRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS.getRegistryName(), FHMain.MODID);

    public static RegistryObject<RecipeSerializer<ElementRecipe>> ELEMENT_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("element", ElementRecipe.Serializer::new);
    public static RegistryObject<RecipeSerializer<AlloyModifierRecipe>> ALLOY_MODIFIER_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("alloy_modifier", AlloyModifierRecipe.Serializer::new);
    public static RegistryObject<RecipeSerializer<OldAlloyingRecipe>> ALLOYING_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("alloying", OldAlloyingRecipe.Serializer::new);
}
