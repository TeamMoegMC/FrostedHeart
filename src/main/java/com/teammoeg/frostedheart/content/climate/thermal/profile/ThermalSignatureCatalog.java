/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import java.util.LinkedHashMap;
import java.util.Map;

/** Engine-generation-local primitive lookup for immutable signature topology. */
public final class ThermalSignatureCatalog {
    public static final int UNRESOLVED = -1;
    private static final int MICROCELL_COUNT = 64;

    private final ResolvedThermalSignature[] signatures;
    private final ConservativeAirGeometry.Resolution[] geometries;
    private final int[] topologyClass;
    private final long[] airMask;
    private final ThermalSignatureRegistry registry;

    public ThermalSignatureCatalog(ThermalSignatureRegistry registry) {
        this.registry = registry;
        int count = registry.signatureCount();
        signatures = new ResolvedThermalSignature[count];
        geometries = new ConservativeAirGeometry.Resolution[count];
        topologyClass = new int[count];
        airMask = new long[count];
        Map<TopologyIdentity, Integer> classes = new LinkedHashMap<>();
        for (int id = 0; id < count; id++) {
            ResolvedThermalSignature signature =
                    registry.signatureOrNull(id);
            if (signature == null) {
                throw new IllegalStateException(
                        "signature registry lost a dense ID");
            }
            ConservativeAirGeometry.Resolution geometry =
                    signature.airGeometry();
            signatures[id] = signature;
            geometries[id] = geometry;
            topologyClass[id] = classes.computeIfAbsent(
                    new TopologyIdentity(
                            signature.materialProfileId(),
                            signature.materialContactPatternId(),
                            geometry),
                    ignored -> classes.size());
            long mask = geometry.provenAirMicrocellMask();
            airMask[id] = mask;
        }
    }

    public boolean valid(int signatureId) {
        return signatureId >= 0 && signatureId < signatures.length;
    }

    public ResolvedThermalSignature signature(int signatureId) {
        return valid(signatureId) ? signatures[signatureId] : null;
    }

    public ConservativeAirGeometry.Resolution geometry(int signatureId) {
        return valid(signatureId) ? geometries[signatureId] : null;
    }

    public long airMask(int signatureId) {
        return valid(signatureId) ? airMask[signatureId] : 0L;
    }

    public int componentOrdinal(int signatureId, int microcell) {
        if (!valid(signatureId) || microcell < 0 || microcell >= MICROCELL_COUNT) {
            return 0xff;
        }
        return registry.componentOrdinal(signatureId, microcell);
    }

    public boolean topologyEquivalent(int first, int second) {
        return first == second
                || valid(first) && valid(second)
                && topologyClass[first] == topologyClass[second];
    }

    private record TopologyIdentity(
            int materialProfileId,
            int materialContactPatternId,
            ConservativeAirGeometry.Resolution geometry
    ) {
    }
}
