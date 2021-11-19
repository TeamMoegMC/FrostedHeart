/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.util;

import java.util.Optional;
import java.util.function.Supplier;

import net.minecraftforge.fml.ModList;

public enum Mods {
    DIET,
    CREATE,
    IMMERSIVEENGINEERING,
    RANKINE,
    PRIMALWINTER,
    THEONEPROBE,
    STEAMPOWERED,
    FROSTEDHEART,
    CURIOS,
    JEI;

    /**
     * @return a boolean of whether the mod is loaded or not based on mod id
     */
    public boolean isLoaded() {
        return ModList.get().isLoaded(asId());
    }

    /**
     * @return the mod id
     */
    public String asId() {
        return Lang.asId(name());
    }

    /**
     * Simple hook to run code if a mod is installed
     *
     * @param toRun will be run only if the mod is loaded
     * @return Optional.empty() if the mod is not loaded, otherwise an Optional of the return value of the given supplier
     */
    public <T> Optional<T> runIfInstalled(Supplier<Supplier<T>> toRun) {
        if (isLoaded())
            return Optional.of(toRun.get().get());
        return Optional.empty();
    }
}
