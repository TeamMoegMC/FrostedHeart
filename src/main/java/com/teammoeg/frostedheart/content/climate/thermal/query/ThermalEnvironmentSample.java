/*
 * Copyright (c) 2026 TeamMoeg
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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.query;

/** Caller-owned mutable gameplay environment query result. */
public final class ThermalEnvironmentSample {
    private boolean airAvailable;
    private double airTemperatureC = Double.NaN;
    private double radiantFluxWPerM2;
    private boolean sharedAirRegion;
    private long airSampleTick = -1;
    private int airBasisTerms;
    private boolean airMinimumQuality;

    public boolean airAvailable() { return airAvailable; }
    public double airTemperatureC() { return airTemperatureC; }
    public double radiantFluxWPerM2() { return radiantFluxWPerM2; }
    public boolean sharedAirRegion() { return sharedAirRegion; }
    public long airSampleTick() { return airSampleTick; }
    public int airBasisTerms() { return airBasisTerms; }
    public boolean airMinimumQuality() { return airMinimumQuality; }

    public void clear() {
        airAvailable = false;
        airTemperatureC = Double.NaN;
        radiantFluxWPerM2 = 0.0D;
        sharedAirRegion = false;
        airSampleTick = -1;
        airBasisTerms = 0;
        airMinimumQuality = false;
    }

    public void setAir(double temperature) {
        airAvailable = true;
        airTemperatureC = temperature;
        sharedAirRegion = false;
        airSampleTick = -1;
        airBasisTerms = 0;
        airMinimumQuality = false;
    }

    public void setAir(double temperature, long sampleTick, int basisTerms, boolean minimumQuality) {
        setAir(temperature);
        airSampleTick = sampleTick;
        airBasisTerms = basisTerms;
        airMinimumQuality = minimumQuality;
    }

    public void setAirFromRegion(double temperature, long sampleTick, int basisTerms, boolean minimumQuality) {
        setAir(temperature, sampleTick, basisTerms, minimumQuality);
        sharedAirRegion = true;
    }

    public void setRadiation(double flux) {
        radiantFluxWPerM2 = flux;
    }

    public void setComposedAir(double temperature) {
        airAvailable = true;
        airTemperatureC = temperature;
    }

    public void copyFrom(ThermalEnvironmentSample source) {
        airAvailable = source.airAvailable;
        airTemperatureC = source.airTemperatureC;
        radiantFluxWPerM2 = source.radiantFluxWPerM2;
        sharedAirRegion = source.sharedAirRegion;
        airSampleTick = source.airSampleTick;
        airBasisTerms = source.airBasisTerms;
        airMinimumQuality = source.airMinimumQuality;
    }

}
