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

package com.teammoeg.frostedheart.capability;

public class TempForecastCapability implements ITempForecastCapability {

    private int rainTime, thunderTime, clearTime;

    public TempForecastCapability(int r, int t, int c) {
        this.rainTime = r;
        this.thunderTime = t;
        this.clearTime = c;
    }

    @Override
    public int getRainTime() {
        return rainTime;
    }

    @Override
    public int getThunderTime() {
        return thunderTime;
    }

    @Override
    public int getClearTime() {
        return clearTime;
    }

    @Override
    public void setRainTime(int rainTime) {
        this.rainTime = rainTime;
    }

    @Override
    public void setThunderTime(int thunderTime) {
        this.thunderTime = thunderTime;
    }

    @Override
    public void setClearTime(int clearTime) {
        this.clearTime = clearTime;
    }
}
