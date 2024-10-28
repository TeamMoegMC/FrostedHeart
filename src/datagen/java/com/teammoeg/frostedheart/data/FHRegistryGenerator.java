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

package com.teammoeg.frostedheart.data;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.teammoeg.frostedheart.FHDamageTypes;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;


public class FHRegistryGenerator extends DatapackBuiltinEntriesProvider {
	public FHRegistryGenerator(PackOutput output, CompletableFuture<Provider> registries) {
		super(output, registries,new RegistrySetBuilder()
				.add(Registries.DAMAGE_TYPE,FHRegistryGenerator::bootstrapDamageTypes)
				,Set.of(FHMain.MODID));
	}
	public static void bootstrapDamageTypes(BootstapContext<DamageType> pContext) {
		HolderGetter<DamageType> holder=pContext.lookup(Registries.DAMAGE_TYPE);
		register(pContext, FHDamageTypes.BLIZZARD, t->new DamageType(t, DamageScaling.NEVER, 0.1f, DamageEffects.FREEZING));
		register(pContext, FHDamageTypes.HYPERTHERMIA, t->new DamageType(t, DamageScaling.NEVER, 0, DamageEffects.BURNING));
		register(pContext, FHDamageTypes.HYPOTHERMIA, t->new DamageType(t, DamageScaling.NEVER, 0.1F, DamageEffects.FREEZING));
		register(pContext, FHDamageTypes.HYPERTHERMIA_INSTANT, t->new DamageType(t, DamageScaling.ALWAYS, 0, DamageEffects.BURNING));
		register(pContext, FHDamageTypes.HYPOTHERMIA_INSTANT, t->new DamageType(t, DamageScaling.ALWAYS, 0.1F, DamageEffects.FREEZING));
		register(pContext, FHDamageTypes.RAD, t->new DamageType(t, DamageScaling.ALWAYS, 0.1F, DamageEffects.POKING));
	}
	private static <T> void register(BootstapContext<T> pContext,ResourceKey<T> rk,Function<String,T> supplier) {
		pContext.register(rk, supplier.apply(rk.location().getPath()));
	}
	public static Block block(String type) {
		return BuiltInRegistries.BLOCK.get(new ResourceLocation(FHMain.MODID,type));
	}
	@Override
	public String getName() {
		return "Frostedheart Registry Generator";
	}
}
