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

package com.teammoeg.frostedheart.util.mixin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import top.theillusivec4.diet.DietMod;
import top.theillusivec4.diet.api.DietCapability;
import top.theillusivec4.diet.api.IDietTracker;
import top.theillusivec4.diet.common.capability.DietTrackerCapability;

public class FixedDietProvider implements ICapabilitySerializable<INBT> {

    private static final IDietTracker EMPTY_TRACKER = new DietTrackerCapability.EmptyDietTracker();

    final net.minecraftforge.common.util.LazyOptional<IDietTracker> capability;

    public FixedDietProvider(net.minecraftforge.common.util.LazyOptional<IDietTracker> capability) {
        this.capability = capability;
    }

    @Override
    public void deserializeNBT(INBT nbt) {

        if (DietCapability.DIET_TRACKER != null) {
            DietCapability.DIET_TRACKER.readNBT(capability.orElse(EMPTY_TRACKER), null, nbt);
        } else {
            DietMod.LOGGER.error("Missing Diet capability!");
        }
    }

    @Nonnull
    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@Nonnull Capability<T> cap,
                                                                            @Nullable Direction side) {

        if (DietCapability.DIET_TRACKER != null && capability.isPresent()) {
            return DietCapability.DIET_TRACKER.orEmpty(cap, this.capability);
        }
        DietMod.LOGGER.error("Missing Diet capability!");
        return net.minecraftforge.common.util.LazyOptional.empty();
    }

    @Override
    public INBT serializeNBT() {

        if (DietCapability.DIET_TRACKER != null && capability.isPresent()) {
            return DietCapability.DIET_TRACKER.writeNBT(capability.orElse(EMPTY_TRACKER), null);
        }
        DietMod.LOGGER.error("Missing Diet capability!");
        return new CompoundNBT();
    }

}