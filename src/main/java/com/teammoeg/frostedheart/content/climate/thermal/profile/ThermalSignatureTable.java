/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Server-wide immutable primitive lookup for thermal signatures and geometry. */
public final class ThermalSignatureTable {
    public static final int UNRESOLVED = -1;
    private static final int NO_COMPONENT = -1;
    private static final int SINGLE_COMPONENT = -2;
    private static final int MICROCELL_COUNT = 64;
    private static final Integer UNRESOLVED_PAYLOAD = Integer.valueOf(UNRESOLVED);

    private final int[] geometryIdBySignature;
    private final int[] materialProfileIdBySignature;
    private final Integer[] uniformPayloadBySignature;
    private final ConservativeAirGeometry.Resolution[] geometries;
    private final long[] airMaskByGeometry;
    private final int[] contactPatternIdByGeometry;
    private final int[] componentOffsetByGeometry;
    private final byte[] mixedComponentOrdinals;

    private ThermalSignatureTable(Builder builder) {
        geometryIdBySignature = builder.signatureGeometry.stream()
                .mapToInt(Integer::intValue).toArray();
        materialProfileIdBySignature = builder.signatureMaterial.stream()
                .mapToInt(Integer::intValue).toArray();
        uniformPayloadBySignature = new Integer[geometryIdBySignature.length];
        for (int id = 0; id < uniformPayloadBySignature.length; id++) {
            uniformPayloadBySignature[id] = Integer.valueOf(id);
        }

        geometries = builder.geometries.toArray(
                ConservativeAirGeometry.Resolution[]::new);
        airMaskByGeometry = new long[geometries.length];
        contactPatternIdByGeometry = builder.geometryContactPattern.stream()
                .mapToInt(Integer::intValue).toArray();
        componentOffsetByGeometry = new int[geometries.length];

        int mixedGeometryCount = 0;
        for (ConservativeAirGeometry.Resolution geometry : geometries) {
            if (geometry.components().size() > 1) {
                mixedGeometryCount++;
            }
        }
        mixedComponentOrdinals = new byte[Math.multiplyExact(
                mixedGeometryCount, MICROCELL_COUNT)];
        Arrays.fill(mixedComponentOrdinals, (byte) 0xff);

        int mixedOffset = 0;
        for (int geometryId = 0; geometryId < geometries.length; geometryId++) {
            ConservativeAirGeometry.Resolution geometry = geometries[geometryId];
            airMaskByGeometry[geometryId] = geometry.provenAirMicrocellMask();
            int componentCount = geometry.components().size();
            if (componentCount == 0) {
                componentOffsetByGeometry[geometryId] = NO_COMPONENT;
                continue;
            }
            if (componentCount == 1) {
                componentOffsetByGeometry[geometryId] = SINGLE_COMPONENT;
                continue;
            }
            componentOffsetByGeometry[geometryId] = mixedOffset;
            for (var component : geometry.components()) {
                if (component.id() < 0 || component.id() >= 0xff) {
                    throw new IllegalArgumentException(
                            "signature Air region does not fit one byte");
                }
                long remaining = component.microcellMask();
                while (remaining != 0L) {
                    int microcell = Long.numberOfTrailingZeros(remaining);
                    mixedComponentOrdinals[mixedOffset + microcell] =
                            (byte) component.id();
                    remaining &= remaining - 1L;
                }
            }
            mixedOffset += MICROCELL_COUNT;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean valid(int signatureId) {
        return signatureId >= 0 && signatureId < geometryIdBySignature.length;
    }

    public ConservativeAirGeometry.Resolution geometry(int signatureId) {
        return valid(signatureId)
                ? geometries[geometryIdBySignature[signatureId]] : null;
    }

    public long airMask(int signatureId) {
        return valid(signatureId)
                ? airMaskByGeometry[geometryIdBySignature[signatureId]] : 0L;
    }

    public int materialProfileId(int signatureId) {
        return valid(signatureId)
                ? materialProfileIdBySignature[signatureId] : 0;
    }

    public int materialContactPatternId(int signatureId) {
        if (!valid(signatureId)
                || materialProfileIdBySignature[signatureId] == 0) {
            return 0;
        }
        return contactPatternIdByGeometry[geometryIdBySignature[signatureId]];
    }

    public int componentOrdinal(int signatureId, int microcell) {
        if (!valid(signatureId)
                || microcell < 0 || microcell >= MICROCELL_COUNT) {
            return 0xff;
        }
        int geometryId = geometryIdBySignature[signatureId];
        int offset = componentOffsetByGeometry[geometryId];
        if (offset == NO_COMPONENT) {
            return 0xff;
        }
        if (offset == SINGLE_COMPONENT) {
            return (airMaskByGeometry[geometryId] & 1L << microcell) != 0L
                    ? 0 : 0xff;
        }
        return Byte.toUnsignedInt(mixedComponentOrdinals[offset + microcell]);
    }

    public Integer uniformPayload(int signatureId) {
        if (signatureId == UNRESOLVED) {
            return UNRESOLVED_PAYLOAD;
        }
        if (!valid(signatureId)) {
            throw new IllegalArgumentException(
                    "uniform payload requires a valid signature ID");
        }
        return uniformPayloadBySignature[signatureId];
    }

    /** Startup-only deterministic interning builder. */
    public static final class Builder {
        private final Map<ConservativeAirGeometry.Resolution, Integer>
                geometryIds = new LinkedHashMap<>();
        private final List<ConservativeAirGeometry.Resolution> geometries =
                new ArrayList<>();
        private final List<Integer> geometryContactPattern = new ArrayList<>();
        private final Map<SignatureKey, Integer> signatureIds =
                new LinkedHashMap<>();
        private final List<Integer> signatureGeometry = new ArrayList<>();
        private final List<Integer> signatureMaterial = new ArrayList<>();

        public int intern(ResolvedThermalSignature signature) {
            Objects.requireNonNull(signature, "signature");
            int geometryId = geometryIds.computeIfAbsent(
                    signature.airGeometry(), geometry -> {
                        int id = geometries.size();
                        geometries.add(geometry);
                        geometryContactPattern.add(0);
                        return id;
                    });
            int contactPatternId = signature.materialContactPatternId();
            if (contactPatternId != 0) {
                int current = geometryContactPattern.get(geometryId);
                if (current != 0 && current != contactPatternId) {
                    throw new IllegalArgumentException(
                            "one geometry resolved to multiple contact patterns");
                }
                geometryContactPattern.set(geometryId, contactPatternId);
            }
            SignatureKey key = new SignatureKey(
                    geometryId, signature.materialProfileId());
            Integer existing = signatureIds.get(key);
            if (existing != null) {
                return existing;
            }
            int id = signatureGeometry.size();
            signatureIds.put(key, id);
            signatureGeometry.add(geometryId);
            signatureMaterial.add(signature.materialProfileId());
            return id;
        }

        public ThermalSignatureTable build() {
            return new ThermalSignatureTable(this);
        }
    }

    private record SignatureKey(int geometryId, int materialProfileId) {
    }
}
