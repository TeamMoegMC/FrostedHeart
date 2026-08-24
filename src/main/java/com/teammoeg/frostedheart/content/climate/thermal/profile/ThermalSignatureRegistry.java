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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Immutable, dense int-ID table of deduplicated resolved thermal signatures. */
public final class ThermalSignatureRegistry {
    private final List<ResolvedThermalSignature> signatures;
    private final Map<ResolvedThermalSignature, Integer> idsBySignature;

    private ThermalSignatureRegistry(List<ResolvedThermalSignature> signatures) {
        this.signatures = List.copyOf(signatures);
        Map<ResolvedThermalSignature, Integer> ids = new LinkedHashMap<>();
        for (int id = 0; id < signatures.size(); id++) {
            Integer previous = ids.put(signatures.get(id), id);
            if (previous != null) {
                throw new IllegalArgumentException("frozen signature table contains a duplicate");
            }
        }
        this.idsBySignature = Map.copyOf(ids);
    }

    public static Builder builder() {
        return new Builder();
    }

    public int signatureCount() {
        return signatures.size();
    }

    public Optional<ResolvedThermalSignature> signature(int signatureId) {
        if (signatureId < 0 || signatureId >= signatures.size()) {
            return Optional.empty();
        }
        return Optional.of(signatures.get(signatureId));
    }

    public OptionalInt idOf(ResolvedThermalSignature signature) {
        Integer id = idsBySignature.get(Objects.requireNonNull(signature, "signature"));
        return id == null ? OptionalInt.empty() : OptionalInt.of(id);
    }

    /**
     * Main-thread builder. IDs are deterministic for a deterministic first-seen
     * registration order and remain primitive ints throughout correctness code.
     */
    public static final class Builder {
        private final List<ResolvedThermalSignature> signatures = new ArrayList<>();
        private final Map<ResolvedThermalSignature, Integer> idsBySignature = new LinkedHashMap<>();

        public int intern(ResolvedThermalSignature signature) {
            Objects.requireNonNull(signature, "signature");
            Integer existing = idsBySignature.get(signature);
            if (existing != null) {
                return existing;
            }
            int id = signatures.size();
            signatures.add(signature);
            idsBySignature.put(signature, id);
            return id;
        }

        public ThermalSignatureResolution internResolution(
                ThermalResolution<ResolvedThermalSignature> resolution
        ) {
            Objects.requireNonNull(resolution, "resolution");
            if (!resolution.isResolved()) {
                return ThermalSignatureResolution.failure(resolution);
            }
            return ThermalSignatureResolution.resolved(intern(resolution.value().orElseThrow()));
        }

        public int signatureCount() {
            return signatures.size();
        }

        public ThermalSignatureRegistry build() {
            return new ThermalSignatureRegistry(signatures);
        }
    }
}
