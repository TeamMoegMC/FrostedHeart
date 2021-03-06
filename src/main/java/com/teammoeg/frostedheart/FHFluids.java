/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart;

import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHFluids {
    public static final ResourceLocation STILL_FLUID_TEXTURE = new ResourceLocation("block/water_still");
    public static final ResourceLocation FLOWING_FLUID_TEXTURE =new ResourceLocation("block/water_flow");
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, FHMain.MODID);


    /*public static RegistryObject<FlowingFluid> HOT_WATER = FLUIDS.register("hot_water", () -> {
        return new ForgeFlowingFluid.Source(FHFluids.HOT_WATER_PROPERTIES);
    });
    public static RegistryObject<FlowingFluid> HOT_WATER_FLOWING = FLUIDS.register("hot_water_flowing", () -> {
        return new ForgeFlowingFluid.Flowing(FHFluids.HOT_WATER_PROPERTIES);
    });*/
    public static RegistryObject<FlowingFluid> FLUORINE = FLUIDS.register("fluorine", () -> {
        return new ForgeFlowingFluid.Source(FHFluids.FLUORINE_PROPERTIES);
    });
    public static RegistryObject<FlowingFluid> FLUORINE_FLOWING = FLUIDS.register("fluorine_flowing", () -> {
        return new ForgeFlowingFluid.Flowing(FHFluids.FLUORINE_PROPERTIES);

    });
    public static RegistryObject<FlowingFluid> CHLORINE = FLUIDS.register("chlorine", () -> {
        return new ForgeFlowingFluid.Source(FHFluids.CHLORINE_PROPERTIES);
    });
    public static RegistryObject<FlowingFluid> CHLORINE_FLOWING = FLUIDS.register("chlorine_flowing", () -> {
        return new ForgeFlowingFluid.Flowing(FHFluids.CHLORINE_PROPERTIES);

    });

    public static RegistryObject<FlowingFluid> STEAM = FLUIDS.register("steam", () -> {
        return new ForgeFlowingFluid.Source(FHFluids.STEAM_PROPERTIES);
    });
    public static RegistryObject<FlowingFluid> STEAM_FLOWING = FLUIDS.register("steam_flowing", () -> {
        return new ForgeFlowingFluid.Flowing(FHFluids.STEAM_PROPERTIES);
    });

    public static RegistryObject<FlowingFluid> WOLFBERRY_TEA = FLUIDS.register("wolfberry_tea", () -> {
        return new ForgeFlowingFluid.Source(FHFluids.WOLFBERRY_TEA_PROPERTIES);
    });
    public static RegistryObject<FlowingFluid> WOLFBERRY_TEA_FLOWING = FLUIDS.register("wolfberry_tea_flowing", () -> {
        return new ForgeFlowingFluid.Flowing(FHFluids.WOLFBERRY_TEA_PROPERTIES);
    });

    /*public static ForgeFlowingFluid.Properties HOT_WATER_PROPERTIES =
            new ForgeFlowingFluid.Properties(HOT_WATER, HOT_WATER_FLOWING, FluidAttributes.builder(STILL_FLUID_TEXTURE, FLOWING_FLUID_TEXTURE)
                    .color(0xFF3ABDFF).temperature(333)).block(null)
                    .slopeFindDistance(3).explosionResistance(100F);*/
    public static ForgeFlowingFluid.Properties STEAM_PROPERTIES =
            new ForgeFlowingFluid.Properties(STEAM, STEAM_FLOWING, FluidAttributes.builder(STILL_FLUID_TEXTURE, FLOWING_FLUID_TEXTURE)
                    .color(0xFFFFFFFF).viscosity(-1))
                    .slopeFindDistance(1).explosionResistance(100F);
    public static ForgeFlowingFluid.Properties WOLFBERRY_TEA_PROPERTIES =
            new ForgeFlowingFluid.Properties(WOLFBERRY_TEA, WOLFBERRY_TEA_FLOWING, FluidAttributes.builder(STILL_FLUID_TEXTURE, FLOWING_FLUID_TEXTURE)
                    .color(0xFF6ABDFF).temperature(333)).block(null)
                    .slopeFindDistance(3).explosionResistance(100F);
    public static ForgeFlowingFluid.Properties FLUORINE_PROPERTIES=new ForgeFlowingFluid.Properties(FLUORINE, FLUORINE_FLOWING, FluidAttributes.builder(STILL_FLUID_TEXTURE, FLOWING_FLUID_TEXTURE)
                    .color(0xFF00AA00).density(-1).gaseous().viscosity(-1)).block(null)
                    .slopeFindDistance(3).explosionResistance(100F);
    public static ForgeFlowingFluid.Properties CHLORINE_PROPERTIES=new ForgeFlowingFluid.Properties(CHLORINE, CHLORINE_FLOWING, FluidAttributes.builder(STILL_FLUID_TEXTURE, FLOWING_FLUID_TEXTURE)
            .color(0xFFADFF2F).density(-1).gaseous().viscosity(-1)).block(null)
            .slopeFindDistance(3).explosionResistance(100F);
}
