/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable lookup of calibrated static FarField impedances. */
public final class FarFieldProfileRegistry {
    public enum Approval {
        CANDIDATE,
        APPROVED_STATIC_IMPEDANCE
    }

    public enum OpeningClass {
        FULL_FACE,
        MULTI_FACE,
        HALF_OPEN
    }

    public enum Orientation {
        HORIZONTAL,
        UPWARD,
        DOWNWARD
    }

    public enum WindBucket {
        CALM,
        WINDY,
        STRONG
    }

    public enum EnvironmentClass {
        OVERWORLD_OUTDOOR,
        NETHER_OUTDOOR,
        CUSTOM_NATURAL
    }

    public enum TopologyClass {
        OPEN_SPACE,
        HALF_OPEN_SPACE,
        CAVERN,
        TUNNEL_EXIT
    }

    public record Key(
            int cellLevel,
            OpeningClass openingClass,
            int openingAreaBucket,
            Orientation orientation,
            WindBucket windBucket,
            EnvironmentClass environmentClass,
            TopologyClass topologyClass
    ) {
        public Key {
            if (cellLevel < 0) {
                throw new IllegalArgumentException("cellLevel must be non-negative");
            }
            if (openingAreaBucket < 0) {
                throw new IllegalArgumentException("openingAreaBucket must be non-negative");
            }
            Objects.requireNonNull(openingClass, "openingClass");
            Objects.requireNonNull(orientation, "orientation");
            Objects.requireNonNull(windBucket, "windBucket");
            Objects.requireNonNull(environmentClass, "environmentClass");
            Objects.requireNonNull(topologyClass, "topologyClass");
        }
    }

    /** Calibration domain that must contain the current operating point. */
    public record ApplicabilityDomain(
            double maximumAbsoluteSourcePowerW,
            double maximumAbsoluteLocalNaturalDeltaC
    ) {
        public ApplicabilityDomain {
            requireNonNegativeFinite(
                    "maximumAbsoluteSourcePowerW", maximumAbsoluteSourcePowerW);
            requireNonNegativeFinite(
                    "maximumAbsoluteLocalNaturalDeltaC",
                    maximumAbsoluteLocalNaturalDeltaC);
        }

        public boolean contains(double absoluteSourcePowerW, double absoluteTemperatureDeltaC) {
            requireNonNegativeFinite("absoluteSourcePowerW", absoluteSourcePowerW);
            requireNonNegativeFinite("absoluteTemperatureDeltaC", absoluteTemperatureDeltaC);
            return absoluteSourcePowerW <= maximumAbsoluteSourcePowerW
                    && absoluteTemperatureDeltaC <= maximumAbsoluteLocalNaturalDeltaC;
        }
    }

    /** Conservative holdout envelope; boundary-energy limits preserve their sign. */
    public record ErrorEnvelope(
            double maximumTemperatureErrorC,
            double maximumThresholdCrossingErrorSeconds,
            boolean thresholdCrossingMismatchObserved,
            double minimumBoundaryEnergyErrorJ,
            double maximumBoundaryEnergyErrorJ,
            double maximumPhasePowerErrorW
    ) {
        public ErrorEnvelope {
            requireNonNegativeFinite("maximumTemperatureErrorC", maximumTemperatureErrorC);
            requireNonNegativeFinite(
                    "maximumThresholdCrossingErrorSeconds",
                    maximumThresholdCrossingErrorSeconds);
            requireFinite("minimumBoundaryEnergyErrorJ", minimumBoundaryEnergyErrorJ);
            requireFinite("maximumBoundaryEnergyErrorJ", maximumBoundaryEnergyErrorJ);
            requireNonNegativeFinite("maximumPhasePowerErrorW", maximumPhasePowerErrorW);
            if (minimumBoundaryEnergyErrorJ > maximumBoundaryEnergyErrorJ) {
                throw new IllegalArgumentException(
                        "boundary-energy error envelope must be ordered");
            }
        }

        public double maximumAbsoluteBoundaryEnergyErrorJ() {
            return Math.max(
                    Math.abs(minimumBoundaryEnergyErrorJ),
                    Math.abs(maximumBoundaryEnergyErrorJ));
        }
    }

    public record Profile(
            Key key,
            double conductanceWPerK,
            ApplicabilityDomain domain,
            ErrorEnvelope errorEnvelope,
            Approval approval
    ) {
        public Profile {
            Objects.requireNonNull(key, "key");
            requirePositiveFinite("conductanceWPerK", conductanceWPerK);
            Objects.requireNonNull(domain, "domain");
            Objects.requireNonNull(errorEnvelope, "errorEnvelope");
            Objects.requireNonNull(approval, "approval");
            if (approval == Approval.APPROVED_STATIC_IMPEDANCE
                    && errorEnvelope.thresholdCrossingMismatchObserved()) {
                throw new IllegalArgumentException(
                        "an approved profile cannot contain a threshold-crossing mismatch");
            }
        }

        public boolean isApprovedFor(
                double absoluteSourcePowerW,
                double absoluteTemperatureDeltaC
        ) {
            return approval == Approval.APPROVED_STATIC_IMPEDANCE
                    && domain.contains(absoluteSourcePowerW, absoluteTemperatureDeltaC);
        }
    }

    private final Map<Key, Profile> profiles;

    public FarFieldProfileRegistry(List<Profile> profiles) {
        Objects.requireNonNull(profiles, "profiles");
        Map<Key, Profile> indexed = new LinkedHashMap<>();
        for (Profile profile : profiles) {
            Objects.requireNonNull(profile, "profiles contains null");
            if (indexed.putIfAbsent(profile.key(), profile) != null) {
                throw new IllegalArgumentException(
                        "duplicate FarField profile key: " + profile.key());
            }
        }
        this.profiles = Map.copyOf(indexed);
    }

    public static FarFieldProfileRegistry empty() {
        return new FarFieldProfileRegistry(List.of());
    }

    public int profileCount() {
        return profiles.size();
    }

    public Optional<Profile> profile(Key key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(profiles.get(key));
    }

    public Optional<Profile> approved(
            Key key,
            double absoluteSourcePowerW,
            double absoluteTemperatureDeltaC
    ) {
        return profile(key).filter(profile -> profile.isApprovedFor(
                absoluteSourcePowerW, absoluteTemperatureDeltaC));
    }

    private static void requirePositiveFinite(String name, double value) {
        requireFinite(name, value);
        if (value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireNonNegativeFinite(String name, double value) {
        requireFinite(name, value);
        if (value < 0.0D) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
