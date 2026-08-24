/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.profile;

import java.util.Objects;

/**
 * Extension point for explicit profile overrides and bounded contextual
 * resolvers. Minecraft integration invokes {@link #resolveSnapshot} only on
 * the main thread; workers receive only the resulting primitive signature ID.
 */
public interface ThermalSignatureResolver<B, F> {
    /** Stable, deterministic registration ID. */
    String resolverId();

    DependencyOffsetMask dependencyMask();

    int maxOutputRegions();

    ThermalResolution<ResolvedThermalSignature> resolve(ResolverBlockView.Access<B, F> view);

    /** Runs one audited resolution and normalizes ignored sentinel accesses. */
    default ThermalResolution<ResolvedThermalSignature> resolveSnapshot(
            ResolverBlockView<B, F> snapshot
    ) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (!Objects.equals(snapshot.dependencyMask(), dependencyMask())) {
            return ThermalResolution.unsupported(ThermalResolution.Reason.INVALID_RESOLVER_OUTPUT);
        }
        ResolverBlockView.Access<B, F> access = snapshot.openAccess();
        ThermalResolution<ResolvedThermalSignature> result = resolve(access);
        if (result == null) {
            return ThermalResolution.unsupported(ThermalResolution.Reason.INVALID_RESOLVER_OUTPUT);
        }
        ThermalResolution<ResolvedThermalSignature> normalized = access.normalize(result);
        if (normalized.isResolved()
                && normalized.value().orElseThrow().localAirRegionCount() > maxOutputRegions()) {
            return ThermalResolution.unsupported(ThermalResolution.Reason.REGION_LIMIT_EXCEEDED);
        }
        return normalized;
    }
}
