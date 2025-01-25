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

package com.teammoeg.frostedheart.bootstrap.reference;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public class FHDamageTypes {
    public static final ResourceKey<DamageType> HYPOTHERMIA = create("hypothermia");
    public static final ResourceKey<DamageType> HYPERTHERMIA = create("hyperthermia");
    public static final ResourceKey<DamageType> BLIZZARD = create("blizzard");
    public static final ResourceKey<DamageType> RAD = create("radiation");
    public static final ResourceKey<DamageType> HYPOTHERMIA_INSTANT = create("hypothermia_instant");
    public static final ResourceKey<DamageType> HYPERTHERMIA_INSTANT = create("hyperthermia_instant");
    public static ResourceKey<DamageType> create(String name) {
    	return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(FHMain.MODID,name));
    }
}
