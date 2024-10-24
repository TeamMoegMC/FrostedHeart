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

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.teammoeg.frostedheart.util.utility.ReferenceSupplier;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistries.Keys;

public class FHFluids {
    public static final ResourceLocation STILL_FLUID_TEXTURE = new ResourceLocation("block/water_still");
    public static final ResourceLocation PROTEIN_FLUID_TEXTURE = new ResourceLocation(FHMain.MODID,
            "block/protein_fluid");
    public static final ResourceLocation FLOWING_FLUID_TEXTURE = new ResourceLocation("block/water_flow");
    static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, FHMain.MODID);
	static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(Keys.FLUID_TYPES, FHMain.MODID);
	public static RegistryObject<Fluid> WOLFBERRY_TEA =
			registerFluid("wolfberry_tea_flowing",STILL_FLUID_TEXTURE,FLOWING_FLUID_TEXTURE,0xFF6ABDFF,
					FluidType.Properties.create().temperature(70),
					p->p.block(null).slopeFindDistance(3).explosionResistance(100F)
			);
    public static RegistryObject<Fluid> FLUORINE = registerSimpleGas("fluorine", 0xFF00AA00);
   
    public static RegistryObject<Fluid> CHLORINE = registerSimpleGas("chlorine", 0xFFADFF2F);
    public static RegistryObject<Fluid> STEAM = registerSimpleGas("steam", 0xFFFFFFFF);
    public static RegistryObject<Fluid> SO2 = registerSimpleGas("sulfur_dioxide", 0xFFEEE888);

	/*
	  event.create('ferrous_chloride').textureThin(0xBB3333)
	  event.create('ferric_chloride').textureThin(0xB0FFDE)
	  event.create('copper_chloride').textureThin(0xB0FFDE)
	  event.create('zinc_sulfate').textureThin(0xB0C4FF)
	  event.create('lime_water').textureThin(0xB0C4DE)
	  event.create('magnesium_chloride').textureThin(0xDEDEEE)
	  event.create('sulfuric_acid').textureThin(0xEEE8AA)
	  event.create('hydrochloric_acid').textureThin(0xAAFFAA)
	  event.create('cryolite').textureThin(0x90EE90)
	  event.create('tar').textureThick(0x000000).viscosity(1150).density(950)
	  event.create('protein').textureStill("kubejs:block/protein_fluid").textureFlowing("kubejs:block/protein_fluid").viscosity(200).density(200)
	 */
	public static RegistryObject<Fluid> FERROUS_CHLORIDE = registerSimpleLiquid("ferrous_chloride", 0xBB3333);
	public static RegistryObject<Fluid> FERRIC_CHLORIDE = registerSimpleLiquid("ferric_chloride", 0xB0FFDE);
	public static RegistryObject<Fluid> COPPER_CHLORIDE = registerSimpleLiquid("copper_chloride", 0xB0FFDE);
	public static RegistryObject<Fluid> ZINC_SULFATE = registerSimpleLiquid("zinc_sulfate", 0xB0C4FF);
	public static RegistryObject<Fluid> LIME_WATER = registerSimpleLiquid("lime_water", 0xB0C4DE);
	public static RegistryObject<Fluid> MAGNESIUM_CHLORIDE = registerSimpleLiquid("magnesium_chloride", 0xDEDEEE);
	public static RegistryObject<Fluid> SULFURIC_ACID = registerSimpleLiquid("sulfuric_acid", 0xEEE8AA);
	public static RegistryObject<Fluid> HYDROCHLORIC_ACID = registerSimpleLiquid("hydrochloric_acid", 0xAAFFAA);
	public static RegistryObject<Fluid> CRYOLITE = registerSimpleLiquid("cryolite", 0x90EE90);
	public static RegistryObject<Fluid> TAR = registerSimpleLiquid("tar", 0x000000);
	public static RegistryObject<Fluid> PROTEIN = registerFluid("protein",PROTEIN_FLUID_TEXTURE,PROTEIN_FLUID_TEXTURE,0xFFFFFF,FluidType.Properties.create().viscosity(200).density(200),p->p);
	public static RegistryObject<Fluid> LATEX = registerSimpleLiquid("latex", 0xFFD700);
	public static RegistryObject<Fluid> RESIN = registerSimpleLiquid("resin", 0x8B4513);


    public static RegistryObject<Fluid> registerFluid(String name,ResourceLocation still,ResourceLocation flowing, int color,FluidType.Properties properties,UnaryOperator<ForgeFlowingFluid.Properties> blockProperties) {
    	RegistryObject<FluidType> GAS=FLUID_TYPES.register(name,()->new FluidType(properties) {
			@Override
			public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
				consumer.accept(new IClientFluidTypeExtensions() {
					@Override
					public int getTintColor() {
						return color;
					}

					@Override
					public ResourceLocation getStillTexture() {
						return STILL_FLUID_TEXTURE;
					}

					@Override
					public ResourceLocation getFlowingTexture() {
						return FLOWING_FLUID_TEXTURE;
					}
					
				});
			}
    		
    		
    	});
    	ReferenceSupplier<Fluid> ls=new ReferenceSupplier<Fluid>();
    	//builder(STILL_FLUID_TEXTURE, FLOWING_FLUID_TEXTURE).color(color)
    	return ls.set(FLUIDS.register(name, ()->new ForgeFlowingFluid.Flowing(
    		blockProperties.apply(new ForgeFlowingFluid.Properties(GAS,ls,ls)))));
    	
    }

	public static RegistryObject<Fluid> registerSimpleGas(String name, int color) {
		return registerSimpleFluid(name, color, -1, 0, 300, false);
	}

	public static RegistryObject<Fluid> registerSimpleLiquid(String name, int color) {
		return registerSimpleFluid(name, color, 1, 0, 300, false);
	}

    public static RegistryObject<Fluid> registerSimpleFluid(String name, int color, int density, int viscosity, int temperature, boolean canConvertToSource) {
    	RegistryObject<FluidType> GAS=FLUID_TYPES.register(name,()->new FluidType(FluidType.Properties.create().density(density).viscosity(viscosity).temperature(temperature).canConvertToSource(canConvertToSource)) {
			@Override
			public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
				consumer.accept(new IClientFluidTypeExtensions() {
					@Override
					public int getTintColor() {
						return color;
					}

					@Override
					public ResourceLocation getStillTexture() {
						return STILL_FLUID_TEXTURE;
					}

					@Override
					public ResourceLocation getFlowingTexture() {
						return FLOWING_FLUID_TEXTURE;
					}
					
				});
			}
    		
    		
    	});
    	ReferenceSupplier<Fluid> ls=new ReferenceSupplier<Fluid>();
    	//builder(STILL_FLUID_TEXTURE, FLOWING_FLUID_TEXTURE).color(color)
    	return ls.set(FLUIDS.register(name, ()->new ForgeFlowingFluid.Flowing(new ForgeFlowingFluid.Properties(GAS,ls,ls)
    		.explosionResistance(100F).block(null).bucket(null).slopeFindDistance(3))));
    	
    }

}
