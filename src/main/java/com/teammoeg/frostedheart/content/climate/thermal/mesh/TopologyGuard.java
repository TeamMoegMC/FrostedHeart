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

import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;

import java.util.Objects;
import java.util.Optional;

/**
 * Classifies one already-snapshotted frontier without reading or expanding the world.
 */
public final class TopologyGuard {
    private TopologyGuard() {
    }

    public enum FrontierClass {
        MATERIAL,
        OPEN_AMBIENT,
        OPEN_CONTINUATION,
        UNRESOLVED
    }

    public enum SurfaceKind {
        MATERIAL,
        OPEN
    }

    public enum Reason {
        MATERIAL_SURFACE,
        TOPOLOGY_UNRESOLVED,
        OUTDOOR_PROOF_MISSING,
        PROFILE_MISSING_OR_UNAPPROVED,
        PROFILE_OUTSIDE_CALIBRATION_DOMAIN,
        APPROVED_STATIC_IMPEDANCE
    }

    /** Loaded-only evidence prepared by the logical geometry owner. */
    public record Evidence(
            SurfaceKind surfaceKind,
            boolean immediateTopologyResolved,
            boolean loadedOnlyEvidence,
            boolean naturalEnvironment,
            boolean skyExposed,
            boolean coarseOpeningKnown,
            int localOpenDirectionCount,
            FarFieldProfileRegistry.Key profileKey
    ) {
        public Evidence {
            Objects.requireNonNull(surfaceKind, "surfaceKind");
            if (localOpenDirectionCount < 0 || localOpenDirectionCount > 6) {
                throw new IllegalArgumentException(
                        "localOpenDirectionCount must be in [0, 6]");
            }
            if (surfaceKind == SurfaceKind.OPEN && profileKey == null) {
                throw new IllegalArgumentException("open evidence requires a profile key");
            }
            if (surfaceKind == SurfaceKind.MATERIAL && profileKey != null) {
                throw new IllegalArgumentException("material evidence cannot name a FarField key");
            }
        }

        public static Evidence material() {
            return new Evidence(
                    SurfaceKind.MATERIAL, true, true, false,
                    false, false, 0, null);
        }

        public static Evidence open(
                boolean immediateTopologyResolved,
                boolean loadedOnlyEvidence,
                boolean naturalEnvironment,
                boolean skyExposed,
                boolean coarseOpeningKnown,
                int localOpenDirectionCount,
                FarFieldProfileRegistry.Key profileKey
        ) {
            return new Evidence(
                    SurfaceKind.OPEN,
                    immediateTopologyResolved,
                    loadedOnlyEvidence,
                    naturalEnvironment,
                    skyExposed,
                    coarseOpeningKnown,
                    localOpenDirectionCount,
                    profileKey);
        }

        private boolean hasCheapOutdoorProof() {
            return loadedOnlyEvidence
                    && naturalEnvironment
                    && (skyExposed
                    || (coarseOpeningKnown && localOpenDirectionCount >= 2));
        }
    }

    public record OperatingPoint(
            double absoluteSourcePowerW,
            double absoluteLocalNaturalDeltaC
    ) {
        public OperatingPoint {
            requireNonNegativeFinite("absoluteSourcePowerW", absoluteSourcePowerW);
            requireNonNegativeFinite(
                    "absoluteLocalNaturalDeltaC", absoluteLocalNaturalDeltaC);
        }
    }

    public record Decision(
            FrontierClass frontierClass,
            Reason reason,
            Optional<FarFieldProfileRegistry.Profile> farFieldProfile
    ) {
        public Decision {
            Objects.requireNonNull(frontierClass, "frontierClass");
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(farFieldProfile, "farFieldProfile");
            if ((frontierClass == FrontierClass.OPEN_AMBIENT) != farFieldProfile.isPresent()) {
                throw new IllegalArgumentException(
                        "only OPEN_AMBIENT decisions may carry a FarField profile");
            }
        }

        public boolean requestsMoreGeometry() {
            return frontierClass == FrontierClass.OPEN_CONTINUATION
                    || frontierClass == FrontierClass.UNRESOLVED;
        }

        /** Returns no operator for material, continuation, or unresolved frontiers. */
        public Optional<ThermalSweep.BoundaryOperation> boundaryOperation(
                int cell,
                double naturalTemperatureC
        ) {
            if (cell < 0) {
                throw new IllegalArgumentException("cell must be non-negative");
            }
            requireFinite("naturalTemperatureC", naturalTemperatureC);
            return farFieldProfile.map(profile -> new ThermalSweep.BoundaryOperation(
                    cell, naturalTemperatureC, profile.conductanceWPerK()));
        }
    }

    public static Decision classify(
            Evidence evidence,
            OperatingPoint operatingPoint,
            FarFieldProfileRegistry registry
    ) {
        Objects.requireNonNull(evidence, "evidence");
        Objects.requireNonNull(operatingPoint, "operatingPoint");
        Objects.requireNonNull(registry, "registry");

        if (evidence.surfaceKind() == SurfaceKind.MATERIAL) {
            return decision(FrontierClass.MATERIAL, Reason.MATERIAL_SURFACE);
        }
        if (!evidence.immediateTopologyResolved()) {
            return decision(FrontierClass.UNRESOLVED, Reason.TOPOLOGY_UNRESOLVED);
        }
        if (!evidence.hasCheapOutdoorProof()) {
            return decision(
                    FrontierClass.OPEN_CONTINUATION, Reason.OUTDOOR_PROOF_MISSING);
        }

        Optional<FarFieldProfileRegistry.Profile> profile = registry.profile(evidence.profileKey());
        if (profile.isEmpty()
                || profile.get().approval()
                != FarFieldProfileRegistry.Approval.APPROVED_STATIC_IMPEDANCE) {
            return decision(
                    FrontierClass.OPEN_CONTINUATION,
                    Reason.PROFILE_MISSING_OR_UNAPPROVED);
        }
        if (!profile.get().domain().contains(
                operatingPoint.absoluteSourcePowerW(),
                operatingPoint.absoluteLocalNaturalDeltaC())) {
            return decision(
                    FrontierClass.OPEN_CONTINUATION,
                    Reason.PROFILE_OUTSIDE_CALIBRATION_DOMAIN);
        }
        return new Decision(
                FrontierClass.OPEN_AMBIENT,
                Reason.APPROVED_STATIC_IMPEDANCE,
                profile);
    }

    private static Decision decision(FrontierClass frontierClass, Reason reason) {
        return new Decision(frontierClass, reason, Optional.empty());
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
