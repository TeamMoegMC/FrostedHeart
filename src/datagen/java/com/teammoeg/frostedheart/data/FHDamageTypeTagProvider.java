/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.data;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.reference.FHDamageTypes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class FHDamageTypeTagProvider  extends TagsProvider<DamageType> {
    protected FHDamageTypeTagProvider(PackOutput output,
                                      CompletableFuture<HolderLookup.Provider> provider,
                                      @Nullable ExistingFileHelper existingFileHelper) {
        super(output, Registries.DAMAGE_TYPE, provider, FHMain.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        tag(DamageTypeTags.BYPASSES_ARMOR).add(
                FHDamageTypes.BLIZZARD,
                FHDamageTypes.RAD,
                FHDamageTypes.HYPERTHERMIA,
                FHDamageTypes.HYPOTHERMIA,
                FHDamageTypes.HYPERTHERMIA_INSTANT,
                FHDamageTypes.HYPOTHERMIA_INSTANT,
                FHDamageTypes.THIRST
                );

        tag(DamageTypeTags.IS_FREEZING).add(
                FHDamageTypes.BLIZZARD,
                FHDamageTypes.HYPOTHERMIA,
                FHDamageTypes.HYPOTHERMIA_INSTANT
        );

        tag(DamageTypeTags.IS_FIRE).add(
                FHDamageTypes.HYPERTHERMIA,
                FHDamageTypes.HYPERTHERMIA_INSTANT
        );
    }
}
