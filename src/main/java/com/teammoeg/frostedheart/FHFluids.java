/*
 * Copyright (c) 2021-2024 TeamMoeg
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

import com.simibubi.create.content.fluids.VirtualFluid;
import com.tterrag.registrate.util.entry.FluidEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistries.Keys;

import static com.teammoeg.frostedheart.FHMain.REGISTRATE;

public class FHFluids {

    public static final ResourceLocation DEFAULT_STILL_TEXTURE = new ResourceLocation("block/water_still");
    public static final ResourceLocation DEFAULT_FLOWING_TEXTURE = new ResourceLocation("block/water_flow");
    public static final int DEFAULT_COLOR = 0xFFFFFFFF;

    static {
        REGISTRATE.setCreativeTab(FHTabs.MATERIALS_TAB);
    }

    public static final FluidEntry<ForgeFlowingFluid.Flowing> TEST = REGISTRATE.standardColoredWater(
            "test", 0xFF00FF00, FluidType.Properties.create()
                            .density(-1000).viscosity(10).temperature(300).canConvertToSource(false))
            .register();

    public static final FluidEntry<VirtualFluid> TEST3 = REGISTRATE.virtualColoredFluid(
            "test3", DEFAULT_STILL_TEXTURE, DEFAULT_FLOWING_TEXTURE, 0xFF00FF00)
            .register();

    public static final FluidEntry<VirtualFluid> RED_WATER = REGISTRATE.virtualColoredWater(
                    "red_water", 0xFFFF0000)
            .register();

    public static final FluidEntry<VirtualFluid> GREEN_WATER = REGISTRATE.virtualColoredWater(
            "green_water", 0xFF00FF00)
            .register();

    public static final FluidEntry<VirtualFluid> BLUE_WATER = REGISTRATE.virtualColoredWater(
            "blue_water", 0xFF0000FF)
            .register();

    // purple
    public static final FluidEntry<VirtualFluid> PURPLE_WATER = REGISTRATE.virtualColoredWater(
            "purple_water", 0xFF800080)
            .register();

    public static final FluidEntry<VirtualFluid> FLUORINE = REGISTRATE.virtualColoredGas(
                    "fluorine", 0xFF00AA00)
            .tag(FHTags.forgeFluidTag("fluorine"))
            .register();
    public static final FluidEntry<VirtualFluid> CHLORINE = REGISTRATE.virtualColoredGas(
            "chlorine", 0xFFADFF2F)
            .tag(FHTags.forgeFluidTag("chlorine"))
            .register();
    public static final FluidEntry<VirtualFluid> STEAM = REGISTRATE.virtualColoredGas(
            "steam", 0xFFFFFFFF)
            .tag(FHTags.forgeFluidTag("steam"))
            .register();
    public static final FluidEntry<VirtualFluid> SO2 = REGISTRATE.virtualColoredGas(
            "sulfur_dioxide", 0xFFEEE888)
            .tag(FHTags.forgeFluidTag("sulfur_dioxide"))
            .register();
    public static final FluidEntry<VirtualFluid> FERROUS_CHLORIDE = REGISTRATE.virtualColoredLiquid(
            "ferrous_chloride", 0xFFBB3333)
            .tag(FHTags.forgeFluidTag("ferrous_chloride"))
            .register();
    public static final FluidEntry<VirtualFluid> FERRIC_CHLORIDE = REGISTRATE.virtualColoredLiquid(
            "ferric_chloride", 0xFFB0FFDE)
            .tag(FHTags.forgeFluidTag("ferric_chloride"))
            .register();
    public static final FluidEntry<VirtualFluid> COPPER_CHLORIDE = REGISTRATE.virtualColoredLiquid(
            "copper_chloride", 0xFFB0FFDE)
            .tag(FHTags.forgeFluidTag("copper_chloride"))
            .register();
    public static final FluidEntry<VirtualFluid> ZINC_SULFATE = REGISTRATE.virtualColoredLiquid(
            "zinc_sulfate", 0xFFB0C4FF)
            .tag(FHTags.forgeFluidTag("zinc_sulfate"))
            .register();
    public static final FluidEntry<VirtualFluid> LIME_WATER = REGISTRATE.virtualColoredLiquid(
            "lime_water", 0xFFB0C4DE)
            .tag(FHTags.forgeFluidTag("lime_water"))
            .register();
    public static final FluidEntry<VirtualFluid> MAGNESIUM_CHLORIDE = REGISTRATE.virtualColoredLiquid(
            "magnesium_chloride", 0xFFDEDEEE)
            .tag(FHTags.forgeFluidTag("magnesium_chloride"))
            .register();
    public static final FluidEntry<VirtualFluid> SULFURIC_ACID = REGISTRATE.virtualColoredLiquid(
            "sulfuric_acid", 0xFFEEE8AA)
            .tag(FHTags.forgeFluidTag("sulfuric_acid"))
            .register();
    public static final FluidEntry<VirtualFluid> HYDROCHLORIC_ACID = REGISTRATE.virtualColoredLiquid(
            "hydrochloric_acid", 0xFFAAFFAA)
            .tag(FHTags.forgeFluidTag("hydrochloric_acid"))
            .register();
    public static final FluidEntry<VirtualFluid> CRYOLITE = REGISTRATE.virtualColoredLiquid(
            "cryolite", 0xFF90EE90)
            .tag(FHTags.forgeFluidTag("cryolite"))
            .register();
    public static final FluidEntry<VirtualFluid> TAR = REGISTRATE.virtualColoredLiquid(
            "tar", 0xFF000000)
            .tag(FHTags.forgeFluidTag("tar"))
            .register();
    public static final FluidEntry<VirtualFluid> PROTEIN = REGISTRATE.virtualColoredFluid(
            "protein",
                    new ResourceLocation(FHMain.MODID, "block/protein_fluid"),
                    new ResourceLocation(FHMain.MODID, "block/protein_fluid"), 0xFFFFFF)
            .tag(FHTags.forgeFluidTag("protein"))
            .register();
    public static final FluidEntry<VirtualFluid> LATEX = REGISTRATE.virtualColoredLiquid(
            "latex", 0xFFFFD700)
            .tag(FHTags.forgeFluidTag("latex"))
            .register();
    public static final FluidEntry<VirtualFluid> RESIN = REGISTRATE.virtualColoredLiquid(
            "resin", 0xFF8B4513)
            .tag(FHTags.forgeFluidTag("resin"))
            .register();
    // purified water
    public static final FluidEntry<VirtualFluid> PURIFIED_WATER = REGISTRATE.virtualColoredLiquid(
            "purified_water", 0xFF3ABDFF)
            .tag(FHTags.forgeFluidTag("purified_water"))
            .register();
    // wolfberry tea
    public static final FluidEntry<VirtualFluid> WOLFBERRY_TEA = REGISTRATE.virtualColoredLiquid(
            "wolfberry_tea", 0xFF6ABDFF)
            .tag(FHTags.forgeFluidTag("wolfberry_tea"))
            .register();

    static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, FHMain.MODID);
    static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(Keys.FLUID_TYPES, FHMain.MODID);

}
