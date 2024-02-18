/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.steamenergy.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.content.steamenergy.HeatController;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class SteamEnergyNetwork {
    private HeatController provider;
    private boolean isValid = true;
    ArrayList<BlockPos> tiles=new ArrayList<>();
    BranchNode root;
    Map<BlockPos,BaseNode> nodesByPos=new HashMap<>();
    public void setPosNode(BlockPos pos,BaseNode node) {
    	nodesByPos.put(pos, node);
    }
    public void removePosNode(BlockPos pos) {
    	nodesByPos.remove(pos);
    }
    public SteamEnergyNetwork(HeatController provider) {
        this.provider = provider;
    }

    public float drainHeat(float val) {
        return provider.drainHeat(val);
    }

    public float fillHeat(float val) {
        return provider.fillHeat(val);
    }

    public HeatController getController() {
        return provider;
    }

    public float getTemperatureLevel() {
        return provider.getTemperatureLevel();
    }

    public boolean hasEnoughHeat(float val) {
        return provider.getMaxHeat() >= val;
    }

    public void invalidate() {
        isValid = false;
    }

    public boolean isValid() {
        return isValid && (provider instanceof TileEntity) && (!((TileEntity) provider).isRemoved());
    }

    @Override
    public String toString() {
        return "SteamEnergyNetwork [provider=" + provider + ", isValid=" + isValid + (isValid() ? (", temp=" + this.getTemperatureLevel() + ", power=" + this.provider.getMaxHeat()) : "") + "]";
    }
}
