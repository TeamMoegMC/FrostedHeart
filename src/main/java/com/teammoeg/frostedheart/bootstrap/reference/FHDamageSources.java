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

import com.teammoeg.chorda.util.CDamageSourceHelper;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;


public class FHDamageSources {
	public static DamageSource hypothermia(Level level) {
        return CDamageSourceHelper.source(level, FHDamageTypes.HYPOTHERMIA);
    }

    public static DamageSource hyperthermia(Level level) {
        return CDamageSourceHelper.source(level, FHDamageTypes.HYPERTHERMIA);
    }

    public static DamageSource blizzard(Level level) {
        return CDamageSourceHelper.source(level, FHDamageTypes.BLIZZARD);
    }

    public static DamageSource radiation(Level level) {
        return CDamageSourceHelper.source(level, FHDamageTypes.RAD);
    }

    public static DamageSource hypothermiaInstant(Level level) {
        return CDamageSourceHelper.source(level, FHDamageTypes.HYPOTHERMIA_INSTANT);
    }

    public static DamageSource hyperthermiaInstant(Level level) {
        return CDamageSourceHelper.source(level, FHDamageTypes.HYPERTHERMIA_INSTANT);
    }
}
