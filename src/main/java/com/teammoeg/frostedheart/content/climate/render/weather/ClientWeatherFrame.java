/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.render.weather;

import com.teammoeg.frostedheart.content.climate.gamedata.climate.MutableVisualWeatherSample;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WhiteCurtainFieldModel;

/** One reusable, immutable-for-the-frame view of {@link ClientWeatherState}. */
public final class ClientWeatherFrame {
    public enum Ownership {
        CUSTOM,
        WALL_ONLY,
        FALLBACK
    }

    public static final ClientWeatherFrame INSTANCE = new ClientWeatherFrame();
    private final MutableVisualWeatherSample cameraSample = new MutableVisualWeatherSample();
    private ClientWeatherState state;
    private WeatherQualityProfile profile = WeatherQualityProfile.FAST;
    private Ownership ownership = Ownership.FALLBACK;
    private double cameraX;
    private double cameraY;
    private double cameraZ;
    private float partialTick;
    private long frameSerial;
    private boolean valid;

    private ClientWeatherFrame() {
    }

    /** One begin per render frame; later weather hooks only read these frozen primitives. */
    public void begin(ClientWeatherState state, WeatherRenderingMode mode,
                      double cameraX, double cameraY, double cameraZ, float partialTick) {
        frameSerial++;
        if (state == null || mode == null || !mode.isSpatial() || !state.hasGrid()) {
            invalidate();
            return;
        }
        this.state = state;
        this.profile = mode.profile();
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.partialTick = partialTick;
        state.sampleGrid(cameraX, cameraZ, partialTick, cameraSample);
        applyCameraExposure(cameraSample);
        ownership = state.hasPrecipitationFootprint()
                ? Ownership.CUSTOM
                : state.wallCandidateCount() > 0 ? Ownership.WALL_ONLY : Ownership.FALLBACK;
        valid = true;
    }

    public void invalidate() {
        state = null;
        ownership = Ownership.FALLBACK;
        cameraSample.clear();
        valid = false;
    }

    public boolean valid() {
        return valid;
    }

    public Ownership ownership() {
        return valid ? ownership : Ownership.FALLBACK;
    }

    public boolean ownsPrecipitation() {
        return ownership() == Ownership.CUSTOM;
    }

    public MutableVisualWeatherSample cameraSample() {
        return cameraSample;
    }

    public void sample(double blockX, double blockZ, MutableVisualWeatherSample out) {
        if (!valid) {
            out.clear();
            return;
        }
        state.sampleGrid(blockX, blockZ, partialTick, out);
        applyCameraExposure(out);
    }

    public void samplePrecipitation(double blockX, double blockZ, MutableVisualWeatherSample out) {
        if (!valid) {
            out.clear();
            return;
        }
        state.samplePrecipitation(blockX, blockZ, partialTick, out);
        applyCameraExposure(out);
    }

    private void applyCameraExposure(MutableVisualWeatherSample sample) {
        state.applyCameraExposure(sample);
    }

    public int wallCount() {
        return valid ? state.wallCandidateCount() : 0;
    }

    public WhiteCurtainFieldModel.VisualKernel wallKernel(int index) {
        return state.wallKernel(index);
    }

    public WeatherQualityProfile profile() {
        return profile;
    }

    public double cameraX() {
        return cameraX;
    }

    public double cameraY() {
        return cameraY;
    }

    public double cameraZ() {
        return cameraZ;
    }

    public float partialTick() {
        return partialTick;
    }

    public long frameSerial() {
        return frameSerial;
    }
}
