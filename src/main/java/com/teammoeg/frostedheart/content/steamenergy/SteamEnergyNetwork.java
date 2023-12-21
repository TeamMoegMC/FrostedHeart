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

package com.teammoeg.frostedheart.content.steamenergy;

import net.minecraft.tileentity.TileEntity;

public class SteamEnergyNetwork {
    private HeatController provider;
    private boolean isValid = true;

    public SteamEnergyNetwork(HeatController provider) {
        this.provider = provider;
    }
    
    public boolean hasEnoughHeat(float val) {
    	return provider.getMaxHeat()>=val;
    }
    public float drainHeat(float val) {
        return provider.drainHeat(val);
    }
    
    public float fillHeat(float val) {
    	return provider.fillHeat(val);
    }
    
    public float getTemperatureLevel() {
        return provider.getTemperatureLevel();
    }

    @Override
    public String toString() {
        return "SteamEnergyNetwork [provider=" + provider + ", isValid=" + isValid + (isValid() ? (", temp=" + this.getTemperatureLevel() + ", power=" + this.provider.getMaxHeat()) : "") + "]";
    }

    public boolean isValid() {
        return isValid && (provider instanceof TileEntity) && (!((TileEntity) provider).isRemoved());
    }

    public void invalidate() {
        isValid = false;
    }
}
