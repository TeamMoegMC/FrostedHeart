/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Caupona.
 *
 * Caupona is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Caupona is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Specially, we allow this software to be used alongside with closed source software Minecraft(R) and Forge or other modloader.
 * Any mods or plugins can also use apis provided by forge or com.teammoeg.caupona.api without using GPL or open source.
 *
 * You should have received a copy of the GNU General Public License
 * along with Caupona. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.caupona.datagen;


import java.util.concurrent.CompletableFuture;

import com.teammoeg.caupona.CPFluids;
import com.teammoeg.caupona.CPMain;
import com.teammoeg.caupona.CPTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CPFluidTagGenerator extends TagsProvider<Fluid> {

	public CPFluidTagGenerator(DataGenerator dataGenerator, String modId, ExistingFileHelper existingFileHelper,CompletableFuture<HolderLookup.Provider> provider) {
		super(dataGenerator.getPackOutput(), Registries.FLUID, provider,modId,existingFileHelper);
	}

	@Override
	protected void addTags(Provider p) {
		TagAppender<Fluid> stews=tag(CPTags.Fluids.STEWS);
		tag(CPTags.Fluids.BOILABLE).add(BuiltInRegistries.FLUID.getResourceKey(Fluids.WATER).get()).add(NeoForgeMod.MILK.getKey())
				.addTag(CPTags.Fluids.STEWS);
		CPFluids.getAllKeys().forEach(stews::add);
		tag(CPTags.Fluids.ANY_WATER).add(ResourceKey.create(Registries.FLUID,mrl("stock"))).add(ResourceKey.create(Registries.FLUID,mrl("nail_soup")));
		
		tag(CPTags.Fluids.PUMICE_ON).add(BuiltInRegistries.FLUID.getResourceKey(Fluids.WATER).get());
		tag(ResourceLocation.fromNamespaceAndPath("watersource", "drink")).add(ResourceKey.create(Registries.FLUID,mrl("nail_soup")));
	}
	private Fluid cp(String s) {
		Fluid i = BuiltInRegistries.FLUID.get(mrl(s));
		return i;// just going to cause trouble if not exists
	}
	private TagAppender<Fluid> tag(String s) {
		return this.tag(FluidTags.create(mrl(s)));
	}

	private TagAppender<Fluid> tag(ResourceLocation s) {
		return this.tag(FluidTags.create(s));
	}

	private ResourceLocation rl(DeferredHolder<Fluid,Fluid> it) {
		return it.getId();
	}

	private ResourceLocation rl(String r) {
		return ResourceLocation.parse(r);
	}

	private TagKey<Fluid> otag(String s) {
		return FluidTags.create(mrl(s));
	}

	private TagKey<Fluid> atag(ResourceLocation s) {
		return FluidTags.create(s);
	}

	private ResourceLocation mrl(String s) {
		return ResourceLocation.fromNamespaceAndPath(CPMain.MODID, s);
	}

	private ResourceLocation frl(String s) {
		return ResourceLocation.fromNamespaceAndPath("c", s);
	}

	private ResourceLocation mcrl(String s) {
		return ResourceLocation.withDefaultNamespace(s);
	}

	@Override
	public String getName() {
		return CPMain.MODID + " fluid tags";
	}
/*
	@Override
	protected Path getPath(ResourceLocation id) {
		return this.generator.getOutputFolder()
				.resolve("data/" + id.getNamespace() + "/tags/fluids/" + id.getPath() + ".json");
	}*/

}
