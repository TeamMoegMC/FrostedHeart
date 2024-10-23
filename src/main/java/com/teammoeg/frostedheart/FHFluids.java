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
	public static final ResourceLocation PURIFIED_WATER_STILL_FLUID_TEXTURE = new ResourceLocation(FHMain.MODID, "block/fluid/purified_water_still");
	public static final ResourceLocation PURIFIED_WATER_FLOWING_FLUID_TEXTURE = new ResourceLocation(FHMain.MODID, "block/fluid/purified_water_flow");
    static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, FHMain.MODID);
	static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(Keys.FLUID_TYPES, FHMain.MODID);
	
    public static RegistryObject<Fluid> FLUORINE = registerGas("fluorine", 0xFF00AA00);
   
    public static RegistryObject<Fluid> CHLORINE = registerGas("chlorine", 0xFFADFF2F);
    public static RegistryObject<Fluid> STEAM = registerGas("steam", 0xFFFFFFFF);
    public static RegistryObject<Fluid> SO2 = registerGas("sulfur_dioxide", 0xFFEEE888);
    public static RegistryObject<Fluid> WOLFBERRY_TEA =
    	registerFluid("wolfberry_tea_flowing",STILL_FLUID_TEXTURE,FLOWING_FLUID_TEXTURE,0xFF6ABDFF,
    		FluidType.Properties.create().temperature(70),
    		p->p.block(null).slopeFindDistance(3).explosionResistance(100F)
    		);
	public static RegistryObject<Fluid> PURIFIED_WATER = registerFluid("purified_water", PURIFIED_WATER_STILL_FLUID_TEXTURE,PURIFIED_WATER_FLOWING_FLUID_TEXTURE,0xCC3ABDFF,
			FluidType.Properties.create().temperature(27),p->p.block(null).slopeFindDistance(3).explosionResistance(100F));
    
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
    public static RegistryObject<Fluid> registerGas(String name, int color) {
    	RegistryObject<FluidType> GAS=FLUID_TYPES.register(name,()->new FluidType(FluidType.Properties.create().density(-1).viscosity(-1).temperature(100).canConvertToSource(false)) {
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
