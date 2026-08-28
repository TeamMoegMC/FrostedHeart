/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

/** Caller-owned mutable gameplay environment query result. */
public final class ThermalEnvironmentSample {
    private boolean airAvailable;
    private double airTemperatureC = Double.NaN;
    private double radiantFluxWPerM2;
    private int mediumId = -1;
    private float confidence;
    private long sampleTick = -1L;
    private int cellFlags;
    private int flags;

    public boolean valid() { return airAvailable || radiantFluxWPerM2 > 0.0D; }
    public boolean airAvailable() { return airAvailable; }
    public double airTemperatureC() { return airTemperatureC; }
    public double radiantFluxWPerM2() { return radiantFluxWPerM2; }
    public int mediumId() { return mediumId; }
    public float confidence() { return confidence; }
    public long sampleTick() { return sampleTick; }
    public int cellFlags() { return cellFlags; }
    public int flags() { return flags; }

    public void clear() {
        airAvailable = false;
        airTemperatureC = Double.NaN;
        radiantFluxWPerM2 = 0.0D;
        mediumId = -1;
        confidence = 0.0F;
        sampleTick = -1L;
        cellFlags = 0;
        flags = 0;
    }

    void setAir(double temperature, int medium, int cellFlags, long tick) {
        airAvailable = true;
        airTemperatureC = temperature;
        mediumId = medium;
        this.cellFlags = cellFlags;
        sampleTick = tick;
        confidence = Math.max(confidence, 1.0F);
    }

    void setRadiation(double flux, float confidence) {
        radiantFluxWPerM2 = flux;
        this.confidence = airAvailable
                ? Math.min(this.confidence, confidence) : confidence;
    }

    void setComposedAir(double temperature) {
        airAvailable = true;
        airTemperatureC = temperature;
    }

    void addFlag(int flag) {
        flags |= flag;
    }
}
