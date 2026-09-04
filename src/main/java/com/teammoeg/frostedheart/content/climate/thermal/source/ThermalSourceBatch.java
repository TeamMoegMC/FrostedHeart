/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import java.util.Arrays;
import java.util.Objects;

/** Immutable exact-sized source event cut transferred to one dimension worker. */
public final class ThermalSourceBatch {
    private static final Kind[] KINDS = Kind.values();
    private static final ThermalSourceMode[] MODES = ThermalSourceMode.values();
    private static final byte[] NO_BYTES = new byte[0];
    private static final long[] NO_LONGS = new long[0];
    private static final int[] NO_INTS = new int[0];
    private static final double[] NO_DOUBLES = new double[0];
    private static final boolean[] NO_BOOLEANS = new boolean[0];
    private static final EmissionPort[][] NO_PORTS = new EmissionPort[0][];
    public static final ThermalSourceBatch EMPTY = new ThermalSourceBatch(
            NO_BYTES, NO_LONGS, NO_LONGS, NO_INTS, NO_BYTES, NO_DOUBLES,
            NO_BOOLEANS, NO_INTS, NO_INTS, NO_INTS, NO_INTS, NO_INTS,
            NO_PORTS);

    public enum Kind {
        REGISTER,
        POWER_CHANGE,
        ENABLED_CHANGE,
        IMPULSE,
        UNLOAD
    }

    private final byte[] kinds;
    private final long[] effectiveTicks;
    private final long[] sourceIds;
    private final int[] lifecycleGenerations;
    private final byte[] modes;
    private final double[] values;
    private final boolean[] enabled;
    private final int[] portIds;
    private final int[] anchorX;
    private final int[] anchorY;
    private final int[] anchorZ;
    private final int[] profileIds;
    private final EmissionPort[][] ports;

    private ThermalSourceBatch(
            byte[] kinds,
            long[] effectiveTicks,
            long[] sourceIds,
            int[] lifecycleGenerations,
            byte[] modes,
            double[] values,
            boolean[] enabled,
            int[] portIds,
            int[] anchorX,
            int[] anchorY,
            int[] anchorZ,
            int[] profileIds,
            EmissionPort[][] ports
    ) {
        this.kinds = kinds;
        this.effectiveTicks = effectiveTicks;
        this.sourceIds = sourceIds;
        this.lifecycleGenerations = lifecycleGenerations;
        this.modes = modes;
        this.values = values;
        this.enabled = enabled;
        this.portIds = portIds;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.profileIds = profileIds;
        this.ports = ports;
    }

    public int size() {
        return kinds.length;
    }

    public boolean isEmpty() {
        return kinds.length == 0;
    }

    public Kind kind(int index) {
        return KINDS[Byte.toUnsignedInt(kinds[index])];
    }

    public long effectiveTick(int index) {
        return effectiveTicks[index];
    }

    public long sourceId(int index) {
        return sourceIds[index];
    }

    public int lifecycleGeneration(int index) {
        return lifecycleGenerations[index];
    }

    public ThermalSourceMode mode(int index) {
        return MODES[Byte.toUnsignedInt(modes[index])];
    }

    /** Power for register/change, or signed joules for an impulse. */
    public double value(int index) {
        return values[index];
    }

    public boolean enabled(int index) {
        return enabled[index];
    }

    public int portId(int index) {
        return portIds[index];
    }

    public int anchorX(int index) { return anchorX[index]; }
    public int anchorY(int index) { return anchorY[index]; }
    public int anchorZ(int index) { return anchorZ[index]; }
    public int profileId(int index) { return profileIds[index]; }

    /** Returns the transferred register payload. Worker code must not mutate it. */
    public EmissionPort[] ports(int index) {
        return ports[index];
    }

    public static final class Builder {
        private static final int INITIAL_CAPACITY = 8;

        private byte[] kinds = new byte[INITIAL_CAPACITY];
        private long[] effectiveTicks = new long[INITIAL_CAPACITY];
        private long[] sourceIds = new long[INITIAL_CAPACITY];
        private int[] lifecycleGenerations = new int[INITIAL_CAPACITY];
        private byte[] modes = new byte[INITIAL_CAPACITY];
        private double[] values = new double[INITIAL_CAPACITY];
        private boolean[] enabled = new boolean[INITIAL_CAPACITY];
        private int[] portIds = new int[INITIAL_CAPACITY];
        private int[] anchorX = new int[INITIAL_CAPACITY];
        private int[] anchorY = new int[INITIAL_CAPACITY];
        private int[] anchorZ = new int[INITIAL_CAPACITY];
        private int[] profileIds = new int[INITIAL_CAPACITY];
        private EmissionPort[][] ports = new EmissionPort[INITIAL_CAPACITY][];
        private int size;
        private long lastEffectiveTick;

        public Builder(long initialTick) {
            if (initialTick < 0L) {
                throw new IllegalArgumentException("source batch baseline is invalid");
            }
            lastEffectiveTick = initialTick;
        }

        public void addRegister(
                long sourceId,
                int lifecycleGeneration,
                ThermalSourceMode mode,
                double powerW,
                boolean sourceEnabled,
                long effectiveTick,
                int sourceAnchorX,
                int sourceAnchorY,
                int sourceAnchorZ,
                int sourceProfileId,
                EmissionPort[] sourcePorts
        ) {
            if (lifecycleGeneration < 0 || sourceProfileId < 0) {
                throw new IllegalArgumentException(
                        "source lifecycle generation must be non-negative");
            }
            add(
                    Kind.REGISTER,
                    sourceId,
                    lifecycleGeneration,
                    Objects.requireNonNull(mode, "mode"),
                    powerW,
                    sourceEnabled,
                    -1,
                    sourceAnchorX,
                    sourceAnchorY,
                    sourceAnchorZ,
                    sourceProfileId,
                    Objects.requireNonNull(sourcePorts, "sourcePorts"),
                    effectiveTick);
        }

        public void addPowerChange(long sourceId, double powerW, long effectiveTick) {
            add(Kind.POWER_CHANGE, sourceId, 0, ThermalSourceMode.POWER_SOURCE,
                    powerW, false, -1, 0, 0, 0, -1,
                    null, effectiveTick);
        }

        public void addEnabledChange(
                long sourceId,
                boolean sourceEnabled,
                long effectiveTick
        ) {
            add(Kind.ENABLED_CHANGE, sourceId, 0, ThermalSourceMode.POWER_SOURCE,
                    0.0D, sourceEnabled, -1, 0, 0, 0, -1,
                    null, effectiveTick);
        }

        public void addImpulse(
                long sourceId,
                int portId,
                double signedEnergyJ,
                long effectiveTick
        ) {
            add(Kind.IMPULSE, sourceId, 0, ThermalSourceMode.POWER_SOURCE,
                    signedEnergyJ, false, portId, 0, 0, 0, -1,
                    null, effectiveTick);
        }

        public void addUnload(
                long sourceId,
                int expectedLifecycleGeneration,
                long effectiveTick
        ) {
            if (expectedLifecycleGeneration < 0) {
                throw new IllegalArgumentException(
                        "source lifecycle generation must be non-negative");
            }
            add(Kind.UNLOAD, sourceId, expectedLifecycleGeneration,
                    ThermalSourceMode.POWER_SOURCE, 0.0D, false, -1,
                    0, 0, 0, -1, null, effectiveTick);
        }

        public ThermalSourceBatch buildAndReset() {
            if (size == 0) {
                return EMPTY;
            }
            ThermalSourceBatch result = new ThermalSourceBatch(
                    Arrays.copyOf(kinds, size),
                    Arrays.copyOf(effectiveTicks, size),
                    Arrays.copyOf(sourceIds, size),
                    Arrays.copyOf(lifecycleGenerations, size),
                    Arrays.copyOf(modes, size),
                    Arrays.copyOf(values, size),
                    Arrays.copyOf(enabled, size),
                    Arrays.copyOf(portIds, size),
                    Arrays.copyOf(anchorX, size),
                    Arrays.copyOf(anchorY, size),
                    Arrays.copyOf(anchorZ, size),
                    Arrays.copyOf(profileIds, size),
                    Arrays.copyOf(ports, size));
            Arrays.fill(ports, 0, size, null);
            size = 0;
            return result;
        }

        private void add(
                Kind kind,
                long sourceId,
                int lifecycleGeneration,
                ThermalSourceMode mode,
                double value,
                boolean sourceEnabled,
                int portId,
                int sourceAnchorX,
                int sourceAnchorY,
                int sourceAnchorZ,
                int sourceProfileId,
                EmissionPort[] sourcePorts,
                long effectiveTick
        ) {
            if (!Double.isFinite(value) || effectiveTick < lastEffectiveTick) {
                throw new IllegalArgumentException(
                        "source value must be finite and ticks monotonic");
            }
            ensureCapacity(size + 1);
            kinds[size] = (byte) kind.ordinal();
            effectiveTicks[size] = effectiveTick;
            sourceIds[size] = sourceId;
            lifecycleGenerations[size] = lifecycleGeneration;
            modes[size] = (byte) mode.ordinal();
            values[size] = value;
            enabled[size] = sourceEnabled;
            portIds[size] = portId;
            anchorX[size] = sourceAnchorX;
            anchorY[size] = sourceAnchorY;
            anchorZ[size] = sourceAnchorZ;
            profileIds[size] = sourceProfileId;
            ports[size] = sourcePorts;
            lastEffectiveTick = effectiveTick;
            size++;
        }

        private void ensureCapacity(int required) {
            if (required <= kinds.length) {
                return;
            }
            int capacity = Math.max(required, kinds.length + (kinds.length >>> 1));
            kinds = Arrays.copyOf(kinds, capacity);
            effectiveTicks = Arrays.copyOf(effectiveTicks, capacity);
            sourceIds = Arrays.copyOf(sourceIds, capacity);
            lifecycleGenerations = Arrays.copyOf(lifecycleGenerations, capacity);
            modes = Arrays.copyOf(modes, capacity);
            values = Arrays.copyOf(values, capacity);
            enabled = Arrays.copyOf(enabled, capacity);
            portIds = Arrays.copyOf(portIds, capacity);
            anchorX = Arrays.copyOf(anchorX, capacity);
            anchorY = Arrays.copyOf(anchorY, capacity);
            anchorZ = Arrays.copyOf(anchorZ, capacity);
            profileIds = Arrays.copyOf(profileIds, capacity);
            ports = Arrays.copyOf(ports, capacity);
        }
    }
}
