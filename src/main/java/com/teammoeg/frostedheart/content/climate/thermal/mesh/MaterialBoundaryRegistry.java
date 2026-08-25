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

/**
 * Immutable worker-safe material parameters and contact masks. ID zero is the
 * reserved "no material boundary" value carried by geometry-only signatures.
 */
public final class MaterialBoundaryRegistry {
    public enum Model {
        STATELESS_CONDUCTANCE,
        CAPACITIVE_SURFACE,
        NATURAL_ROCK,
        PHASE_RESERVOIR
    }

    public enum TransitionMutationPolicy {
        NONE,
        RESPECT_RANDOM_TICK_SPEED,
        IGNORE_RANDOM_TICK_SPEED,
        SCRIPT_CONTROLLED
    }

    public enum TransitionAction {
        NONE,
        REMOVE_ONE_SNOW_LAYER,
        MELT_ICE_TO_WATER,
        APPLY_STATE_TRANSITION_RECIPE,
        CUSTOM
    }

    /** One bit per block-local 4x4x4 microcell occupied by this material. */
    public record ContactPattern(int id, long materialMicrocellMask) {
        public ContactPattern {
            requirePositiveId("contact pattern", id);
            if (materialMicrocellMask == 0L) {
                throw new IllegalArgumentException("material contact mask must not be empty");
            }
        }

        public static ContactPattern fullBlock(int id) {
            return new ContactPattern(id, -1L);
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
            double deepConductanceWPerK,
            double deepCapacityJPerK,
            double naturalConductanceWPerK,
            double initialTemperatureC,
            double naturalTemperatureAtY0C,
            double geothermalGradientCPerBlock,
            boolean initializeAtNaturalTemperature,
            double transitionTemperatureC,
            double transitionEnergyJPerUnit,
            TransitionMutationPolicy transitionMutationPolicy,
            TransitionAction transitionAction
    ) {
        public Profile {
            requirePositiveId("material profile", id);
            Objects.requireNonNull(model, "model");
            requirePositiveFinite("faceConductanceWPerK", faceConductanceWPerK);
            requireNonNegativeFinite("surfaceCapacityJPerK", surfaceCapacityJPerK);
            requireNonNegativeFinite("deepConductanceWPerK", deepConductanceWPerK);
            requireNonNegativeFinite("deepCapacityJPerK", deepCapacityJPerK);
            requireNonNegativeFinite("naturalConductanceWPerK", naturalConductanceWPerK);
            requireFinite("initialTemperatureC", initialTemperatureC);
            requireFinite("naturalTemperatureAtY0C", naturalTemperatureAtY0C);
            requireNonNegativeFinite(
                    "geothermalGradientCPerBlock", geothermalGradientCPerBlock);
            requireFinite("transitionTemperatureC", transitionTemperatureC);
            requireNonNegativeFinite(
                    "transitionEnergyJPerUnit", transitionEnergyJPerUnit);
            Objects.requireNonNull(
                    transitionMutationPolicy, "transitionMutationPolicy");
            Objects.requireNonNull(transitionAction, "transitionAction");

            switch (model) {
                case STATELESS_CONDUCTANCE -> {
                    requireZero("surfaceCapacityJPerK", surfaceCapacityJPerK);
                    requireZero("deepConductanceWPerK", deepConductanceWPerK);
                    requireZero("deepCapacityJPerK", deepCapacityJPerK);
                    requireZero("naturalConductanceWPerK", naturalConductanceWPerK);
                    requireZero("geothermalGradientCPerBlock", geothermalGradientCPerBlock);
                    requireExplicitInitialTemperature(initializeAtNaturalTemperature);
                    requireNoTransition(
                            transitionEnergyJPerUnit,
                            transitionMutationPolicy,
                            transitionAction);
                }
                case CAPACITIVE_SURFACE -> {
                    requirePositiveFinite("surfaceCapacityJPerK", surfaceCapacityJPerK);
                    requireZero("deepConductanceWPerK", deepConductanceWPerK);
                    requireZero("deepCapacityJPerK", deepCapacityJPerK);
                    requireZero("naturalConductanceWPerK", naturalConductanceWPerK);
                    requireZero("geothermalGradientCPerBlock", geothermalGradientCPerBlock);
                    requireNoTransition(
                            transitionEnergyJPerUnit,
                            transitionMutationPolicy,
                            transitionAction);
                }
                case NATURAL_ROCK -> {
                    requireExplicitInitialTemperature(initializeAtNaturalTemperature);
                    requirePositiveFinite("surfaceCapacityJPerK", surfaceCapacityJPerK);
                    requirePositiveFinite("deepConductanceWPerK", deepConductanceWPerK);
                    if (deepCapacityJPerK > 0.0D) {
                        requirePositiveFinite(
                                "naturalConductanceWPerK", naturalConductanceWPerK);
                    } else {
                        requireZero("naturalConductanceWPerK", naturalConductanceWPerK);
                    }
                    requireNoTransition(
                            transitionEnergyJPerUnit,
                            transitionMutationPolicy,
                            transitionAction);
                }
                case PHASE_RESERVOIR -> {
                    requireZero("surfaceCapacityJPerK", surfaceCapacityJPerK);
                    requireZero("deepConductanceWPerK", deepConductanceWPerK);
                    requireZero("deepCapacityJPerK", deepCapacityJPerK);
                    requireZero("naturalConductanceWPerK", naturalConductanceWPerK);
                    requireZero("geothermalGradientCPerBlock", geothermalGradientCPerBlock);
                    requireExplicitInitialTemperature(initializeAtNaturalTemperature);
                    requirePositiveFinite(
                            "transitionEnergyJPerUnit", transitionEnergyJPerUnit);
                    if (transitionMutationPolicy == TransitionMutationPolicy.NONE
                            || transitionAction == TransitionAction.NONE) {
                        throw new IllegalArgumentException(
                                "phase reservoir requires mutation policy and action");
                    }
                }
            }
        }

        public static Profile stateless(int id, double wallConductanceWPerK) {
            return new Profile(
                    id, Model.STATELESS_CONDUCTANCE, wallConductanceWPerK,
                    0.0D, 0.0D, 0.0D, 0.0D,
                    0.0D, 0.0D, 0.0D,
                    false, 0.0D, 0.0D,
                    TransitionMutationPolicy.NONE, TransitionAction.NONE);
        }

        public static Profile capacitiveSurface(
                int id,
                double faceConductanceWPerK,
                double surfaceCapacityJPerK,
                double initialTemperatureC
        ) {
            return new Profile(
                    id, Model.CAPACITIVE_SURFACE, faceConductanceWPerK,
                    surfaceCapacityJPerK, 0.0D, 0.0D, 0.0D,
                    initialTemperatureC, 0.0D, 0.0D,
                    false, 0.0D, 0.0D,
                    TransitionMutationPolicy.NONE, TransitionAction.NONE);
        }

        /** Creates a gameplay surface initialized from its Page's natural air. */
        public static Profile capacitiveSurfaceAtNaturalTemperature(
                int id,
                double faceConductanceWPerK,
                double surfaceCapacityJPerK
        ) {
            return new Profile(
                    id, Model.CAPACITIVE_SURFACE, faceConductanceWPerK,
                    surfaceCapacityJPerK, 0.0D, 0.0D, 0.0D,
                    0.0D, 0.0D, 0.0D,
                    true, 0.0D, 0.0D,
                    TransitionMutationPolicy.NONE, TransitionAction.NONE);
        }

        public static Profile naturalRock(
                int id,
                double faceConductanceWPerK,
                double surfaceCapacityJPerK,
                double deepConductanceWPerK,
                double deepCapacityJPerK,
                double naturalConductanceWPerK,
                double naturalTemperatureAtY0C,
                double geothermalGradientCPerBlock
        ) {
            return new Profile(
                    id, Model.NATURAL_ROCK, faceConductanceWPerK,
                    surfaceCapacityJPerK, deepConductanceWPerK,
                    deepCapacityJPerK, naturalConductanceWPerK,
                    naturalTemperatureAtY0C, naturalTemperatureAtY0C,
                    geothermalGradientCPerBlock,
                    false, 0.0D, 0.0D,
                    TransitionMutationPolicy.NONE, TransitionAction.NONE);
        }

        public static Profile phaseReservoir(
                int id,
                double faceConductanceWPerK,
                double transitionTemperatureC,
                double transitionEnergyJPerUnit,
                TransitionMutationPolicy mutationPolicy,
                TransitionAction transitionAction
        ) {
            return new Profile(
                    id, Model.PHASE_RESERVOIR, faceConductanceWPerK,
                    0.0D, 0.0D, 0.0D, 0.0D,
                    transitionTemperatureC, 0.0D, 0.0D,
                    false, transitionTemperatureC, transitionEnergyJPerUnit,
                    mutationPolicy, transitionAction);
        }

        public double naturalTemperatureC(int blockY) {
            double temperature = naturalTemperatureAtY0C
                    - geothermalGradientCPerBlock * blockY;
            requireFinite("natural material temperature", temperature);
            return temperature;
        }

        public double poleInitialTemperatureC(
                int blockY,
                double pageNaturalTemperatureC
        ) {
            requireFinite("pageNaturalTemperatureC", pageNaturalTemperatureC);
            return model == Model.NATURAL_ROCK
                    ? naturalTemperatureC(blockY)
                    : initializeAtNaturalTemperature
                            ? pageNaturalTemperatureC
                            : initialTemperatureC;
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

    public static MaterialBoundaryRegistry empty() {
        return new MaterialBoundaryRegistry(List.of(), List.of());
    }

    public Optional<Profile> profile(int id) {
        return Optional.ofNullable(profiles.get(id));
    }

    public Optional<ContactPattern> contactPattern(int id) {
        return Optional.ofNullable(contactPatterns.get(id));
    }

    public int profileCount() {
        return profiles.size();
    }

    public int contactPatternCount() {
        return contactPatterns.size();
    }

    public int phaseProfileCount() {
        int count = 0;
        for (Profile profile : profiles.values()) {
            if (profile.model() == Model.PHASE_RESERVOIR) {
                count++;
            }
        }
        return count;
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

    private static void requireExplicitInitialTemperature(boolean initializeAtNatural) {
        if (initializeAtNatural) {
            throw new IllegalArgumentException(
                    "natural Page initialization is only valid for capacitive surfaces");
        }
    }

    private static void requireNoTransition(
            double transitionEnergyJPerUnit,
            TransitionMutationPolicy mutationPolicy,
            TransitionAction transitionAction
    ) {
        requireZero("transitionEnergyJPerUnit", transitionEnergyJPerUnit);
        if (mutationPolicy != TransitionMutationPolicy.NONE
                || transitionAction != TransitionAction.NONE) {
            throw new IllegalArgumentException(
                    "non-phase material profiles cannot declare a transition");
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
