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

import java.util.function.Function;

import com.teammoeg.frostedheart.FHDamageTypes;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;

public class FHDamageTypeProvider {
	public static void bootstrap(BootstapContext<DamageType> pContext) {
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

}
