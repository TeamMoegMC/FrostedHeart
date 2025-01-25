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

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Note: Use a null source entity to represent environmental damage so that no knockback is applied!
 */
public class FHDamageSources {
    // Source and destination entity are different
    public static DamageSource source(Level level, ResourceKey<DamageType> type, Entity source, Entity dest) {
    	return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),dest,source);
    }

    // Destination and source entity are the same
    public static DamageSource source(Level level, ResourceKey<DamageType> type, Entity dest) {
    	return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),dest);
    }

    // Environmental damage with no knockback!
    public static DamageSource source(Level level, ResourceKey<DamageType> type, Vec3 pos) {
    	return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),pos);
    }

    // Environmental damage with no knockback!
    public static DamageSource source(Level level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
    }

    public static DamageSource hypothermia(Level level) {
        return source(level, FHDamageTypes.HYPOTHERMIA);
    }

    public static DamageSource hyperthermia(Level level) {
        return source(level, FHDamageTypes.HYPERTHERMIA);
    }

    public static DamageSource blizzard(Level level) {
        return source(level, FHDamageTypes.BLIZZARD);
    }

    public static DamageSource radiation(Level level) {
        return source(level, FHDamageTypes.RAD);
    }

    public static DamageSource hypothermiaInstant(Level level) {
        return source(level, FHDamageTypes.HYPOTHERMIA_INSTANT);
    }

    public static DamageSource hyperthermiaInstant(Level level) {
        return source(level, FHDamageTypes.HYPERTHERMIA_INSTANT);
    }
}
