/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared immutable whole-block attributes; no shape or per-position cache. */
public final class ThermalSignatureTable {
    public static final int UNRESOLVED = -1;
    private final byte[] ventilation;
    private final int[] materials;
    private final int[] materialStates;
    private final int[] materialTypes;
    private final byte[] fullContactFaces;
    private final boolean[] airMatter;
    private final Integer[] payloads;

    private ThermalSignatureTable(List<ResolvedThermalSignature> values) {
        ventilation = new byte[values.size()];
        materials = new int[values.size()];
        materialStates = new int[values.size()];
        materialTypes = new int[values.size()];
        fullContactFaces = new byte[values.size()];
        airMatter = new boolean[values.size()];
        payloads = new Integer[values.size()];
        for (int i = 0; i < values.size(); i++) {
            ventilation[i] = (byte) values.get(i).ventilation();
            materials[i] = values.get(i).materialProfileId();
            materialStates[i] = values.get(i).materialStateId();
            materialTypes[i] = values.get(i).materialTypeId();
            fullContactFaces[i] = (byte) values.get(i).fullContactFaces();
            airMatter[i] = values.get(i).airMatter();
            payloads[i] = i;
        }
    }
    public boolean valid(int id) { return id >= 0 && id < ventilation.length; }
    public int ventilation(int id) { return valid(id) ? ventilation[id] : 0; }
    public int materialProfileId(int id) { return valid(id) ? materials[id] : 0; }
    public boolean isAir(int id) { return valid(id) && airMatter[id]; }
    public int materialStateId(int id) { return valid(id) ? materialStates[id] : -1; }
    public boolean sameMaterialType(int first, int second) {
        return valid(first) && valid(second) && (materialTypes[first] < 0 || materialTypes[second] < 0
                || materialTypes[first] == materialTypes[second]);
    }
    public int fullContactFaces(int id) { return valid(id) ? fullContactFaces[id] : 0; }
    public boolean mergeable(int id) { return isAir(id) && ventilation(id) == 100 && materialProfileId(id) == 0; }
    public Integer uniformPayload(int id) { return id == UNRESOLVED ? UNRESOLVED : payloads[id]; }
    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private final Map<ResolvedThermalSignature, Integer> ids = new LinkedHashMap<>();
        private final List<ResolvedThermalSignature> values = new ArrayList<>();
        public int intern(ResolvedThermalSignature value) {
            return ids.computeIfAbsent(value, v -> { int id = values.size(); values.add(v); return id; });
        }
        public ThermalSignatureTable build() { return new ThermalSignatureTable(values); }
    }
}
