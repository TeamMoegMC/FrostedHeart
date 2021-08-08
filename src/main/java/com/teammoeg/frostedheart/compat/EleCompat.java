package com.teammoeg.frostedheart.compat;

import electrodynamics.DeferredRegisters;
import electrodynamics.common.tile.TileChemicalCrystallizer;
import electrodynamics.common.tile.TileChemicalMixer;
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

                Fluids.WATER, DeferredRegisters.fluidEthanol
                , ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "chlorine"))

        };
        TileChemicalMixer.SUPPORTED_OUTPUT_FLUIDS = new Fluid[]{

                DeferredRegisters.fluidSulfuricAcid, DeferredRegisters.fluidPolyethylene
                , ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "magnesium_chloride"))
                , ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "lime_water"))

        };
        ArrayList<Fluid> list = Arrays.stream(TileChemicalCrystallizer.SUPPORTED_INPUT_FLUIDS).collect(Collectors.toCollection(ArrayList::new));
        list.add(ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "magnesium_chloride")));
        list.add(ForgeRegistries.FLUIDS.getValue(new ResourceLocation("kubejs", "lime_water")));
        TileChemicalCrystallizer.SUPPORTED_INPUT_FLUIDS = list.toArray(new Fluid[list.size()]);

    }
}
