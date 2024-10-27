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

import com.teammoeg.caupona.CPMain;

import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.registries.VanillaRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = CPMain.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CPDataGenerator {
	@SubscribeEvent
	public static void gatherData(GatherDataEvent event) {
		System.out.println("Gather data");
		DataGenerator gen = event.getGenerator();
		ExistingFileHelper exHelper = event.getExistingFileHelper();
		
		CompletableFuture<HolderLookup.Provider> completablefuture = CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor());
		gen.addProvider(event.includeClient(),new CPItemModelProvider(gen, CPMain.MODID, exHelper));
		gen.addProvider(event.includeServer(),new CPRecipeProvider(gen,completablefuture));
		gen.addProvider(event.includeServer(),new CPItemTagGenerator(gen, CPMain.MODID, exHelper,event.getLookupProvider()));
		gen.addProvider(event.includeServer(),new CPBlockTagGenerator(gen, CPMain.MODID, exHelper,event.getLookupProvider()));
		gen.addProvider(event.includeServer(),new CPFluidTagGenerator(gen, CPMain.MODID, exHelper,event.getLookupProvider()));
		gen.addProvider(event.includeServer(),new CPGlobalLootModifiersGenerator(gen.getPackOutput(),completablefuture,exHelper,CPMain.MODNAME+" global_modifiers"));
		gen.addProvider(event.includeServer(),new CPLootGenerator(gen,completablefuture));
		gen.addProvider(event.includeClient()||event.includeServer(),new CPStatesProvider(gen, CPMain.MODID, exHelper));
		gen.addProvider(event.includeServer(),new CPBookGenerator(gen.getPackOutput(), exHelper));
		/*gen.addProvider(event.includeServer()||event.includeClient(),new PackMetadataGenerator(gen.getPackOutput()).add(PackMetadataSection.TYPE,new PackMetadataSection(MutableComponent.create(new TranslatableContents("pack.caupona.title",CPMain.MODNAME+" Data",new Object[0])),
            DetectedVersion.BUILT_IN.getPackVersion(PackType.SERVER_DATA),
            Optional.of(new InclusiveRange<>(0, Integer.MAX_VALUE)))));*/
		gen.addProvider(event.includeServer(),new CPRegistryGenerator(gen.getPackOutput(),completablefuture));
		gen.addProvider(event.includeClient(),new FluidAnimationGenerator(gen.getPackOutput(),exHelper));
		gen.addProvider(event.includeClient()||event.includeServer(), new RegistryJavaGenerator(gen.getPackOutput(),exHelper));
		
	}
}
