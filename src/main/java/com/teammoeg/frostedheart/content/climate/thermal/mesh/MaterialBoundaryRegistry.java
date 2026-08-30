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

/**
 * Immutable solve-safe material parameters and contact masks. ID zero is the
 * reserved "no material boundary" value carried by geometry-only signatures.
 */
public final class MaterialBoundaryRegistry {
    public enum Model {
        CAPACITIVE_SURFACE,
        PHASE_RESERVOIR
    }

    /** One bit per block-local 4x4x4 microcell occupied by this material. */
    public record ContactPattern(int id, long materialMicrocellMask) {
        public ContactPattern {
            requirePositiveId("contact pattern", id);
            if (materialMicrocellMask == 0L) {
                throw new IllegalArgumentException("material contact mask must not be empty");
            }
        }

        public boolean contains(int x, int y, int z) {
            if ((x | y | z) < 0 || x >= 4 || y >= 4 || z >= 4) {
                return false;
            }
            int bit = (y << 4) | (z << 2) | x;
            return (materialMicrocellMask & (1L << bit)) != 0L;
        }
    }

    /**
     * Conductances and capacities are specified per full exposed block face.
     * Partial 4x4 contacts scale them by represented area; all exposed faces
     * of one block share the same capacitive pole.
     */
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

    private final Map<Integer, Profile> profiles;
    private final Map<Integer, ContactPattern> contactPatterns;

    public MaterialBoundaryRegistry(
            List<Profile> profiles,
            List<ContactPattern> contactPatterns
    ) {
        Objects.requireNonNull(profiles, "profiles");
        Objects.requireNonNull(contactPatterns, "contactPatterns");
        this.profiles = indexProfiles(profiles);
        this.contactPatterns = indexPatterns(contactPatterns);
    }

    public Profile profileOrNull(int id) {
        return profiles.get(id);
    }

    public ContactPattern contactPatternOrNull(int id) {
        return contactPatterns.get(id);
    }

    private static Map<Integer, Profile> indexProfiles(List<Profile> values) {
        Map<Integer, Profile> indexed = new LinkedHashMap<>();
        for (Profile profile : values) {
            Objects.requireNonNull(profile, "profiles contains null");
            if (indexed.putIfAbsent(profile.id(), profile) != null) {
                throw new IllegalArgumentException(
                        "duplicate material profile ID: " + profile.id());
            }
        }
        return Map.copyOf(indexed);
    }

    private static Map<Integer, ContactPattern> indexPatterns(
            List<ContactPattern> values
    ) {
        Map<Integer, ContactPattern> indexed = new LinkedHashMap<>();
        for (ContactPattern pattern : values) {
            Objects.requireNonNull(pattern, "contactPatterns contains null");
            if (indexed.putIfAbsent(pattern.id(), pattern) != null) {
                throw new IllegalArgumentException(
                        "duplicate material contact pattern ID: " + pattern.id());
            }
        }
        return Map.copyOf(indexed);
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
