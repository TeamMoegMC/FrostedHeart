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

package com.teammoeg.frostedheart.content.steamenergy.capabilities;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.world.item.ItemStack;

public class HeatStorageCapability {
    private static final String STEAM_KEY = "steam";
    int maxstore;
    ItemStack container;

    public HeatStorageCapability(ItemStack stack, int maxstorage) {
    }

    protected float getEnergy() {
        return container != null ? container.getOrCreateTag().getFloat(FHMain.MODID + ":" + STEAM_KEY) : 0;
    }

    // TODO: Check
    protected void setEnergy(float energy) {
        if (container != null)
            container.getOrCreateTag().putFloat(FHMain.MODID + ":" + STEAM_KEY, energy);
    }

    public float getEnergyStored() {
        return getEnergy();
    }

    public float receiveEnergy(float value, boolean simulate) {
        float current = getEnergy();
        float actual = Math.min(maxstore - current, value);
        if (!simulate) {
            setEnergy(actual + current);
        }
        return actual;
    }

    public float extractEnergy(float value, boolean simulate) {
        float current = getEnergy();
        float extracted = Math.min(value, current);
        if (!simulate) {
            setEnergy(current - extracted);
        }
        return extracted;
    }
}
