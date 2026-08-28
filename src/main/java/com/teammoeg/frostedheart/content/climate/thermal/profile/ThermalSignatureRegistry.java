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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable, dense int-ID table of deduplicated resolved thermal signatures. */
public final class ThermalSignatureRegistry {
    private final List<ResolvedThermalSignature> signatures;
    private final Map<ResolvedThermalSignature, Integer> idsBySignature;
    private final byte[] componentOrdinal;

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
        componentOrdinal = new byte[signatures.size() * 64];
        Arrays.fill(componentOrdinal, (byte) 0xff);
        for (int signatureId = 0;
             signatureId < signatures.size();
             signatureId++) {
            for (LocalAirRegionPattern region
                    : signatures.get(signatureId).airRegions()) {
                if (region.localRegionId() < 0
                        || region.localRegionId() >= 0xff) {
                    throw new IllegalArgumentException(
                            "signature Air region does not fit one byte");
                }
                long remaining = region.provenAirMicrocellMask();
                while (remaining != 0L) {
                    int microcell = Long.numberOfTrailingZeros(remaining);
                    componentOrdinal[signatureId * 64 + microcell] =
                            (byte) region.localRegionId();
                    remaining &= remaining - 1L;
                }
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public int signatureCount() {
        return signatures.size();
    }

    public ResolvedThermalSignature signatureOrNull(int signatureId) {
        return signatureId < 0 || signatureId >= signatures.size()
                ? null : signatures.get(signatureId);
    }

    public int idOrDefault(
            ResolvedThermalSignature signature,
            int fallback
    ) {
        Integer id = idsBySignature.get(Objects.requireNonNull(signature, "signature"));
        return id == null ? fallback : id;
    }

    public int componentOrdinal(int signatureId, int microcell) {
        if (signatureId < 0 || signatureId >= signatures.size()
                || microcell < 0 || microcell >= 64) {
            return 0xff;
        }
        return Byte.toUnsignedInt(
                componentOrdinal[signatureId * 64 + microcell]);
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

        public ThermalSignatureRegistry build() {
            return new ThermalSignatureRegistry(signatures);
        }
    }
}
