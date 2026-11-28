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
    private final double inverseRadiusSquared;
    private final double upperExtent;
    private final double lowerExtent;
    private final double temperatureC;
    private final double centerBoostC;

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

    /**
     * Creates a sphere with an additional temperature offset at its center.
     * The offset tapers as {@code (1 - distanceSquared / radiusSquared)^2},
     * reaching zero with zero slope at the boundary.
     *
     * @param key provider-scoped field identity
     * @param priority ordering within the combine mode
     * @param combineMode how the field modifies the queried temperature
     * @param x center X coordinate, in blocks
     * @param y center Y coordinate, in blocks
     * @param z center Z coordinate, in blocks
     * @param radius sphere radius, in blocks
     * @param temperatureC field value at the boundary, in degrees Celsius
     * @param centerBoostC additional center value, in degrees Celsius
     */
    public ThermalAnalyticField(
            ThermalFieldKey key, int priority, CombineMode combineMode,
            double x, double y, double z, double radius,
            double temperatureC, double centerBoostC
    ) {
        this(key, priority, combineMode, Shape.SPHERE,
                x, y, z, radius, radius, radius, temperatureC, centerBoostC);
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
        this(key, priority, combineMode, shape, centerX, centerY, centerZ,
                radius, upperExtent, lowerExtent, temperatureC, 0);
    }

    private ThermalAnalyticField(
            ThermalFieldKey key, int priority, CombineMode combineMode, Shape shape,
            double centerX, double centerY, double centerZ, double radius,
            double upperExtent, double lowerExtent, double temperatureC, double centerBoostC
    ) {
        this.combineMode = Objects.requireNonNull(
                combineMode, "combineMode");
        this.shape = Objects.requireNonNull(shape, "shape");
        requireFinite(centerX);
        requireFinite(centerY);
        requireFinite(centerZ);
        requireFinite(temperatureC);
        requireFinite(centerBoostC);
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
        this.inverseRadiusSquared = 1 / radiusSquared;
        this.upperExtent = upperExtent;
        this.lowerExtent = lowerExtent;
        this.temperatureC = temperatureC;
        this.centerBoostC = centerBoostC;
    }

    public ThermalFieldKey key() { return key; }
    public int priority() { return priority; }
    public CombineMode combineMode() { return combineMode; }
    public Shape shape() { return shape; }
    public double centerX() { return centerX; }
    public double centerY() { return centerY; }
    public double centerZ() { return centerZ; }
    public double radius() { return radius; }
    /** Returns the base field value before any center boost, in degrees Celsius. */
    public double temperatureC() { return temperatureC; }

    /**
     * Evaluates the field value at an absolute world position, including its
     * containment test. Boosted spheres reuse that squared distance and the
     * precomputed inverse squared radius; queries do not divide or take a square root.
     *
     * @param x world X coordinate, in blocks
     * @param y world Y coordinate, in blocks
     * @param z world Z coordinate, in blocks
     * @return absolute temperature or delta according to the combine mode, in Celsius;
     *         {@link Double#NaN} outside the field
     */
    public double temperatureCAt(double x, double y, double z) {
        if (centerBoostC == 0) return contains(x, y, z) ? temperatureC : Double.NaN;
        double dx = x - centerX;
        double dy = y - centerY;
        double dz = z - centerZ;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        return SphericalHeatFieldModel.temperatureAt(distanceSquared, radiusSquared, inverseRadiusSquared,
                temperatureC, centerBoostC);
    }

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
            double upperExtent, double lowerExtent, double temperatureC, double centerBoostC) {
        return this.priority == priority && combineMode == mode && this.shape == shape
                && centerX == x && centerY == y && centerZ == z && this.radius == radius
                && this.upperExtent == upperExtent && this.lowerExtent == lowerExtent
                && this.temperatureC == temperatureC && this.centerBoostC == centerBoostC;
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
