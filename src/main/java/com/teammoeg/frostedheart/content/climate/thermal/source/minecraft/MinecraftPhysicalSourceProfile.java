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
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceChannel;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Frozen physical defaults used only by the explicitly enabled shadow runtime. */
public record MinecraftPhysicalSourceProfile(
        int profileId,
        double ratedPowerW,
        MissingPortPolicy missingPortPolicy,
        Port[] ports,
        double radiationOffsetX,
        double radiationOffsetY,
        double radiationOffsetZ,
        double radiationDirectionalUpperBound
) {
    private static final double SHARE_TOLERANCE = 1.0e-12D;

    public static final MinecraftPhysicalSourceProfile CAMPFIRE =
            new MinecraftPhysicalSourceProfile(
                    1,
                    8_000.0D,
                    MissingPortPolicy.EXPLICIT_LOSS,
                    new Port[]{
                            Port.airFace(
                                    0, SourceChannel.CONVECTION, 0.8D,
                                    0, 1, 0,
                                    ConservativeAirGeometry.Face.NEGATIVE_Y),
                            Port.declaredLoss(1, SourceChannel.RADIATION, 0.2D)
                    },
                    0.5D, 0.75D, 0.5D, 1.0D);

    public static final MinecraftPhysicalSourceProfile GENERATOR =
            new MinecraftPhysicalSourceProfile(
                    2,
                    10_000.0D,
                    MissingPortPolicy.INTERNAL_HEAT,
                    new Port[]{
                            Port.airFace(
                                    0, SourceChannel.CONVECTION, 0.7D,
                                    0, 0, 0,
                                    ConservativeAirGeometry.Face.NEGATIVE_Y),
                            Port.internalHeat(1, SourceChannel.CONTACT, 0.1D),
                            Port.declaredLoss(2, SourceChannel.RADIATION, 0.2D)
                    },
                    0.5D, 0.5D, 0.5D, 1.0D);

    public MinecraftPhysicalSourceProfile(
            int profileId,
            double ratedPowerW,
            MissingPortPolicy missingPortPolicy,
            Port[] ports
    ) {
        this(profileId, ratedPowerW, missingPortPolicy, ports,
                0.5D, 0.5D, 0.5D, 1.0D);
    }

    public MinecraftPhysicalSourceProfile {
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
        Objects.requireNonNull(missingPortPolicy, "missingPortPolicy");
        ports = Objects.requireNonNull(ports, "ports").clone();
        if (ports.length == 0) {
            throw new IllegalArgumentException("a physical source requires at least one port");
        }
        Set<Integer> portIds = new HashSet<>();
        double share = 0.0D;
        for (Port port : ports) {
            Objects.requireNonNull(port, "ports contains null");
            if (!portIds.add(port.portId())) {
                throw new IllegalArgumentException("duplicate portId: " + port.portId());
            }
            share += port.powerShare();
        }
        if (!Double.isFinite(share) || Math.abs(share - 1.0D) > SHARE_TOLERANCE) {
            throw new IllegalArgumentException("physical source port shares must sum to one");
        }
    }

    @Override
    public Port[] ports() {
        return ports.clone();
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
        double share = 0.0D;
        for (Port port : ports) {
            if (port.channel() == SourceChannel.RADIATION) {
                share += port.powerShare();
            }
        }
        return totalPowerW * share;
    }

    public enum MissingPortPolicy {
        REDISTRIBUTE_TO_VALID_PORTS,
        INTERNAL_HEAT,
        EXPLICIT_LOSS
    }

    public enum PortKind {
        AIR_FACE,
        INTERNAL_HEAT,
        DECLARED_LOSS
    }

    public record Port(
            int portId,
            SourceChannel channel,
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
            Objects.requireNonNull(channel, "channel");
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
                SourceChannel channel,
                double powerShare,
                int offsetX,
                int offsetY,
                int offsetZ,
                ConservativeAirGeometry.Face targetFace
        ) {
            return new Port(
                    portId, channel, powerShare, PortKind.AIR_FACE,
                    offsetX, offsetY, offsetZ, targetFace);
        }

        public static Port internalHeat(
                int portId,
                SourceChannel channel,
                double powerShare
        ) {
            return new Port(
                    portId, channel, powerShare, PortKind.INTERNAL_HEAT,
                    0, 0, 0, null);
        }

        public static Port declaredLoss(
                int portId,
                SourceChannel channel,
                double powerShare
        ) {
            return new Port(
                    portId, channel, powerShare, PortKind.DECLARED_LOSS,
                    0, 0, 0, null);
        }
    }
}
