package com.teammoeg.frostedheart;

import gloridifice.watersource.WaterSource;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHFluids {
    public static final ResourceLocation STILL_FLUID_TEXTURE = new ResourceLocation(WaterSource.MODID, "block/fluid/water_still");
    public static final ResourceLocation FLOWING_FLUID_TEXTURE = new ResourceLocation(WaterSource.MODID, "block/fluid/water_flow");
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, FHMain.MODID);


    public static RegistryObject<FlowingFluid> HOT_WATER = FLUIDS.register("hot_water", () -> {
        return new ForgeFlowingFluid.Source(FHFluids.HOT_WATER_PROPERTIES);
    });
    public static RegistryObject<FlowingFluid> HOT_WATER_FLOWING = FLUIDS.register("hot_water_flowing", () -> {
        return new ForgeFlowingFluid.Flowing(FHFluids.HOT_WATER_PROPERTIES);
    });

    public static ForgeFlowingFluid.Properties HOT_WATER_PROPERTIES =
            new ForgeFlowingFluid.Properties(HOT_WATER, HOT_WATER_FLOWING, FluidAttributes.builder(STILL_FLUID_TEXTURE, FLOWING_FLUID_TEXTURE)
                    .color(0x3ABDFF).viscosity(1000))
                    .slopeFindDistance(3).explosionResistance(100F);
}
