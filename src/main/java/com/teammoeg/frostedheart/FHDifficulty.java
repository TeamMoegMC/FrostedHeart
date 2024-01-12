/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart;

import java.util.function.Function;

import net.minecraft.entity.player.ServerPlayerEntity;

public enum FHDifficulty {
    Easy(s -> 0.05F),
    Normal(s -> 0.036F),
    Hard(s -> s.isSprinting() ? 0.036F : 0.024F),
    HardCore(s -> 0F);

    public final Function<ServerPlayerEntity, Float> self_heat;

    private FHDifficulty(Function<ServerPlayerEntity, Float> self_heat) {
        this.self_heat = self_heat;

    }
}
