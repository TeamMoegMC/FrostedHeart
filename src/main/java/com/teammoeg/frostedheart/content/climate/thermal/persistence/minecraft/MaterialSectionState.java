/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch.MaterialChanges.*;

/** Immutable, position-addressed body energy and its actual sampling time. */
public final class MaterialSectionState {
    private final short[] positions;
    private final double[] enthalpiesJ;
    private final byte[] branches;
    private final int[] paletteIndexes;
    private final int[] stateIds;
    private final MaterialThermalLaw[] laws;
    private final long brickMask;
    private final long savedTick;
    private long[] savedTicks;

    private MaterialSectionState(short[] positions, double[] enthalpiesJ, byte[] branches,
            int[] paletteIndexes, int[] stateIds, MaterialThermalLaw[] laws, long savedTick, long[] savedTicks) {
        this.positions = positions;
        this.enthalpiesJ = enthalpiesJ;
        this.branches = branches;
        this.paletteIndexes = paletteIndexes;
        this.stateIds = stateIds;
        this.laws = laws;
        this.savedTick = savedTick;
        this.savedTicks = savedTicks;
        long mask = 0;
        for (short position : positions) mask |= 1L << brickIndex(position);
        brickMask = mask;
    }

    public int size() { return positions.length; }
    public int position(int index) { return Short.toUnsignedInt(positions[index]); }
    public double enthalpyJ(int index) { return enthalpiesJ[index]; }
    public byte branch(int index) { return branches[index]; }
    public long savedTick(int index) { return savedTicks == null ? savedTick : savedTicks[index]; }
    public int stateId(int index) { return stateIds[paletteIndexes[index]]; }
    public MaterialThermalLaw law(int index) { return laws[paletteIndexes[index]]; }
    public int find(int position) { return Arrays.binarySearch(positions, (short) position); }
    public double temperatureC(int index) { return law(index).temperatureC(enthalpiesJ[index], branches[index]); }

    public long brickMask() {
        return brickMask;
    }

    public static boolean contentEquals(MaterialSectionState first, MaterialSectionState second) {
        return first == second || first != null && second != null
                && Arrays.equals(first.positions, second.positions)
                && Arrays.equals(first.enthalpiesJ, second.enthalpiesJ)
                && Arrays.equals(first.branches, second.branches)
                && Arrays.equals(first.paletteIndexes, second.paletteIndexes)
                && Arrays.equals(first.stateIds, second.stateIds)
                && Arrays.equals(first.laws, second.laws)
                && first.savedTick == second.savedTick && Arrays.equals(first.savedTicks, second.savedTicks);
    }

    /** Same positions, so editing data/palettes does not rescan the Section's Brick mask. */
    private MaterialSectionState(MaterialSectionState previous, double[] energies, byte[] branches,
            int[] indexes, int[] states, MaterialThermalLaw[] laws) {
        positions = previous.positions;
        brickMask = previous.brickMask;
        enthalpiesJ = energies;
        this.branches = branches;
        paletteIndexes = indexes;
        stateIds = states;
        this.laws = laws;
        savedTick = previous.savedTick;
        savedTicks = previous.savedTicks;
    }

    /** Main-thread edits copy once after a snapshot is shared; removals compact only when publishing. */
    public static final class Editor {
        private MaterialSectionState state;
        private boolean shared = true;
        private int size;
        private int[] references;
        private QueryPublication.MutableMaterialSample changeSample;

        public Editor(MaterialSectionState initial) {
            state = initial;
            size = initial == null ? 0 : initial.size();
        }

        public int find(int position) {
            int index = state == null ? -1 : state.find(position);
            return index < 0 || state.paletteIndexes[index] < 0 ? -1 : index;
        }

        public boolean isEmpty() { return size == 0; }

        public DormantChunkThermalState.MaterialRead read(int position, int stateId, QueryPublication.MutableMaterialSample out) {
            int index = find(position);
            if (index < 0) return DormantChunkThermalState.MaterialRead.MISSING;
            if (state.stateId(index) != stateId) return DormantChunkThermalState.MaterialRead.MISMATCH;
            state.readCheckpoint(index, out);
            return DormantChunkThermalState.MaterialRead.MATCH;
        }

        public MaterialSectionState snapshot() {
            if (state == null) return null;
            if (size != state.size()) {
                short[] positions = new short[size];
                double[] energies = new double[size];
                byte[] branches = new byte[size];
                int[] indexes = new int[size];
                long[] ticks = state.savedTicks == null ? null : new long[size];
                for (int i = 0, next = 0; i < state.size(); i++) {
                    if (state.paletteIndexes[i] < 0) continue;
                    positions[next] = state.positions[i];
                    energies[next] = state.enthalpiesJ[i];
                    branches[next] = state.branches[i];
                    indexes[next] = state.paletteIndexes[i];
                    if (ticks != null) ticks[next] = state.savedTick(i);
                    next++;
                }
                state = new MaterialSectionState(positions, energies, branches, indexes, state.stateIds, state.laws,
                        state.savedTick, ticks);
            }
            shared = true;
            return state;
        }

        public boolean applyChange(int position, int stateId, MaterialThermalLaw nextLaw, byte cause,
                double naturalC, long tick, double coolingRate, double coolingNaturalC) {
            int index = find(position);
            if (index < 0) return false;
            if (nextLaw == null) return removeAt(index);
            double energy = state.enthalpiesJ[index];
            byte branch = state.branches[index];
            if (cause != REPLACE && coolingRate > 0) {
                if (changeSample == null) changeSample = new QueryPublication.MutableMaterialSample();
                state.readCheckpoint(index, changeSample);
                DormantThermalCooling.project(changeSample, tick, coolingNaturalC, coolingRate);
                energy = changeSample.enthalpyJ();
                branch = changeSample.branch();
            }
            if (cause == REPLACE) energy = nextLaw.enthalpyAtTemperature(naturalC);
            else if (cause == MASS_CHANGE) energy = state.law(index).afterMassChange(energy, nextLaw, naturalC);
            else if (cause == GAMEPLAY_TRANSITION) energy = nextLaw.enthalpyAtTemperature(state.law(index).temperatureC(energy, branch));
            return updateAt(index, stateId, nextLaw, energy, cause == 0 ? branch : 0, tick);
        }

        public boolean update(int position, int stateId, MaterialThermalLaw law, double energy, byte branch, long tick) {
            int index = find(position);
            return index >= 0 && updateAt(index, stateId, law, energy, branch, tick);
        }

        private boolean updateAt(int index, int stateId, MaterialThermalLaw law, double energy, byte branch, long tick) {
            if (state.stateId(index) == stateId && state.law(index).equals(law)
                    && Double.doubleToLongBits(state.enthalpiesJ[index]) == Double.doubleToLongBits(energy)
                    && state.branches[index] == branch && state.savedTick(index) == tick) return false;
            writable();
            int palette = 0;
            while (palette < references.length && (state.stateIds[palette] != stateId || !law.equals(state.laws[palette]))) palette++;
            references[state.paletteIndexes[index]]--;
            if (palette == references.length) {
                palette = 0;
                while (palette < references.length && references[palette] != 0) palette++;
                if (palette == references.length) {
                    references = Arrays.copyOf(references, palette + 1);
                    state = new MaterialSectionState(state, state.enthalpiesJ, state.branches, state.paletteIndexes,
                            Arrays.copyOf(state.stateIds, palette + 1), Arrays.copyOf(state.laws, palette + 1));
                }
                state.stateIds[palette] = stateId;
                state.laws[palette] = law;
            }
            references[palette]++;
            state.paletteIndexes[index] = palette;
            state.enthalpiesJ[index] = energy;
            state.branches[index] = branch;
            if (state.savedTicks == null && tick != state.savedTick) {
                state.savedTicks = new long[state.size()];
                Arrays.fill(state.savedTicks, state.savedTick);
            }
            if (state.savedTicks != null) state.savedTicks[index] = tick;
            return true;
        }

        public boolean remove(int position) {
            int index = find(position);
            return index >= 0 && removeAt(index);
        }

        private boolean removeAt(int index) {
            if (size == 1) {
                state = null; size = 0; references = null;
                return true;
            }
            writable();
            references[state.paletteIndexes[index]]--;
            state.paletteIndexes[index] = -1;
            size--;
            return true;
        }

        private void writable() {
            if (shared) {
                state = new MaterialSectionState(state, state.enthalpiesJ.clone(), state.branches.clone(),
                        state.paletteIndexes.clone(), state.stateIds.clone(), state.laws.clone());
                if (state.savedTicks != null) state.savedTicks = state.savedTicks.clone();
                shared = false;
            }
            if (references == null) {
                references = new int[state.stateIds.length];
                for (int palette : state.paletteIndexes) if (palette >= 0) references[palette]++;
            }
        }
    }

    private BitSet referencedPalettes(int excludedEntry) {
        BitSet used = new BitSet(stateIds.length);
        for (int entry = 0; entry < paletteIndexes.length; entry++) {
            if (entry != excludedEntry) used.set(paletteIndexes[entry]);
        }
        return used;
    }

    /** Replace only sampled Bricks; untouched dormant material remains owned by the chunk. */
    public static MaterialSectionState merge(MaterialSectionState previous, MaterialSectionState current, long sampledBricks) {
        if (previous == null) return current;
        int retained = 0;
        for (short position : previous.positions) if ((sampledBricks & 1L << brickIndex(position)) == 0) retained++;
        if (retained == 0) return current;
        int count = retained + (current == null ? 0 : current.size());
        short[] positions = new short[count];
        double[] energy = new double[count];
        byte[] branches = new byte[count];
        int[] indexes = new int[count];
        long[] ticks = null;
        long firstTick = previous.savedTick(0);
        LinkedHashMap<PaletteKey, Integer> palette = new LinkedHashMap<>();
        int oldIndex = 0, newIndex = 0;
        for (int index = 0; index < count; index++) {
            while (oldIndex < previous.size() && (sampledBricks & 1L << brickIndex(previous.positions[oldIndex])) != 0) oldIndex++;
            boolean takeOld = oldIndex < previous.size() && (current == null || newIndex == current.size()
                    || previous.positions[oldIndex] < current.positions[newIndex]);
            MaterialSectionState source = takeOld ? previous : current;
            int sourceIndex = takeOld ? oldIndex++ : newIndex++;
            positions[index] = source.positions[sourceIndex];
            energy[index] = source.enthalpiesJ[sourceIndex];
            branches[index] = source.branches[sourceIndex];
            long tick = source.savedTick(sourceIndex);
            if (index == 0) firstTick = tick;
            if (ticks == null && tick != firstTick) {
                ticks = new long[count];
                Arrays.fill(ticks, 0, index, firstTick);
            }
            if (ticks != null) ticks[index] = tick;
            PaletteKey key = new PaletteKey(source.stateId(sourceIndex), source.law(sourceIndex));
            indexes[index] = palette.computeIfAbsent(key, ignored -> palette.size());
        }
        int[] states = new int[palette.size()];
        MaterialThermalLaw[] laws = new MaterialThermalLaw[palette.size()];
        for (var entry : palette.entrySet()) {
            states[entry.getValue()] = entry.getKey().stateId;
            laws[entry.getValue()] = entry.getKey().law;
        }
        return new MaterialSectionState(positions, energy, branches, indexes, states, laws, firstTick, ticks);
    }

    private record PaletteKey(int stateId, MaterialThermalLaw law) {}

    private static int brickIndex(int position) {
        return (position & 15) >>> 2 | (position >>> 4 & 15) >>> 2 << 2 | (position >>> 8 & 15) >>> 2 << 4;
    }

    private void readCheckpoint(int index, QueryPublication.MutableMaterialSample out) {
        out.setStored(enthalpiesJ[index], law(index), branches[index], savedTick(index));
    }

    public void read(int index, MaterialThermalLaw currentLaw, long tick, double naturalC, double coolingRate,
            QueryPublication.MutableMaterialSample out) {
        read(index, currentLaw, tick, naturalC, coolingRate,
                DormantThermalCooling.fraction(savedTick(index), tick, coolingRate), out);
    }

    public void read(int index, MaterialThermalLaw currentLaw, long tick, double naturalC, double coolingRate,
            double sensibleFraction, QueryPublication.MutableMaterialSample out) {
        readCheckpoint(index, out);
        DormantThermalCooling.project(out, tick, naturalC, coolingRate, sensibleFraction);
        adaptLaw(out, currentLaw);
    }

    public static void adaptLaw(QueryPublication.MutableMaterialSample out, MaterialThermalLaw currentLaw) {
        MaterialThermalLaw previous = out.law();
        double energy = out.enthalpyJ();
        byte branch = out.branch();
        if (!previous.equals(currentLaw)) {
            double temperature = previous.temperatureC(energy, branch);
            var oldEdge = previous.transition(branch);
            var newEdge = currentLaw.transition(branch);
            if (oldEdge != null && newEdge != null && oldEdge.targetStateId() == newEdge.targetStateId()) {
                energy = newEdge.sourceEnthalpyJ() + oldEdge.progress(energy)
                        * (newEdge.targetEnthalpyJ() - newEdge.sourceEnthalpyJ());
            } else {
                double latentRemainder = energy - previous.enthalpyAtTemperature(temperature);
                energy = currentLaw.enthalpyAtTemperature(temperature) + latentRemainder;
                branch = MaterialThermalLaw.SENSIBLE;
            }
        }
        out.setStored(energy, currentLaw, branch, out.sampleTick());
    }

    public static final class CaptureScratch {
        final double[] enthalpies = new double[4096];
        final byte[] branches = new byte[4096];
        final int[] palettes = new int[4096];
        final long[] presence = new long[64];
        final Int2IntOpenHashMap paletteByState = new Int2IntOpenHashMap();
        final ArrayList<Integer> states = new ArrayList<>();
        final ArrayList<MaterialThermalLaw> laws = new ArrayList<>();
        final QueryPublication.MutableMaterialSample sample = new QueryPublication.MutableMaterialSample();
    }

    public record Capture(boolean valid, long sampledBricks, MaterialSectionState state) {}

    public static Capture capture(PagePublication page, QueryPublication query,
            ThermalSignatureTable signatures, CaptureScratch scratch) {
        Arrays.fill(scratch.presence, 0);
        scratch.paletteByState.clear();
        scratch.paletteByState.defaultReturnValue(-1);
        scratch.states.clear();
        scratch.laws.clear();
        long tick = -1, sampledBricks = 0;
        int count = 0;
        for (int brick = 0; brick < 64; brick++) {
            var payload = page.brick(brick);
            if (!payload.resolved() || payload.firstSlot() < 0) continue;
            sampledBricks |= 1L << brick;
            if (payload.blockLayout() == null) continue;
            for (int block = 0; block < 64; block++) {
                int node = payload.blockLayout().nodeAt(block);
                int stateId = signatures.materialStateId(payload.signatureAtBlock(block));
                if (node < 0 || stateId < 0) continue;
                if (!query.tryReadMaterial(payload.firstSlot() + node, payload.arenaGeneration(),
                        page.topologyGeneration(), scratch.sample)) return new Capture(false, 0, null);
                if (tick >= 0 && tick != scratch.sample.sampleTick()) return new Capture(false, 0, null);
                tick = scratch.sample.sampleTick();
                int palette = scratch.paletteByState.get(stateId);
                if (palette < 0) {
                    palette = scratch.states.size();
                    scratch.paletteByState.put(stateId, palette);
                    scratch.states.add(stateId);
                    scratch.laws.add(scratch.sample.law());
                }
                int position = com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout.pageBlock(brick, block);
                scratch.presence[position >>> 6] |= 1L << (position & 63);
                scratch.enthalpies[position] = scratch.sample.enthalpyJ();
                scratch.branches[position] = scratch.sample.branch();
                scratch.palettes[position] = palette;
                count++;
            }
        }
        if (count == 0) return new Capture(true, sampledBricks, null);
        short[] positions = new short[count];
        double[] enthalpies = new double[count];
        byte[] branches = new byte[count];
        int[] palettes = new int[count];
        int index = 0;
        for (int word = 0; word < 64; word++) {
            long remaining = scratch.presence[word];
            while (remaining != 0) {
                int position = word * 64 + Long.numberOfTrailingZeros(remaining);
                remaining &= remaining - 1;
                positions[index] = (short) position;
                enthalpies[index] = scratch.enthalpies[position];
                branches[index] = scratch.branches[position];
                palettes[index++] = scratch.palettes[position];
            }
        }
        int[] stateIds = new int[scratch.states.size()];
        for (int i = 0; i < stateIds.length; i++) stateIds[i] = scratch.states.get(i);
        return new Capture(true, sampledBricks, new MaterialSectionState(positions, enthalpies, branches,
                palettes, stateIds, scratch.laws.toArray(MaterialThermalLaw[]::new), tick, null));
    }

    CompoundTag encode() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("tick", savedTick);
        if (savedTicks != null) tag.putLongArray("ticks", savedTicks);
        byte[] encodedPositions = new byte[positions.length * 2];
        long[] energies = new long[positions.length];
        for (int index = 0; index < positions.length; index++) {
            encodedPositions[index * 2] = (byte) (positions[index] >>> 8);
            encodedPositions[index * 2 + 1] = (byte) positions[index];
            energies[index] = Double.doubleToRawLongBits(enthalpiesJ[index]);
        }
        tag.putByteArray("positions", encodedPositions);
        tag.putLongArray("enthalpy", energies);
        tag.putByteArray("branches", branches);
        BitSet used = referencedPalettes(-1);
        int[] encodedIndexes = paletteIndexes;
        if (used.cardinality() != stateIds.length) {
            int[] remap = new int[stateIds.length];
            int next = 0;
            for (int index = used.nextSetBit(0); index >= 0; index = used.nextSetBit(index + 1)) remap[index] = next++;
            encodedIndexes = new int[paletteIndexes.length];
            for (int index = 0; index < encodedIndexes.length; index++) encodedIndexes[index] = remap[paletteIndexes[index]];
        }
        tag.putIntArray("palette_indexes", encodedIndexes);
        ListTag palette = new ListTag();
        for (int index = used.nextSetBit(0); index >= 0; index = used.nextSetBit(index + 1)) {
            CompoundTag entry = new CompoundTag();
            entry.put("state", NbtUtils.writeBlockState(Block.BLOCK_STATE_REGISTRY.byId(stateIds[index])));
            entry.putDouble("capacity", laws[index].capacityJPerK());
            entry.putDouble("offset", laws[index].offsetJ());
            writeEdge(entry, "heating", laws[index].heating());
            writeEdge(entry, "cooling", laws[index].cooling());
            palette.add(entry);
        }
        tag.put("palette", palette);
        return tag;
    }

    static MaterialSectionState decode(CompoundTag tag) {
        byte[] encoded = tag.getByteArray("positions"), branches = tag.getByteArray("branches");
        long[] energies = tag.getLongArray("enthalpy");
        int[] indexes = tag.getIntArray("palette_indexes");
        long tick = tag.getLong("tick");
        long[] ticks = tag.contains("ticks", Tag.TAG_LONG_ARRAY) ? tag.getLongArray("ticks") : null;
        if (energies.length == 0 || encoded.length != energies.length * 2
                || branches.length != energies.length || indexes.length != energies.length
                || ticks != null && ticks.length != energies.length) return null;
        ListTag palette = tag.getList("palette", Tag.TAG_COMPOUND);
        int[] stateIds = new int[palette.size()];
        MaterialThermalLaw[] laws = new MaterialThermalLaw[palette.size()];
        for (int index = 0; index < palette.size(); index++) {
            CompoundTag entry = palette.getCompound(index);
            stateIds[index] = readState(entry.getCompound("state"));
            double capacity = entry.getDouble("capacity"), offset = entry.getDouble("offset");
            if (!Double.isFinite(capacity) || capacity <= 0 || !Double.isFinite(offset)) continue;
            laws[index] = new MaterialThermalLaw(capacity, offset, readEdge(entry, "heating"), readEdge(entry, "cooling"));
        }
        short[] positions = new short[energies.length];
        double[] enthalpies = new double[energies.length];
        int count = 0, previous = -1;
        for (int index = 0; index < energies.length; index++) {
            int position = Byte.toUnsignedInt(encoded[index * 2]) << 8 | Byte.toUnsignedInt(encoded[index * 2 + 1]);
            double energy = Double.longBitsToDouble(energies[index]);
            int entry = indexes[index];
            if (position <= previous || position >= 4096 || !Double.isFinite(energy)
                    || entry < 0 || entry >= laws.length || laws[entry] == null) continue;
            previous = position;
            positions[count] = (short) position;
            enthalpies[count] = energy;
            branches[count] = branches[index];
            if (ticks != null) ticks[count] = ticks[index];
            indexes[count++] = entry;
        }
        return count == 0 ? null : new MaterialSectionState(Arrays.copyOf(positions, count),
                Arrays.copyOf(enthalpies, count), Arrays.copyOf(branches, count), Arrays.copyOf(indexes, count), stateIds, laws,
                tick, ticks == null ? null : Arrays.copyOf(ticks, count));
    }

    private static void writeEdge(CompoundTag parent, String key, MaterialThermalLaw.Transition edge) {
        if (edge == null) return;
        CompoundTag tag = new CompoundTag();
        tag.put("target", NbtUtils.writeBlockState(Block.BLOCK_STATE_REGISTRY.byId(edge.targetStateId())));
        tag.putDouble("temperature", edge.temperatureC());
        tag.putDouble("source_energy", edge.sourceEnthalpyJ());
        tag.putDouble("target_energy", edge.targetEnthalpyJ());
        parent.put(key, tag);
    }

    private static MaterialThermalLaw.Transition readEdge(CompoundTag parent, String key) {
        if (!parent.contains(key, Tag.TAG_COMPOUND)) return null;
        CompoundTag tag = parent.getCompound(key);
        double temperature = tag.getDouble("temperature"), source = tag.getDouble("source_energy"), target = tag.getDouble("target_energy");
        if (!Double.isFinite(temperature) || !Double.isFinite(source) || !Double.isFinite(target) || source == target) return null;
        return new MaterialThermalLaw.Transition(readState(tag.getCompound("target")), temperature, source, target);
    }

    private static int readState(CompoundTag state) {
        return Block.BLOCK_STATE_REGISTRY.getId(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), state));
    }
}
