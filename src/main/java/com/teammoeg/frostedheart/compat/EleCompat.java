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

package com.teammoeg.frostedheart.compat;

import electrodynamics.DeferredRegisters;
import electrodynamics.common.tile.TileChemicalCrystallizer;
import electrodynamics.common.tile.TileChemicalMixer;
import electrodynamics.common.tile.TileMineralWasher;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class EleCompat {
    public static void setup() {
        TileChemicalMixer.SUPPORTED_INPUT_FLUIDS = new Fluid[]{

                Fluids.WATER, DeferredRegisters.fluidEthanol, DeferredRegisters.fluidSulfuricAcid
                , ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "chlorine"))
                , ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "hydrogen_fluoride"))
                , ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "fluoroaluminic_acid"))
        };
        TileChemicalMixer.SUPPORTED_OUTPUT_FLUIDS = new Fluid[]{

                DeferredRegisters.fluidSulfuricAcid, DeferredRegisters.fluidPolyethylene
                , ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "magnesium_chloride"))
                , ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "lime_water"))
                , ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "salt_water"))
                , ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "hydrogen_fluoride"))
                , ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "fluoroaluminic_acid"))
                , ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "cryolite"))
        };
        TileMineralWasher.SUPPORTED_INPUT_FLUIDS = new Fluid[]{

                DeferredRegisters.fluidSulfuricAcid,
                ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "sodium_hydroxide"))
        };
        ArrayList<Fluid> list = Arrays.stream(TileChemicalCrystallizer.SUPPORTED_INPUT_FLUIDS).collect(Collectors.toCollection(ArrayList::new));
        list.add(ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "magnesium_chloride")));
        list.add(ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "lime_water")));
        list.add(ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "sodium_aluminate")));
        list.add(ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "aluminium")));
        TileChemicalCrystallizer.SUPPORTED_INPUT_FLUIDS = list.toArray(new Fluid[list.size()]);
        TileMineralWasher.SUPPORTED_OUTPUT_FLUIDS = TileChemicalCrystallizer.SUPPORTED_INPUT_FLUIDS;

    }
}
