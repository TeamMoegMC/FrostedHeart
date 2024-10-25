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

import com.teammoeg.frostedheart.FHDamageSources;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.world.damagesource.DamageType;

public class FHDamageTypeProvider {

    public static void bootstrap(BootstapContext<DamageType> ctx) {
        ctx.register(FHDamageSources.BLIZZARD, new DamageType("frostedheart_blizzard", 0.0F));
        ctx.register(FHDamageSources.RAD, new DamageType("frostedheart_radiation", 0.0F));
        ctx.register(FHDamageSources.HYPERTHERMIA, new DamageType("frostedheart_hyperthermia", 0.0F));
        ctx.register(FHDamageSources.HYPOTHERMIA, new DamageType("frostedheart_hypothermia", 0.0F));
        ctx.register(FHDamageSources.HYPERTHERMIA_INSTANT, new DamageType("frostedheart_hyperthermia_instant", 0.0F));
        ctx.register(FHDamageSources.HYPOTHERMIA_INSTANT, new DamageType("frostedheart_hypothermia_instant", 0.0F));
    }
}
