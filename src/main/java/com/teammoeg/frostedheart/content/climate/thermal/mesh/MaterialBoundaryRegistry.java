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

import java.util.List;
import java.util.Objects;

/**
 * Immutable solve-safe material parameters and contact masks. Entries use
 * dense IDs in list order, starting at one; zero is the reserved "no material
 * boundary" value carried by geometry-only signatures.
 */
public final class MaterialBoundaryRegistry {
    /** One material body law; face area affects conductance, never capacity. */
    public record Profile(int id, double faceConductanceWPerK, MaterialThermalLaw thermalLaw) {
        public Profile {
            requirePositiveId("material profile", id);
            requirePositiveFinite("faceConductanceWPerK", faceConductanceWPerK);
            Objects.requireNonNull(thermalLaw, "thermalLaw");
        }

        public static Profile body(int id, double faceConductanceWPerK, MaterialThermalLaw law) {
            return new Profile(id, faceConductanceWPerK, law);
        }
    }

    private final Profile[] profiles;

    public MaterialBoundaryRegistry(
            List<Profile> profiles
    ) {
        Objects.requireNonNull(profiles, "profiles");
        this.profiles = indexProfiles(profiles);
    }

    public Profile profileOrNull(int id) {
        return id > 0 && id < profiles.length ? profiles[id] : null;
    }

    private static Profile[] indexProfiles(List<Profile> values) {
        Profile[] indexed = new Profile[values.size() + 1];
        int expectedId = 1;
        for (Profile profile : values) {
            Objects.requireNonNull(profile, "profiles contains null");
            if (profile.id() != expectedId) {
                throw new IllegalArgumentException(
                        "material profile ID must be dense and ordered: expected "
                                + expectedId + ", got " + profile.id());
            }
            indexed[expectedId++] = profile;
        }
        return indexed;
    }

    private static void requirePositiveId(String name, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " ID must be positive; zero means none");
        }
    }

    private static void requirePositiveFinite(String name, double value) {
        requireFinite(name, value);
        if (value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
