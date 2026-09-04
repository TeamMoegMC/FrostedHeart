/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.source.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Frozen physical defaults used by the Minecraft thermal runtime. */
public final class MinecraftPhysicalSourceProfile {
    private static final double SHARE_TOLERANCE = 1.0e-12D;

    private final int profileId;
    private final double ratedPowerW;
    private final Port[] ports;
    private final double radiativePowerShare;
    private final double radiationOffsetX;
    private final double radiationOffsetY;
    private final double radiationOffsetZ;
    private final double radiationDirectionalUpperBound;

    public static final MinecraftPhysicalSourceProfile CAMPFIRE =
            campfire(8_000.0D, 0.2D);

    public static MinecraftPhysicalSourceProfile campfire(
            double ratedPowerW,
            double radiationShare
    ) {
        return new MinecraftPhysicalSourceProfile(
                    1,
                    ratedPowerW,
                    new Port[]{
                            Port.airFace(
                                    0, 1.0D - radiationShare,
                                    0, 1, 0,
                                    ConservativeAirGeometry.Face.NEGATIVE_Y),
                            Port.radiationLoss(1, radiationShare)
                    },
                    0.5D, 0.75D, 0.5D, 1.0D);
    }

    public static final MinecraftPhysicalSourceProfile GENERATOR =
            new MinecraftPhysicalSourceProfile(
                    2,
                    10_000.0D,
                    new Port[]{
                            Port.airFace(
                                    0, 0.8D,
                                    0, 0, 0,
                                    ConservativeAirGeometry.Face.NEGATIVE_Y),
                            Port.radiationLoss(1, 0.2D)
                    },
                    0.5D, 0.5D, 0.5D, 1.0D);

    public static final MinecraftPhysicalSourceProfile FOUNTAIN =
            new MinecraftPhysicalSourceProfile(
                    3,
                    2_000.0D,
                    new Port[]{
                            Port.airFace(
                                    0, 0.9D,
                                    0, 0, 0,
                                    ConservativeAirGeometry.Face.NEGATIVE_Y),
                            Port.radiationLoss(1, 0.1D)
                    },
                    0.5D, 0.5D, 0.5D, 1.0D);

    public static final MinecraftPhysicalSourceProfile RADIATOR =
            new MinecraftPhysicalSourceProfile(
                    4,
                    4_000.0D,
                    new Port[]{
                            Port.airFace(
                                    0, 0.9D,
                                    0, 0, 0,
                                    ConservativeAirGeometry.Face.NEGATIVE_Y),
                            Port.radiationLoss(1, 0.1D)
                    },
                    0.5D, 0.5D, 0.5D, 1.0D);

    public static MinecraftPhysicalSourceProfile byId(int profileId) {
        return switch (profileId) {
            case 1 -> CAMPFIRE;
            case 2 -> GENERATOR;
            case 3 -> FOUNTAIN;
            case 4 -> RADIATOR;
            default -> throw new IllegalArgumentException(
                    "unknown Minecraft physical source profile: " + profileId);
        };
    }

    public MinecraftPhysicalSourceProfile(
            int profileId,
            double ratedPowerW,
            Port[] ports,
            double radiationOffsetX,
            double radiationOffsetY,
            double radiationOffsetZ,
            double radiationDirectionalUpperBound
    ) {
        if (profileId < 0) {
            throw new IllegalArgumentException("profileId must be non-negative");
        }
        if (!Double.isFinite(ratedPowerW) || ratedPowerW < 0.0D) {
            throw new IllegalArgumentException("ratedPowerW must be finite and non-negative");
        }
        if (!Double.isFinite(radiationOffsetX)
                || !Double.isFinite(radiationOffsetY)
                || !Double.isFinite(radiationOffsetZ)) {
            throw new IllegalArgumentException("radiation origin offsets must be finite");
        }
        if (!Double.isFinite(radiationDirectionalUpperBound)
                || radiationDirectionalUpperBound <= 0.0D) {
            throw new IllegalArgumentException(
                    "radiationDirectionalUpperBound must be finite and positive");
        }
        ports = Objects.requireNonNull(ports, "ports").clone();
        if (ports.length == 0) {
            throw new IllegalArgumentException("a physical source requires at least one port");
        }
        Set<Integer> portIds = new HashSet<>();
        double share = 0.0D;
        double radiationShare = 0.0D;
        for (Port port : ports) {
            Objects.requireNonNull(port, "ports contains null");
            if (!portIds.add(port.portId())) {
                throw new IllegalArgumentException("duplicate portId: " + port.portId());
            }
            share += port.powerShare();
            if (port.kind() == PortKind.RADIATION_LOSS) {
                radiationShare += port.powerShare();
            }
        }
        if (!Double.isFinite(share) || Math.abs(share - 1.0D) > SHARE_TOLERANCE) {
            throw new IllegalArgumentException("physical source port shares must sum to one");
        }
        this.profileId = profileId;
        this.ratedPowerW = ratedPowerW;
        this.ports = ports;
        this.radiativePowerShare = radiationShare;
        this.radiationOffsetX = radiationOffsetX;
        this.radiationOffsetY = radiationOffsetY;
        this.radiationOffsetZ = radiationOffsetZ;
        this.radiationDirectionalUpperBound =
                radiationDirectionalUpperBound;
    }

    public int profileId() { return profileId; }
    public double ratedPowerW() { return ratedPowerW; }
    public double radiationOffsetX() { return radiationOffsetX; }
    public double radiationOffsetY() { return radiationOffsetY; }
    public double radiationOffsetZ() { return radiationOffsetZ; }
    public double radiationDirectionalUpperBound() {
        return radiationDirectionalUpperBound;
    }

    /** Safe indexed access for runtime code that must not clone the port table. */
    public int portCount() {
        return ports.length;
    }

    /** Ports are immutable values; returning one element does not expose the array. */
    public Port port(int index) {
        return ports[index];
    }

    public double powerForLevel(double level) {
        if (!Double.isFinite(level) || level < 0.0D) {
            throw new IllegalArgumentException("source level must be finite and non-negative");
        }
        double power = ratedPowerW * level;
        if (!Double.isFinite(power)) {
            throw new ArithmeticException("physical source power exceeded the finite domain");
        }
        return power == 0.0D ? 0.0D : power;
    }

    public double radiativePowerW(double totalPowerW) {
        if (!Double.isFinite(totalPowerW) || totalPowerW < 0.0D) {
            throw new IllegalArgumentException("totalPowerW must be finite and non-negative");
        }
        return totalPowerW * radiativePowerShare;
    }

    public enum PortKind {
        AIR_FACE,
        RADIATION_LOSS
    }

    public record Port(
            int portId,
            double powerShare,
            PortKind kind,
            int offsetX,
            int offsetY,
            int offsetZ,
            ConservativeAirGeometry.Face targetFace
    ) {
        public Port {
            if (portId < 0) {
                throw new IllegalArgumentException("portId must be non-negative");
            }
            if (!Double.isFinite(powerShare) || powerShare < 0.0D || powerShare > 1.0D) {
                throw new IllegalArgumentException("powerShare must be finite and in [0, 1]");
            }
            Objects.requireNonNull(kind, "kind");
            if ((kind == PortKind.AIR_FACE) != (targetFace != null)) {
                throw new IllegalArgumentException("only AIR_FACE ports carry a target face");
            }
            if (kind != PortKind.AIR_FACE
                    && (offsetX != 0 || offsetY != 0 || offsetZ != 0)) {
                throw new IllegalArgumentException("sink ports cannot carry a world offset");
            }
        }

        public static Port airFace(
                int portId,
                double powerShare,
                int offsetX,
                int offsetY,
                int offsetZ,
                ConservativeAirGeometry.Face targetFace
        ) {
            return new Port(
                    portId, powerShare, PortKind.AIR_FACE,
                    offsetX, offsetY, offsetZ, targetFace);
        }

        public static Port radiationLoss(
                int portId,
                double powerShare
        ) {
            return new Port(
                    portId, powerShare, PortKind.RADIATION_LOSS,
                    0, 0, 0, null);
        }
    }
}
