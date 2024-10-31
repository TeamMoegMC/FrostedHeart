package com.teammoeg.frostedheart.base.item.rankine.init;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.item.rankine.alloys.AlloyModifierRecipe;
import com.teammoeg.frostedheart.base.item.rankine.alloys.ElementRecipe;
import com.teammoeg.frostedheart.base.item.rankine.alloys.OldAlloyingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RankineRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES.getRegistryName(), FHMain.MODID);


    public static RegistryObject<RecipeType<ElementRecipe>> ELEMENT = RECIPE_TYPES.register("element",() -> new RecipeType<>() {
        public String toString() {
            return "element";
        }
    });

    public static RegistryObject<RecipeType<AlloyModifierRecipe>> ALLOY_MODIFIER = RECIPE_TYPES.register("alloy_modifier",() -> new RecipeType<>() {
        public String toString() {
            return "alloy_modifier";
        }
    });

    public static RegistryObject<RecipeType<OldAlloyingRecipe>> ALLOYING = RECIPE_TYPES.register("alloying",() -> new RecipeType<>() {
        public String toString() {
            return "alloying";
        }
    });
}
