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

import com.google.common.collect.ImmutableSet;
import com.teammoeg.caupona.CPMain;
import com.teammoeg.caupona.data.loot.AddPoolLootModifier;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;


public class CPGlobalLootModifiersGenerator extends GlobalLootModifierProvider {

	public CPGlobalLootModifiersGenerator(PackOutput output,CompletableFuture<HolderLookup.Provider> provider, ExistingFileHelper helper, String name) {
		super(output,provider, CPMain.MODID);
	}

	@Override
	protected void start() {
		for(ResourceKey<LootTable> table:ImmutableSet.of(
				BuiltInLootTables.ABANDONED_MINESHAFT,
				BuiltInLootTables.BURIED_TREASURE,
				BuiltInLootTables.ANCIENT_CITY,
				BuiltInLootTables.DESERT_PYRAMID,
				BuiltInLootTables.IGLOO_CHEST,
				BuiltInLootTables.JUNGLE_TEMPLE,
				BuiltInLootTables.PILLAGER_OUTPOST,
				BuiltInLootTables.SHIPWRECK_TREASURE,
				BuiltInLootTables.SIMPLE_DUNGEON,
				BuiltInLootTables.STRONGHOLD_CORRIDOR,
				BuiltInLootTables.STRONGHOLD_CROSSING,
				BuiltInLootTables.UNDERWATER_RUIN_BIG,
				BuiltInLootTables.UNDERWATER_RUIN_SMALL,
				BuiltInLootTables.WOODLAND_MANSION
				)) {
			this.add(table.location().getPath(), AddPoolLootModifier.builder(CPLootGenerator.ASSES.location()).when(LootTableIdCondition.builder(table.location())).build());
		}
	}
}
