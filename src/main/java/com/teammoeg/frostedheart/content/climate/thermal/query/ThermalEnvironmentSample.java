/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.query;

/** Caller-owned mutable gameplay environment query result. */
public final class ThermalEnvironmentSample {
    private boolean airAvailable;
    private double airTemperatureC = Double.NaN;
    private double radiantFluxWPerM2;
    private boolean sharedAirRegion;
    private long sharedAirRegionBlock;

    public boolean airAvailable() { return airAvailable; }
    public double airTemperatureC() { return airTemperatureC; }
    public double radiantFluxWPerM2() { return radiantFluxWPerM2; }
    public boolean sharedAirRegion() { return sharedAirRegion; }
    public long sharedAirRegionBlock() { return sharedAirRegionBlock; }

    public void clear() {
        airAvailable = false;
        airTemperatureC = Double.NaN;
        radiantFluxWPerM2 = 0.0D;
        sharedAirRegion = false;
        sharedAirRegionBlock = 0;
    }

    public void setAir(double temperature) {
        airAvailable = true;
        airTemperatureC = temperature;
        sharedAirRegion = false;
    }

    public void setAirFromRegion(double temperature, long regionBlock) {
        setAir(temperature);
        sharedAirRegion = true;
        sharedAirRegionBlock = regionBlock;
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
        sharedAirRegionBlock = source.sharedAirRegionBlock;
    }

}
