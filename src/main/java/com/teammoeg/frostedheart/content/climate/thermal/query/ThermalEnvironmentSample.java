/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.query;

/** Caller-owned mutable gameplay environment query result. */
public final class ThermalEnvironmentSample {
    private boolean airAvailable;
    private double airTemperatureC = Double.NaN;
    private double radiantFluxWPerM2;

    public boolean airAvailable() { return airAvailable; }
    public double airTemperatureC() { return airTemperatureC; }
    public double radiantFluxWPerM2() { return radiantFluxWPerM2; }

    public void clear() {
        airAvailable = false;
        airTemperatureC = Double.NaN;
        radiantFluxWPerM2 = 0.0D;
    }

    public void setAir(double temperature) {
        airAvailable = true;
        airTemperatureC = temperature;
    }

    public void setRadiation(double flux) {
        radiantFluxWPerM2 = flux;
    }

    public void setComposedAir(double temperature) {
        airAvailable = true;
        airTemperatureC = temperature;
    }

}
