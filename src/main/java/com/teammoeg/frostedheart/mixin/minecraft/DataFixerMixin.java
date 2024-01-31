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

package com.teammoeg.frostedheart.mixin.minecraft;

import com.mojang.datafixers.DataFixerBuilder;
import com.teammoeg.frostedheart.util.mixin.LazyDataFixerBuilder;

import net.minecraft.util.datafix.DataFixesManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({DataFixesManager.class})
public class DataFixerMixin {

    @Redirect(method = "createFixer", at = @At(value = "NEW", target = "com/mojang/datafixers/DataFixerBuilder", remap = false))
    private static DataFixerBuilder create$replaceBuilder(int dataVersion) {
        return new LazyDataFixerBuilder(dataVersion);
    }
}
