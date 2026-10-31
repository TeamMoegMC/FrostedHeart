/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

/**
 * Owned source intervals in world coordinates. Pending input must not retain a
 * numerical layout, an arena allocation, or the dimension batch that carried it.
 * A material identity is kept until the owning topology handoff remaps it.
 */
public final class SpatialHeatInput {
    public static final byte MATERIAL = -1;
    public static final SpatialHeatInput EMPTY = new SpatialHeatInput(0);
    private final long[] positions, starts, ends, incarnations;
    private final long[] sourceIds, minimumPages;
    private final int[] sourceGenerations;
    private final int[] materialSlots, targetGenerations, materialProfiles, materialStates;
    private final byte[] faces;
    private final double[] amounts;

    private SpatialHeatInput(int size) {
        positions = new long[size]; starts = new long[size]; ends = new long[size];
        incarnations = new long[size];
        sourceIds = new long[size]; sourceGenerations = new int[size];
        minimumPages = new long[size];
        materialSlots = new int[size]; targetGenerations = new int[size]; materialProfiles = new int[size];
        materialStates = new int[size]; java.util.Arrays.fill(materialStates, -1);
        faces = new byte[size]; amounts = new double[size];
    }

    public int size() { return positions.length; }
    public long position(int index) { return positions[index]; }
    public byte face(int index) { return faces[index]; }
    public boolean isAir(int index) { return faces[index] != MATERIAL; }
    public boolean needsRouting(int index) { return faces[index] >= 6; }
    public int materialSlot(int index) { return materialSlots[index]; }
    public int generation(int index) { return targetGenerations[index]; }
    public int materialProfile(int index) { return materialProfiles[index]; }
    public long incarnation(int index) { return incarnations[index]; }
    public int materialStateId(int index) { return materialStates[index]; }
    public long sourceId(int index) { return sourceIds[index]; }
    public int sourceGeneration(int index) { return sourceGenerations[index]; }
    public long fromTick(int index) { return starts[index]; }
    public long toTick(int index) { return ends[index]; }
    public double amount(int index) { return amounts[index]; }
    public boolean impulse(int index) { return starts[index] == ends[index]; }
    public double energyJ(int index) {
        return impulse(index) ? amounts[index] : amounts[index] * ((ends[index] - starts[index]) / 20.0);
    }

    public record Target(long position, byte face, int materialSlot, int generation, int materialProfile, long incarnation,
            long sourceId, int sourceGeneration, int materialStateId, long minimumPageGeneration) {
        public Target(long position, byte face, int materialSlot, int generation, int materialProfile, long incarnation,
                long sourceId, int sourceGeneration, int materialStateId) {
            this(position, face, materialSlot, generation, materialProfile, incarnation, sourceId, sourceGeneration, materialStateId, 0);
        }
        public Target(long position, byte face, int materialSlot, int generation, int materialProfile, long incarnation,
                long sourceId, int sourceGeneration) {
            this(position, face, materialSlot, generation, materialProfile, incarnation, sourceId, sourceGeneration, -1);
        }
    }
    public Target target(int index) {
        return new Target(positions[index], faces[index], materialSlots[index], targetGenerations[index], materialProfiles[index],
                incarnations[index], sourceIds[index], sourceGenerations[index], materialStates[index], minimumPages[index]);
    }

    /** Called inside the input admission that already covers the recorded load. */
    public static SpatialHeatInput takeWorldIdentity(CutLoadBuffer frozen) {
        if (frozen.size() == 0) return EMPTY;
        if (!frozen.frozen()) throw new IllegalArgumentException("Source identity must be frozen before intake");
        var result = new SpatialHeatInput(frozen.size());
        for (int index = 0; index < result.size(); index++) {
            if (SourceBinding.isAirTarget(frozen.target(index))) {
                var load = frozen.airLoad(index, null);
                result.positions[index] = load.block();
                result.faces[index] = (byte) load.face().ordinal();
                result.materialSlots[index] = -1;
                result.targetGenerations[index] = load.pageGeneration();
            } else {
                result.positions[index] = frozen.materialBlock(index);
                result.faces[index] = MATERIAL;
                result.materialSlots[index] = SourceBinding.targetIndex(frozen.target(index));
                result.targetGenerations[index] = frozen.generation(index);
                result.materialProfiles[index] = frozen.materialProfile(index);
                result.incarnations[index] = frozen.materialIncarnation(index);
            }
            result.starts[index] = frozen.startTick(index);
            result.sourceIds[index] = frozen.sourceId(index); result.sourceGenerations[index] = frozen.sourceGeneration(index);
            result.ends[index] = frozen.endTick(index);
            result.amounts[index] = frozen.amount(index);
        }
        return result;
    }

    private void copy(int source, SpatialHeatInput target, int next) {
        target.positions[next] = positions[source]; target.faces[next] = faces[source];
        target.materialSlots[next] = materialSlots[source];
        target.targetGenerations[next] = targetGenerations[source];
        target.materialProfiles[next] = materialProfiles[source];
        target.materialStates[next] = materialStates[source];
        target.minimumPages[next] = minimumPages[source];
        target.incarnations[next] = incarnations[source];
        target.sourceIds[next] = sourceIds[source]; target.sourceGenerations[next] = sourceGenerations[source];
        target.starts[next] = starts[source]; target.ends[next] = ends[source]; target.amounts[next] = amounts[source];
    }

    public long retainedBytes() { return size() == 0 ? 0 : 368L + 77L * size(); }
    /** Builder is exclusively owned by intake or by a cancelled job being returned. */
    public static final class Builder {
        private SpatialHeatInput values = EMPTY;
        private int size;

        public void reserve(int required) {
            if (required <= values.size()) return;
            var next = new SpatialHeatInput(Math.max(required, Math.max(8, values.size() + values.size() / 2)));
            for (int index = 0; index < size; index++) values.copy(index, next, index);
            values = next;
        }
        public void append(SpatialHeatInput input, int record) {
            reserve(size + 1);
            input.copy(record, values, size++);
        }
        public void appendAt(SpatialHeatInput input, int record, Target target) {
            append(input, record);
            int next = size - 1;
            values.positions[next] = target.position; values.faces[next] = target.face;
            values.materialSlots[next] = target.materialSlot; values.targetGenerations[next] = target.generation;
            values.materialProfiles[next] = target.materialProfile;
            values.incarnations[next] = target.incarnation;
            values.materialStates[next] = target.materialStateId;
            values.minimumPages[next] = target.minimumPageGeneration;
            values.sourceIds[next] = target.sourceId; values.sourceGenerations[next] = target.sourceGeneration;
        }
        public void append(SpatialHeatInput input) {
            reserve(size + input.size());
            for (int index = 0; index < input.size(); index++) input.copy(index, values, size++);
        }
        public void appendAggregate(Target target, long fromTick, long toTick, double joules) {
            if (joules == 0) return;
            reserve(size + 1);
            int next = size++;
            values.positions[next] = target.position; values.faces[next] = target.face;
            values.materialSlots[next] = target.materialSlot; values.targetGenerations[next] = target.generation;
            values.materialProfiles[next] = target.materialProfile;
            values.incarnations[next] = target.incarnation;
            values.materialStates[next] = target.materialStateId;
            values.minimumPages[next] = target.minimumPageGeneration;
            values.sourceIds[next] = target.sourceId; values.sourceGenerations[next] = target.sourceGeneration;
            values.starts[next] = fromTick; values.ends[next] = toTick;
            values.amounts[next] = fromTick == toTick ? joules : joules * 20.0 / (toTick - fromTick);
        }
        public SpatialHeatInput freeze() {
            if (size == 0) return EMPTY;
            if (size == values.size()) { var result = values; values = EMPTY; size = 0; return result; }
            var result = new SpatialHeatInput(size);
            for (int index = 0; index < size; index++) values.copy(index, result, index);
            size = 0;
            return result;
        }
        public void clear() { size = 0; }
        public void release() { size = 0; values = EMPTY; }
    }
}
