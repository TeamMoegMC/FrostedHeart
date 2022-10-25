/*
 * Copyright (c) 2022 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.content.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.content.generator.GeneratorRecipeSerializer;
import com.teammoeg.frostedheart.content.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.content.generator.GeneratorSteamRecipeSerializer;
import com.teammoeg.frostedheart.content.incubator.IncubateRecipe;
import com.teammoeg.frostedheart.content.incubator.IncubateRecipeSerializer;
import com.teammoeg.frostedheart.content.recipes.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.content.recipes.CampfireDefrostRecipeSerializer;
import com.teammoeg.frostedheart.content.recipes.DamageModifySerializer;
import com.teammoeg.frostedheart.content.recipes.DietValueRecipe;
import com.teammoeg.frostedheart.content.recipes.DietValueSerializer;
import com.teammoeg.frostedheart.content.recipes.InspireRecipe;
import com.teammoeg.frostedheart.content.recipes.InspireSerializer;
import com.teammoeg.frostedheart.content.recipes.PaperRecipe;
import com.teammoeg.frostedheart.content.recipes.PaperSerializer;
import com.teammoeg.frostedheart.content.recipes.RecipeInner;
import com.teammoeg.frostedheart.content.recipes.RecipeInnerDismantle;
import com.teammoeg.frostedheart.content.recipes.RecipeInnerDismantleSerializer;
import com.teammoeg.frostedheart.content.recipes.RecipeInnerSerializer;
import com.teammoeg.frostedheart.content.recipes.RecipeModifyDamage;
import com.teammoeg.frostedheart.content.recipes.SmokingDefrostRecipe;
import com.teammoeg.frostedheart.content.recipes.SmokingDefrostRecipeSerializer;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerRecipe;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerRecipeSerializer;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaRecipe;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaSerializer;
import com.teammoeg.frostedheart.content.temperature.handstoves.RecipeFueling;
import com.teammoeg.frostedheart.content.temperature.handstoves.RecipeFuelingSerializer;

import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHRecipes {
    public static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
            ForgeRegistries.RECIPE_SERIALIZERS, FHMain.MODID
    );

    static {
        GeneratorRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("generator", GeneratorRecipeSerializer::new);
        GeneratorSteamRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("steam_generator", GeneratorSteamRecipeSerializer::new);
        RecipeInner.SERIALIZER = RECIPE_SERIALIZERS.register("recipe_inner", RecipeInnerSerializer::new);
        ChargerRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("charger", ChargerRecipeSerializer::new);
        RecipeInnerDismantle.SERIALIZER = RECIPE_SERIALIZERS.register("recipe_inner_dismantle", RecipeInnerDismantleSerializer::new);
        CampfireDefrostRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("defrost_campfire", CampfireDefrostRecipeSerializer::new);
        SmokingDefrostRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("defrost_smoking", SmokingDefrostRecipeSerializer::new);
        RecipeFueling.SERIALIZER = RECIPE_SERIALIZERS.register("fuel_stove", RecipeFuelingSerializer::new);
        DietValueRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("diet_override", DietValueSerializer::new);
        IncubateRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("incubate", IncubateRecipeSerializer::new);
        PaperRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("paper", PaperSerializer::new);
        SaunaRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("sauna", SaunaSerializer::new);
        RecipeModifyDamage.SERIALIZER=RECIPE_SERIALIZERS.register("modify_damage", DamageModifySerializer::new);
        InspireRecipe.SERIALIZER=RECIPE_SERIALIZERS.register("inspire",InspireSerializer::new);
    }

    public static void registerRecipeTypes() {
        GeneratorRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":generator");
        GeneratorSteamRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":steam_generator");
        ChargerRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":charger");
        DietValueRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":diet_override");
        IncubateRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":incubate");
        PaperRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":paper");
        SaunaRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":sauna");
        InspireRecipe.TYPE=IRecipeType.register(FHMain.MODID + ":inspire");
    }
}