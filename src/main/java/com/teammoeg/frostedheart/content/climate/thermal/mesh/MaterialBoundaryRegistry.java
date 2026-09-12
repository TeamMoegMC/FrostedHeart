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
    public enum Model {
        CAPACITIVE_SURFACE,
        PHASE_RESERVOIR
    }

    /** Material coefficients; capacity is scaled by exposed whole-block faces. */
    public record Profile(
            int id,
            Model model,
            double faceConductanceWPerK,
            double surfaceCapacityJPerK,
            double transitionTemperatureC,
            double transitionEnergyJPerUnit
    ) {
        public Profile {
            requirePositiveId("material profile", id);
            Objects.requireNonNull(model, "model");
            requirePositiveFinite("faceConductanceWPerK", faceConductanceWPerK);
            requireNonNegativeFinite("surfaceCapacityJPerK", surfaceCapacityJPerK);
            requireFinite("transitionTemperatureC", transitionTemperatureC);
            requireNonNegativeFinite(
                    "transitionEnergyJPerUnit", transitionEnergyJPerUnit);
            if (model == Model.CAPACITIVE_SURFACE) {
                requirePositiveFinite("surfaceCapacityJPerK", surfaceCapacityJPerK);
                requireZero("transitionEnergyJPerUnit", transitionEnergyJPerUnit);
            } else {
                requireZero("surfaceCapacityJPerK", surfaceCapacityJPerK);
                requirePositiveFinite(
                        "transitionEnergyJPerUnit", transitionEnergyJPerUnit);
            }
        }

        /** Creates a gameplay surface initialized from its Page's natural air. */
        public static Profile capacitiveSurfaceAtNaturalTemperature(
                int id,
                double faceConductanceWPerK,
                double surfaceCapacityJPerK
        ) {
            return new Profile(
                    id, Model.CAPACITIVE_SURFACE, faceConductanceWPerK,
                    surfaceCapacityJPerK, 0.0D, 0.0D);
        }

        public static Profile phaseReservoir(
                int id,
                double faceConductanceWPerK,
                double transitionTemperatureC,
                double transitionEnergyJPerUnit
        ) {
            return new Profile(
                    id, Model.PHASE_RESERVOIR, faceConductanceWPerK,
                    0.0D, transitionTemperatureC, transitionEnergyJPerUnit);
        }

        public double poleInitialTemperatureC(double pageNaturalTemperatureC) {
            requireFinite("pageNaturalTemperatureC", pageNaturalTemperatureC);
            if (model != Model.CAPACITIVE_SURFACE) {
                throw new IllegalStateException(
                        "phase reservoirs do not own material-pole temperature");
            }
            return pageNaturalTemperatureC;
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

    private static void requireZero(String name, double value) {
        if (value != 0.0D) {
            throw new IllegalArgumentException(name + " must be zero for this material model");
        }
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
