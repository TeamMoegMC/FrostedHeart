/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dense BlockState-ID lookup with sparse extended thermal semantics. */
public final class MinecraftStateThermalTable {
    public static final int UNRESOLVED_CODE = Integer.MIN_VALUE;
    public static final byte CAMPFIRE_PRESENT = 1;
    public static final byte CAMPFIRE_LIT = 1 << 1;

    public static final byte RADIATION_NONE = 0;
    public static final byte RADIATION_FIXED = 1;
    public static final byte RADIATION_LAVA_SURFACE = 2;

    private final int[] thermalStateCodeByStateId;
    private final int[] signatureByExtended;
    private final int[] radiationProfileByExtended;
    private final byte[] sourceFlagsByExtended;
    private final byte[] occlusionBySignature;
    private final byte[] occlusionByExtended;
    private final byte[] radiationModeByProfile;
    private final double[] radiationPowerByProfile;
    private final int radiationModeMask;

    private MinecraftStateThermalTable(Builder builder) {
        thermalStateCodeByStateId = builder.stateCodes;
        signatureByExtended = builder.extendedSignatures.stream()
                .mapToInt(Integer::intValue).toArray();
        sourceFlagsByExtended = new byte[builder.extendedSourceFlags.size()];
        occlusionByExtended = new byte[builder.extendedOcclusion.size()];
        for (int index = 0; index < sourceFlagsByExtended.length; index++) {
            sourceFlagsByExtended[index] = builder.extendedSourceFlags.get(index);
            occlusionByExtended[index] = builder.extendedOcclusion.get(index);
        }
        occlusionBySignature = new byte[builder.directOcclusion.size()];
        for (int index = 0; index < occlusionBySignature.length; index++) {
            occlusionBySignature[index] =
                    builder.directOcclusion.get(index) == 1 ? (byte) 1 : 0;
        }
        if (builder.radiationEnabled && builder.radiationModes.size() > 1) {
            radiationProfileByExtended = builder.extendedRadiationProfiles.stream()
                    .mapToInt(Integer::intValue).toArray();
            radiationModeByProfile = new byte[builder.radiationModes.size()];
            radiationPowerByProfile = new double[builder.radiationPowers.size()];
            for (int index = 0; index < radiationModeByProfile.length; index++) {
                radiationModeByProfile[index] = builder.radiationModes.get(index);
                radiationPowerByProfile[index] = builder.radiationPowers.get(index);
            }
            int modes = 0;
            for (byte mode : radiationModeByProfile) {
                modes |= 1 << mode;
            }
            radiationModeMask = modes;
        } else {
            radiationProfileByExtended = null;
            radiationModeByProfile = new byte[]{RADIATION_NONE};
            radiationPowerByProfile = new double[]{0.0D};
            radiationModeMask = 1 << RADIATION_NONE;
        }
    }

    public static Builder builder(int initialStateCapacity, boolean radiationEnabled) {
        return new Builder(initialStateCapacity, radiationEnabled);
    }

    public boolean radiationEnabled() {
        return radiationProfileByExtended != null;
    }

    public boolean hasRadiationMode(byte mode) {
        return mode >= 0 && mode < Integer.SIZE
                && (radiationModeMask & 1 << mode) != 0;
    }

    public int code(BlockState state) {
        int stateId = Block.BLOCK_STATE_REGISTRY.getId(state);
        return stateId < 0 || stateId >= thermalStateCodeByStateId.length
                ? UNRESOLVED_CODE : thermalStateCodeByStateId[stateId];
    }

    public int signatureId(BlockState state) {
        return signatureIdFromCode(code(state));
    }

    public int signatureIdFromCode(int code) {
        if (code >= 0) {
            return code;
        }
        if (code == UNRESOLVED_CODE) {
            return ThermalSignatureTable.UNRESOLVED;
        }
        int extended = ~code;
        return extended < signatureByExtended.length
                ? signatureByExtended[extended]
                : ThermalSignatureTable.UNRESOLVED;
    }

    public int radiationProfileIdFromCode(int code) {
        if (code >= 0 || code == UNRESOLVED_CODE
                || radiationProfileByExtended == null) {
            return 0;
        }
        int extended = ~code;
        return extended < radiationProfileByExtended.length
                ? radiationProfileByExtended[extended] : 0;
    }

    public boolean hasStaticRadiation(BlockState state) {
        return radiationProfileIdFromCode(code(state)) != 0;
    }

    public byte sourceFlagsFromCode(int code) {
        if (code >= 0 || code == UNRESOLVED_CODE) {
            return 0;
        }
        int extended = ~code;
        return extended < sourceFlagsByExtended.length
                ? sourceFlagsByExtended[extended] : 0;
    }

    public boolean blocksRadiationFromCode(int code) {
        if (code >= 0) {
            return code < occlusionBySignature.length
                    && occlusionBySignature[code] != 0;
        }
        if (code == UNRESOLVED_CODE) {
            return false;
        }
        int extended = ~code;
        return extended < occlusionByExtended.length
                && occlusionByExtended[extended] != 0;
    }

    public byte radiationMode(int profileId) {
        return profileId > 0 && profileId < radiationModeByProfile.length
                ? radiationModeByProfile[profileId] : RADIATION_NONE;
    }

    public double radiationPower(int profileId) {
        return profileId > 0 && profileId < radiationPowerByProfile.length
                ? radiationPowerByProfile[profileId] : 0.0D;
    }

    public int mutationFlags(BlockState oldState, BlockState newState) {
        int oldCode = code(oldState);
        int newCode = code(newState);
        int oldSignature = signatureIdFromCode(oldCode);
        int newSignature = signatureIdFromCode(newCode);
        int flags = oldSignature == ThermalSignatureTable.UNRESOLVED
                || newSignature == ThermalSignatureTable.UNRESOLVED
                || oldSignature != newSignature
                ? MinecraftThermalProfiles.TOPOLOGY_MUTATION : 0;
        if (sourceFlagsFromCode(oldCode) != sourceFlagsFromCode(newCode)) {
            flags |= MinecraftThermalProfiles.SOURCE_MUTATION;
        }
        if (radiationProfileIdFromCode(oldCode)
                != radiationProfileIdFromCode(newCode)) {
            flags |= MinecraftThermalProfiles.RADIATION_MUTATION;
        }
        if (oldSignature == newSignature
                && blocksRadiationFromCode(oldCode)
                        != blocksRadiationFromCode(newCode)) {
            flags |= MinecraftThermalProfiles.OCCLUSION_MUTATION;
        }
        return flags;
    }

    /** Startup-only table builder. */
    public static final class Builder {
        private int[] stateCodes;
        private final boolean radiationEnabled;
        private final Map<ExtendedSemantics, Integer> extendedIds =
                new LinkedHashMap<>();
        private final List<Integer> extendedSignatures = new ArrayList<>();
        private final List<Integer> extendedRadiationProfiles = new ArrayList<>();
        private final List<Byte> extendedSourceFlags = new ArrayList<>();
        private final List<Byte> extendedOcclusion = new ArrayList<>();
        private final List<Byte> directOcclusion = new ArrayList<>();
        private final List<Byte> radiationModes = new ArrayList<>();
        private final List<Double> radiationPowers = new ArrayList<>();
        private int maximumStateId = -1;

        private Builder(int initialStateCapacity, boolean radiationEnabled) {
            stateCodes = new int[Math.max(1, initialStateCapacity)];
            Arrays.fill(stateCodes, UNRESOLVED_CODE);
            this.radiationEnabled = radiationEnabled;
            radiationModes.add(RADIATION_NONE);
            radiationPowers.add(0.0D);
        }

        public int addRadiationProfile(
                byte mode,
                double power
        ) {
            if (!radiationEnabled) {
                return 0;
            }
            if ((mode != RADIATION_FIXED && mode != RADIATION_LAVA_SURFACE)
                    || !Double.isFinite(power) || power < 0.0D) {
                throw new IllegalArgumentException("invalid static radiation profile");
            }
            if (power == 0.0D) {
                return 0;
            }
            int id = radiationModes.size();
            radiationModes.add(mode);
            radiationPowers.add(power);
            return id;
        }

        public void put(
                BlockState state,
                int signatureId,
                int radiationProfileId,
                byte sourceFlags,
                boolean blocksRadiation
        ) {
            int stateId = Block.BLOCK_STATE_REGISTRY.getId(state);
            if (stateId < 0) {
                return;
            }
            ensureStateCapacity(stateId + 1);
            maximumStateId = Math.max(maximumStateId, stateId);
            boolean directOcclusionMatches = registerDirectOcclusion(
                    signatureId, blocksRadiation);
            if (radiationProfileId == 0 && sourceFlags == 0
                    && directOcclusionMatches) {
                stateCodes[stateId] = signatureId >= 0
                        ? signatureId : UNRESOLVED_CODE;
                return;
            }
            ExtendedSemantics semantics = new ExtendedSemantics(
                    signatureId,
                    radiationProfileId,
                    sourceFlags,
                    blocksRadiation);
            Integer extended = extendedIds.get(semantics);
            if (extended == null) {
                extended = extendedSignatures.size();
                if (extended == Integer.MAX_VALUE) {
                    throw new IllegalStateException(
                            "extended thermal state table exhausted int encoding");
                }
                extendedIds.put(semantics, extended);
                extendedSignatures.add(signatureId);
                extendedRadiationProfiles.add(radiationProfileId);
                extendedSourceFlags.add(sourceFlags);
                extendedOcclusion.add((byte) (blocksRadiation ? 1 : 0));
            }
            stateCodes[stateId] = ~extended;
        }

        public MinecraftStateThermalTable build() {
            stateCodes = Arrays.copyOf(
                    stateCodes, Math.max(1, maximumStateId + 1));
            return new MinecraftStateThermalTable(this);
        }

        private void ensureStateCapacity(int required) {
            if (required <= stateCodes.length) {
                return;
            }
            int old = stateCodes.length;
            int capacity = old;
            while (capacity < required) {
                capacity = Math.max(required, Math.multiplyExact(capacity, 2));
            }
            stateCodes = Arrays.copyOf(stateCodes, capacity);
            Arrays.fill(stateCodes, old, capacity, UNRESOLVED_CODE);
        }

        private boolean registerDirectOcclusion(
                int signatureId,
                boolean blocksRadiation
        ) {
            if (signatureId < 0) {
                return !blocksRadiation;
            }
            while (directOcclusion.size() <= signatureId) {
                directOcclusion.add((byte) -1);
            }
            byte encoded = (byte) (blocksRadiation ? 1 : 0);
            byte current = directOcclusion.get(signatureId);
            if (current == -1) {
                directOcclusion.set(signatureId, encoded);
                return true;
            }
            return current == encoded;
        }
    }

    private record ExtendedSemantics(
            int signatureId,
            int radiationProfileId,
            byte sourceFlags,
            boolean blocksRadiation
    ) {
    }
}
