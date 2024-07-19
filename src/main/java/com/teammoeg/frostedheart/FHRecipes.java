/*
 * Copyright (c) 2022-2024 TeamMoeg
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

import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.content.incubator.IncubateRecipe;
import com.teammoeg.frostedheart.recipes.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.recipes.DietValueRecipe;
import com.teammoeg.frostedheart.recipes.DismantleInnerRecipe;
import com.teammoeg.frostedheart.recipes.InspireRecipe;
import com.teammoeg.frostedheart.recipes.InstallInnerRecipe;
import com.teammoeg.frostedheart.recipes.ModifyDamageRecipe;
import com.teammoeg.frostedheart.recipes.ResearchPaperRecipe;
import com.teammoeg.frostedheart.recipes.ShapelessCopyDataRecipe;
import com.teammoeg.frostedheart.recipes.SmokingDefrostRecipe;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerRecipe;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaRecipe;
import com.teammoeg.frostedheart.content.trade.policy.TradePolicy;
import com.teammoeg.frostedheart.content.utility.handstoves.FuelingRecipe;

import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHRecipes {
    public static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
            ForgeRegistries.RECIPE_SERIALIZERS, FHMain.MODID
    );

    static {
        GeneratorRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("generator", GeneratorRecipe.Serializer::new);
        GeneratorSteamRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("steam_generator", GeneratorSteamRecipe.Serializer::new);
        InstallInnerRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("recipe_inner", InstallInnerRecipe.Serializer::new);
        ChargerRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("charger", ChargerRecipe.Serializer::new);
        DismantleInnerRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("recipe_inner_dismantle", DismantleInnerRecipe.Serializer::new);
        CampfireDefrostRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("defrost_campfire", CampfireDefrostRecipe.Serializer::new);
        SmokingDefrostRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("defrost_smoking", SmokingDefrostRecipe.Serializer::new);
        FuelingRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("fuel_stove", FuelingRecipe.Serializer::new);
        DietValueRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("diet_override", DietValueRecipe.Serializer::new);
        IncubateRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("incubate", IncubateRecipe.Serializer::new);
        ResearchPaperRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("paper", ResearchPaperRecipe.Serializer::new);
        SaunaRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("sauna", SaunaRecipe.Serializer::new);
        ModifyDamageRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("modify_damage", ModifyDamageRecipe.Serializer::new);
        InspireRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("inspire", InspireRecipe.Serializer::new);
        ShapelessCopyDataRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("shapless_copy_data", ShapelessCopyDataRecipe.Serializer::new);
        TradePolicy.SERIALIZER = RECIPE_SERIALIZERS.register("trade", TradePolicy.Serializer::new);
    }

    public static void registerRecipeTypes() {
        GeneratorRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":generator");
        GeneratorSteamRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":steam_generator");
        ChargerRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":charger");
        DietValueRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":diet_override");
        IncubateRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":incubate");
        ResearchPaperRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":paper");
        SaunaRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":sauna");
        InspireRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":inspire");
        TradePolicy.TYPE = IRecipeType.register(FHMain.MODID + ":trade");
    }
}