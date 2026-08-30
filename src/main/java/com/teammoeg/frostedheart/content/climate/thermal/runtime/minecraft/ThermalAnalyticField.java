/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import java.util.Objects;

/** Immutable non-conservative gameplay field definition. */
public final class ThermalAnalyticField {
    private final long fieldId;
    private final int priority;
    private final CombineMode combineMode;
    private final Shape shape;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double radius;
    private final double upperExtent;
    private final double lowerExtent;
    private final double temperatureC;

    public ThermalAnalyticField(
            long fieldId,
            int priority,
            CombineMode combineMode,
            double x,
            double y,
            double z,
            double radius,
            double temperatureC
    ) {
        this(fieldId, priority, combineMode, Shape.SPHERE,
                x, y, z, radius, radius, radius, temperatureC);
    }

    public ThermalAnalyticField(
            long fieldId,
            int priority,
            CombineMode combineMode,
            Shape shape,
            double centerX,
            double centerY,
            double centerZ,
            double radius,
            double upperExtent,
            double lowerExtent,
            double temperatureC
    ) {
        this.combineMode = Objects.requireNonNull(
                combineMode, "combineMode");
        this.shape = Objects.requireNonNull(shape, "shape");
        requireFinite(centerX);
        requireFinite(centerY);
        requireFinite(centerZ);
        requireFinite(temperatureC);
        if (!Double.isFinite(radius) || radius <= 0.0D
                || !Double.isFinite(upperExtent) || upperExtent < 0.0D
                || !Double.isFinite(lowerExtent) || lowerExtent < 0.0D) {
            throw new IllegalArgumentException(
                    "analytic field dimensions are invalid");
        }
        this.fieldId = fieldId;
        this.priority = priority;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.upperExtent = upperExtent;
        this.lowerExtent = lowerExtent;
        this.temperatureC = temperatureC;
    }

    public long fieldId() { return fieldId; }
    public int priority() { return priority; }
    public CombineMode combineMode() { return combineMode; }
    public Shape shape() { return shape; }
    public double centerX() { return centerX; }
    public double centerY() { return centerY; }
    public double centerZ() { return centerZ; }
    public double radius() { return radius; }
    public double temperatureC() { return temperatureC; }

    boolean contains(double x, double y, double z) {
        double dx = x - centerX;
        double dy = y - centerY;
        double dz = z - centerZ;
        return switch (shape) {
            case CUBE -> Math.abs(dx) <= radius
                    && Math.abs(dy) <= radius
                    && Math.abs(dz) <= radius;
            case PILLAR -> dy <= upperExtent && dy >= -lowerExtent
                    && dx * dx + dz * dz <= radius * radius;
            case SPHERE -> dx * dx + dy * dy + dz * dz <= radius * radius;
        };
    }

    boolean intersectsHorizontalBounds(
            double minX,
            double maxX,
            double minZ,
            double maxZ
    ) {
        return centerX + radius >= minX && centerX - radius <= maxX
                && centerZ + radius >= minZ && centerZ - radius <= maxZ;
    }

    void writeInfrared(float[] output, int offset) {
        output[offset] = (float) centerX;
        output[offset + 1] = (float) centerY;
        output[offset + 2] = (float) centerZ;
        output[offset + 3] = shape.infraredMode;
        output[offset + 4] = (float) temperatureC;
        output[offset + 5] = (float) radius;
        output[offset + 6] = shape == Shape.PILLAR
                ? (float) (centerY + upperExtent) : 0.0F;
        output[offset + 7] = shape == Shape.PILLAR
                ? (float) (centerY - lowerExtent) : 0.0F;
    }

    public enum CombineMode {
        OVERRIDE,
        MAX_HEAT,
        MIN_COOL,
        ADD_DELTA
    }

    public enum Shape {
        CUBE(0.0F),
        PILLAR(1.0F),
        SPHERE(2.0F);

        private final float infraredMode;

        Shape(float infraredMode) {
            this.infraredMode = infraredMode;
        }
    }

    private static void requireFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "analytic field value must be finite");
        }
    }
}
