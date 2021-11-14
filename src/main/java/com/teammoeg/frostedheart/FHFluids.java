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


    public static RegistryObject<FlowingFluid> HOT_WATER = FLUIDS.register("hot_water", () -> {
        return new ForgeFlowingFluid.Source(FHFluids.HOT_WATER_PROPERTIES);
    });
    public static RegistryObject<FlowingFluid> HOT_WATER_FLOWING = FLUIDS.register("hot_water_flowing", () -> {
        return new ForgeFlowingFluid.Flowing(FHFluids.HOT_WATER_PROPERTIES);
    });

    public static RegistryObject<FlowingFluid> STEAM = FLUIDS.register("steam", () -> {
        return new ForgeFlowingFluid.Source(FHFluids.STEAM_PROPERTIES);
    });
    public static RegistryObject<FlowingFluid> STEAM_FLOWING = FLUIDS.register("steam_flowing", () -> {
        return new ForgeFlowingFluid.Flowing(FHFluids.STEAM_PROPERTIES);
    });

    public static ForgeFlowingFluid.Properties HOT_WATER_PROPERTIES =
            new ForgeFlowingFluid.Properties(HOT_WATER, HOT_WATER_FLOWING, FluidAttributes.builder(STILL_FLUID_TEXTURE, FLOWING_FLUID_TEXTURE)
                    .color(0x3ABDFF).temperature(333))
                    .slopeFindDistance(3).explosionResistance(100F);
    public static ForgeFlowingFluid.Properties STEAM_PROPERTIES =
            new ForgeFlowingFluid.Properties(STEAM, STEAM_FLOWING, FluidAttributes.builder(STILL_FLUID_TEXTURE, FLOWING_FLUID_TEXTURE)
                    .color(0xFFFFFF).viscosity(-1))
                    .slopeFindDistance(1).explosionResistance(100F);
}
