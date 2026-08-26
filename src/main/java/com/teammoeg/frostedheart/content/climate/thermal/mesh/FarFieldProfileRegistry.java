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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable lookup of the V1 FarField impedance for each natural environment. */
public final class FarFieldProfileRegistry {
    public enum EnvironmentClass {
        OVERWORLD_OUTDOOR,
        NETHER_OUTDOOR,
        CUSTOM_NATURAL
    }

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

    public record Profile(
            EnvironmentClass environmentClass,
            double conductanceWPerK,
            ApplicabilityDomain domain
    ) {
        public Profile {
            Objects.requireNonNull(environmentClass, "environmentClass");
            requirePositiveFinite("conductanceWPerK", conductanceWPerK);
            Objects.requireNonNull(domain, "domain");
        }
    }

    private final Map<EnvironmentClass, Profile> profiles;

    public FarFieldProfileRegistry(List<Profile> profiles) {
        Objects.requireNonNull(profiles, "profiles");
        EnumMap<EnvironmentClass, Profile> indexed =
                new EnumMap<>(EnvironmentClass.class);
        for (Profile profile : profiles) {
            Objects.requireNonNull(profile, "profiles contains null");
            if (indexed.putIfAbsent(profile.environmentClass(), profile) != null) {
                throw new IllegalArgumentException(
                        "duplicate FarField environment: " + profile.environmentClass());
            }
        }
        this.profiles = Map.copyOf(indexed);
    }

    public static FarFieldProfileRegistry empty() {
        return new FarFieldProfileRegistry(List.of());
    }

    public Optional<Profile> profile(EnvironmentClass environmentClass) {
        return Optional.ofNullable(profiles.get(
                Objects.requireNonNull(environmentClass, "environmentClass")));
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
