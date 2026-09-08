/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.field;

import java.util.Objects;

/** Immutable non-conservative gameplay field definition. */
public final class ThermalAnalyticField {
    private final ThermalFieldKey key;
    private final int priority;
    private final CombineMode combineMode;
    private final Shape shape;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double radius;
    private final double radiusSquared;
    private final double upperExtent;
    private final double lowerExtent;
    private final double temperatureC;

    public ThermalAnalyticField(
            ThermalFieldKey key,
            int priority,
            CombineMode combineMode,
            double x,
            double y,
            double z,
            double radius,
            double temperatureC
    ) {
        this(key, priority, combineMode, Shape.SPHERE,
                x, y, z, radius, radius, radius, temperatureC);
    }

    public ThermalAnalyticField(
            ThermalFieldKey key,
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
        this.key = Objects.requireNonNull(key, "key");
        this.priority = priority;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.radiusSquared = radius * radius;
        this.upperExtent = upperExtent;
        this.lowerExtent = lowerExtent;
        this.temperatureC = temperatureC;
    }

    public ThermalFieldKey key() { return key; }
    public int priority() { return priority; }
    public CombineMode combineMode() { return combineMode; }
    public Shape shape() { return shape; }
    public double centerX() { return centerX; }
    public double centerY() { return centerY; }
    public double centerZ() { return centerZ; }
    public double radius() { return radius; }
    public double temperatureC() { return temperatureC; }

    public boolean contains(double x, double y, double z) {
        double dx = x - centerX;
        double dy = y - centerY;
        double dz = z - centerZ;
        return switch (shape) {
            case CUBE -> Math.abs(dx) <= radius
                    && Math.abs(dy) <= radius
                    && Math.abs(dz) <= radius;
            case PILLAR -> dy <= upperExtent && dy >= -lowerExtent
                    && dx * dx + dz * dz <= radiusSquared;
            case SPHERE -> dx * dx + dy * dy + dz * dz <= radiusSquared;
        };
    }

    public double min(int axis) {
        return switch (axis) {
            case 0 -> centerX - radius;
            case 1 -> centerY - (shape == Shape.PILLAR ? lowerExtent : radius);
            case 2 -> centerZ - radius;
            default -> throw new IllegalArgumentException("axis");
        };
    }

    public double max(int axis) {
        return switch (axis) {
            case 0 -> centerX + radius;
            case 1 -> centerY + (shape == Shape.PILLAR ? upperExtent : radius);
            case 2 -> centerZ + radius;
            default -> throw new IllegalArgumentException("axis");
        };
    }

    public boolean intersects(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        if (max(0) < minX || min(0) > maxX || max(1) < minY
                || min(1) > maxY || max(2) < minZ || min(2) > maxZ) return false;
        if (shape == Shape.CUBE) return true;
        double dx = Math.max(minX - centerX, Math.max(0, centerX - maxX));
        double dz = Math.max(minZ - centerZ, Math.max(0, centerZ - maxZ));
        double dy = shape == Shape.PILLAR ? 0 : Math.max(minY - centerY, Math.max(0, centerY - maxY));
        return dx * dx + dy * dy + dz * dz <= radiusSquared;
    }

    boolean matches(int priority, CombineMode mode, Shape shape,
            double x, double y, double z, double radius,
            double upperExtent, double lowerExtent, double temperatureC) {
        return this.priority == priority && combineMode == mode && this.shape == shape
                && centerX == x && centerY == y && centerZ == z && this.radius == radius
                && this.upperExtent == upperExtent && this.lowerExtent == lowerExtent
                && this.temperatureC == temperatureC;
    }

    public enum CombineMode {
        FLOOR_FROM_NATURAL,
        OVERRIDE,
        MAX_HEAT,
        MIN_COOL,
        ADD_DELTA
    }

    public enum Shape {
        CUBE,
        PILLAR,
        SPHERE
    }

    private static void requireFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "analytic field value must be finite");
        }
    }
}
