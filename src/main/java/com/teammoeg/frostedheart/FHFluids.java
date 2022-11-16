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

import com.teammoeg.frostedheart.util.ReferenceSupplier;

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
	public static final ResourceLocation PROTEIN_FLUID_TEXTURE = new ResourceLocation(FHMain.MODID,
			"block/protein_fluid");
	public static final ResourceLocation FLOWING_FLUID_TEXTURE = new ResourceLocation("block/water_flow");
	public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, FHMain.MODID);

	public static RegistryObject<FlowingFluid> registerGas(String name, int color) {
		ReferenceSupplier<FlowingFluid> rss=new ReferenceSupplier<>();
		ForgeFlowingFluid.Properties props=new ForgeFlowingFluid.Properties(rss,rss,
				FluidAttributes.builder(STILL_FLUID_TEXTURE, FLOWING_FLUID_TEXTURE).color(color).density(-1)
				.gaseous().viscosity(-1)).block(null).slopeFindDistance(3).explosionResistance(100F);
		return rss.set(FLUIDS.register(name, () -> new ForgeFlowingFluid.Source(props)));
	}
	public static RegistryObject<FlowingFluid> FLUORINE =registerGas("fluorine",0xFF00AA00);
	public static RegistryObject<FlowingFluid> CHLORINE = registerGas("chlorine",0xFFADFF2F);
	public static RegistryObject<FlowingFluid> STEAM = registerGas("steam",0xFFFFFFFF);
	public static RegistryObject<FlowingFluid> SO2 = registerGas("sulfur_dioxide",0xFFEEE888);

	

	public static RegistryObject<FlowingFluid> WOLFBERRY_TEA = FLUIDS.register("wolfberry_tea", () -> {
		return new ForgeFlowingFluid.Source(FHFluids.WOLFBERRY_TEA_PROPERTIES);
	});
	public static RegistryObject<FlowingFluid> WOLFBERRY_TEA_FLOWING = FLUIDS.register("wolfberry_tea_flowing", () -> {
		return new ForgeFlowingFluid.Flowing(FHFluids.WOLFBERRY_TEA_PROPERTIES);
	});


	public static ForgeFlowingFluid.Properties WOLFBERRY_TEA_PROPERTIES = new ForgeFlowingFluid.Properties(
			WOLFBERRY_TEA, WOLFBERRY_TEA_FLOWING,
			FluidAttributes.builder(STILL_FLUID_TEXTURE, FLOWING_FLUID_TEXTURE).color(0xFF6ABDFF).temperature(333))
					.block(null).slopeFindDistance(3).explosionResistance(100F);

}
