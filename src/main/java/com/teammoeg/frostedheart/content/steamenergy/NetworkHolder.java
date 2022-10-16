/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.content.steamenergy;

public class NetworkHolder {
    SteamEnergyNetwork sen;
    int dist;
    int counter;

    public NetworkHolder() {
    }

    public void connect(SteamEnergyNetwork sen, int dist) {
        this.counter = 10;
        this.sen = sen;
        this.dist = dist;
    }

    public void tick() {
        counter--;
        if (counter < 0) {
            this.sen = null;
            this.dist = Integer.MAX_VALUE;
        }
    }
    public boolean tryDrainHeat(float val) {
    	if (!isValid())return false;
    	if(sen.hasEnoughHeat(val)) {
    		sen.drainHeat(val);
    		return true;
    	}
    	return false;
    }
    public float drainHeat(float val) {
        if (!isValid()) return 0;
        return sen.drainHeat(val);
    }

    public float getTemperatureLevel() {
        if (!isValid()) return 0;
        return sen.getTemperatureLevel();
    }

    public SteamEnergyNetwork getNetwork() {
        return sen;
    }

    public boolean isValid() {
        return sen != null && sen.isValid();
    }

    public int getDistance() {
        return dist;
    }

    @Override
    public String toString() {
        return "NetworkHolder [sen=" + sen + ", dist=" + dist + ", counter=" + counter + "]";
    }
}
