/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.query;

import java.util.Arrays;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MinimumThermalState;

/** One immutable region reading. Geometry metadata is shared across ordinary cuts. */
public final class RegionalQuerySnapshot {
    private static final java.util.concurrent.atomic.AtomicLong FRAME_IDS = new java.util.concurrent.atomic.AtomicLong();
    private final long frameId = FRAME_IDS.incrementAndGet();
    public record Accepted(RegionalQuerySnapshot field, long sampleTick, long consumedSequence) { }

    /** One physical owner and one volatile publication point, shared by the runtime and query directory. */
    public static final class Handle {
        public final long regionId;
        private volatile Accepted accepted = new Accepted(null, -1, 0);
        public Handle(long regionId) { this.regionId = regionId; }
        public Accepted accepted() { return accepted; }
        public void publish(RegionalQuerySnapshot field, long tick, long sequence) {
            accepted = new Accepted(field, tick, sequence);
        }
    }

    public static final class Template {
        final long regionId;
        final Handle handle;
        final AirFieldLayout air;
        final int[] cells, generations, pageSlots;
        final int[] distinctPages, cellPageIndexes;
        final long[] pageBrickMasks;
        final PageIndex checkpointPages;
        final byte[] bricks;
        final int[] materialSlots, materialStateIds;
        final long[] materialIncarnations;
        final long[] materialPositions;
        final MaterialThermalLaw[] laws;

        private Template(TemplatePreparation prepared) {
            handle = prepared.handle; regionId = handle.regionId; air = prepared.air;
            materialSlots = prepared.materialSlots; materialIncarnations = prepared.materialIncarnations; laws = prepared.laws;
            materialPositions = prepared.materialPositions;
            materialStateIds = prepared.materialStateIds;
            cells = prepared.cells; generations = prepared.generations; pageSlots = prepared.pageSlots; bricks = prepared.bricks;
            cellPageIndexes = prepared.cellPageIndexes; distinctPages = prepared.pageIndexes.keySet().toIntArray();
            pageBrickMasks = new long[distinctPages.length];
            for (int index = 0; index < distinctPages.length; index++) pageBrickMasks[index] = prepared.pageMasks.get(distinctPages[index]);
            checkpointPages = prepared.checkpointPages;
        }

        int cellIndex(int slot) { return Arrays.binarySearch(cells, slot); }
        int bodyIndex(int slot) { return Arrays.binarySearch(materialSlots, slot); }

    }

    /** Immutable row indexes shared by all frames of one topology; positions within each page keep solver order. */
    public static final class PageIndex {
        private final long[] sections;
        private final int[] airOffsets, airEntries, materialOffsets, materialEntries;
        private PageIndex(TemplatePreparation prepared) {
            sections = prepared.sectionKeys.toLongArray();
            airOffsets = offsets(prepared.airCounts); materialOffsets = offsets(prepared.materialCounts);
            airEntries = new int[prepared.air.componentCount()]; materialEntries = new int[prepared.materialSlots.length];
        }
        private static int[] offsets(it.unimi.dsi.fastutil.ints.IntArrayList counts) {
            int[] offsets = new int[counts.size() + 1];
            for (int page = 0; page < counts.size(); page++) {
                offsets[page + 1] = offsets[page] + counts.getInt(page);
                counts.set(page, offsets[page]);
            }
            return offsets;
        }
        public int size() { return sections.length; }
        public long section(int page) { return sections[page]; }
        public int airCount(int page) { return airOffsets[page + 1] - airOffsets[page]; }
        public int airComponent(int page, int ordinal) { return airEntries[airOffsets[page] + ordinal]; }
        public int materialCount(int page) { return materialOffsets[page + 1] - materialOffsets[page]; }
        public int materialBody(int page, int ordinal) { return materialEntries[materialOffsets[page] + ordinal]; }
    }

    /** Merge already ordered Air/material addresses, then capture fixed geometry in bounded runs. */
    public static final class TemplatePreparation {
        final Handle handle;
        final AirFieldLayout air;
        final int[] materialSlots, cells, generations, pageSlots, cellPageIndexes;
        final int[] materialStateIds;
        final java.util.function.IntUnaryOperator stateId;
        final long[] materialIncarnations;
        final long[] materialPositions;
        final MaterialThermalLaw[] laws;
        final byte[] bricks;
        final ThermalCellArena arena;
        final it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap pageIndexes = new it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap();
        final it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap pageMasks = new it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap();
        final it.unimi.dsi.fastutil.longs.LongArrayList sectionKeys = new it.unimi.dsi.fastutil.longs.LongArrayList();
        final it.unimi.dsi.fastutil.ints.IntArrayList airCounts = new it.unimi.dsi.fastutil.ints.IntArrayList();
        final it.unimi.dsi.fastutil.ints.IntArrayList materialCounts = new it.unimi.dsi.fastutil.ints.IntArrayList();
        PageIndex checkpointPages;
        int phase, cursor, airCursor, body;
        private Template result;
        public TemplatePreparation(Handle handle, AirFieldLayout air, int[] materialSlots, ThermalCellArena arena,
                java.util.function.IntUnaryOperator stateId) {
            this.handle = handle; this.air = air; this.materialSlots = materialSlots; this.arena = arena;
            this.stateId = stateId;
            cells = new int[air.componentCount() + materialSlots.length]; generations = new int[cells.length];
            pageSlots = new int[cells.length]; cellPageIndexes = new int[cells.length]; bricks = new byte[cells.length];
            laws = new MaterialThermalLaw[materialSlots.length]; materialIncarnations = new long[materialSlots.length];
            materialPositions = new long[materialSlots.length];
            materialStateIds = new int[materialSlots.length];
            pageIndexes.defaultReturnValue(-1);
        }
        public boolean run(long deadline) {
            do {
                if (phase == 0) {
                    int end = Math.min(cells.length, cursor + 512);
                    for (; cursor < end; cursor++) {
                        boolean material = body < materialSlots.length && (airCursor == air.componentCount()
                                || materialSlots[body] < air.component(airCursor).arenaSlot);
                        int slot = material ? materialSlots[body++] : air.component(airCursor++).arenaSlot;
                        cells[cursor] = slot; generations[cursor] = arena.lifecycleGeneration(slot); pageSlots[cursor] = arena.pageSlot(slot);
                        bricks[cursor] = (byte) (((arena.minimum(slot, 0) & 15) >>> 2) | ((arena.minimum(slot, 2) & 15) >>> 2) << 2
                                | ((arena.minimum(slot, 1) & 15) >>> 2) << 4);
                        int index = pageIndexes.get(pageSlots[cursor]);
                        if (index < 0) {
                            index = pageIndexes.size(); pageIndexes.put(pageSlots[cursor], index);
                            sectionKeys.add(net.minecraft.core.SectionPos.asLong(arena.minimum(slot, 0) >> 4,
                                    arena.minimum(slot, 1) >> 4, arena.minimum(slot, 2) >> 4));
                            airCounts.add(0); materialCounts.add(0);
                        }
                        var counts = material ? materialCounts : airCounts;
                        counts.set(index, counts.getInt(index) + 1);
                        cellPageIndexes[cursor] = index;
                        pageMasks.put(pageSlots[cursor], pageMasks.get(pageSlots[cursor]) | 1L << Byte.toUnsignedInt(bricks[cursor]));
                    }
                    if (cursor == cells.length) { checkpointPages = new PageIndex(this); phase++; cursor = 0; }
                } else if (phase == 1) {
                    int end = Math.min(materialSlots.length, cursor + 512);
                    for (; cursor < end; cursor++) {
                        laws[cursor] = arena.materialLaw(materialSlots[cursor]);
                        materialIncarnations[cursor] = arena.materialIncarnation(materialSlots[cursor]);
                        materialPositions[cursor] = net.minecraft.core.BlockPos.asLong(arena.minimum(materialSlots[cursor], 0),
                                arena.minimum(materialSlots[cursor], 1), arena.minimum(materialSlots[cursor], 2));
                        materialStateIds[cursor] = stateId.applyAsInt(materialSlots[cursor]);
                        int page = pageIndexes.get(arena.pageSlot(materialSlots[cursor]));
                        int entry = materialCounts.getInt(page);
                        checkpointPages.materialEntries[entry] = cursor; materialCounts.set(page, entry + 1);
                    }
                    if (cursor == materialSlots.length) { phase++; cursor = 0; }
                } else if (phase == 2) {
                    int end = Math.min(air.componentCount(), cursor + 512);
                    for (; cursor < end; cursor++) {
                        int page = pageIndexes.get(arena.pageSlot(air.component(cursor).arenaSlot));
                        int entry = airCounts.getInt(page);
                        checkpointPages.airEntries[entry] = cursor; airCounts.set(page, entry + 1);
                    }
                    if (cursor == air.componentCount()) phase++;
                } else { result = new Template(this); return true; }
            } while (System.nanoTime() < deadline);
            return false;
        }
        public Template result() { return result; }
    }

    final Template template;
    final long sampleTick, topologyGeneration;
    final double referenceC;
    final double[] airC, enthalpyJ, airMeanC, airMinimumC, airMaximumC, airOffsetSumC;
    final byte[] branches;
    final boolean[] waiting;
    final long[] requests;
    final boolean[] hotMaterials;
    private final MinimumThermalState minimum;
    private final MaterialAnchor materialAnchor;
    private final MinimumThermalState minimumSummary;
    private final MaterialAnchor minimumSummaryAnchor;
    private long infraredPreviousId;
    private long[] changedBrickMasks;
    private Object[] retainedArrays;
    int minimumInfraredEpoch;

    /** A fixed list of allocation families; callers never walk the layout to meter an ordinary frame. */
    Object[] payloadArrays() {
        if (retainedArrays != null) return retainedArrays;
        var arrays = new java.util.ArrayList<Object>(40);
        java.util.Collections.addAll(arrays, template.cells, template.generations, template.pageSlots, template.bricks,
                template.materialSlots, template.laws, template.materialIncarnations, template.materialPositions,
                template.materialStateIds,
                template.distinctPages, template.cellPageIndexes, template.pageBrickMasks,
                template.checkpointPages.sections, template.checkpointPages.airOffsets, template.checkpointPages.airEntries,
                template.checkpointPages.materialOffsets, template.checkpointPages.materialEntries,
                airC, enthalpyJ, branches, waiting, requests, hotMaterials,
                airMeanC, airMinimumC, airMaximumC, airOffsetSumC, changedBrickMasks);
        if (minimum != null) java.util.Collections.addAll(arrays, minimum.stateArrays());
        if (minimumSummary != null) java.util.Collections.addAll(arrays, minimumSummary.stateArrays());
        if (materialAnchor != null) java.util.Collections.addAll(arrays, materialAnchor.payloadArrays());
        if (minimumSummaryAnchor != null) java.util.Collections.addAll(arrays, minimumSummaryAnchor.payloadArrays());
        // This fixed, small family list is not a traversal of the thermal graph.
        int count = 0;
        for (int index = 0; index < arrays.size(); index++) {
            Object array = arrays.get(index);
            if (array == null) continue;
            boolean present = false;
            for (int previous = 0; previous < count; previous++) if (arrays.get(previous) == array) { present = true; break; }
            if (!present) arrays.set(count++, array);
        }
        return retainedArrays = arrays.subList(0, count).toArray();
    }

    public RegionalQuerySnapshot(Template template, double[] coefficientsC, double[] materialH,
            byte[] materialBranches, boolean[] materialWaiting,
            ThermalCellArena arena, double referenceC,
            long sampleTick, long topologyGeneration, RegionalQuerySnapshot previous) {
        this(new Preparation(template, coefficientsC, materialH, materialBranches, materialWaiting,
                arena, referenceC, sampleTick, topologyGeneration, previous).finish());
    }

    private RegionalQuerySnapshot(Preparation prepared) {
        template = prepared.template; sampleTick = prepared.sampleTick; topologyGeneration = prepared.topologyGeneration;
        referenceC = prepared.referenceC; airC = prepared.airC; enthalpyJ = prepared.enthalpyJ;
        branches = prepared.branches; waiting = prepared.waiting; requests = prepared.requests;
        hotMaterials = prepared.hotMaterials; airMeanC = prepared.airMeanC; airMinimumC = prepared.airMinimumC;
        airMaximumC = prepared.airMaximumC; airOffsetSumC = prepared.airOffsetSumC;
        infraredPreviousId = prepared.previous == null ? 0 : prepared.previous.frameId;
        changedBrickMasks = prepared.changedBrickMasks;
        minimum = null; materialAnchor = null; minimumSummary = null; minimumSummaryAnchor = null;
        minimumInfraredEpoch = prepared.previous == null ? 0 : prepared.previous.minimumInfraredEpoch;
    }

    /** Private publication preparation. It never exposes partially copied field arrays. */
    public static final class Preparation {
        private static final int BLOCK = 512;
        final Template template;
        final long sampleTick, topologyGeneration;
        final double referenceC;
        final double[] sourceC, sourceH, airC, enthalpyJ, airMeanC, airMinimumC, airMaximumC, airOffsetSumC;
        final byte[] sourceBranches, branches;
        final boolean[] sourceWaiting, waiting, hotMaterials;
        final long[] requests;
        final ThermalCellArena arena;
        final RegionalQuerySnapshot previous;
        final long[] changedBrickMasks;
        boolean airChanged;
        int bodyCursor, airCursor;
        int phase, cursor;
        private RegionalQuerySnapshot result;
        public static long backingBytes(Template template) {
            int bodies = template.materialSlots.length, components = template.air.componentCount();
            return 256 + AirBackingMeter.arrayPayloadBytes(template.air.coefficientCount(), 8)
                    + 2 * AirBackingMeter.arrayPayloadBytes(bodies, 8) + 3 * AirBackingMeter.arrayPayloadBytes(bodies, 1)
                    + 4 * AirBackingMeter.arrayPayloadBytes(components, 8)
                    + AirBackingMeter.arrayPayloadBytes(template.distinctPages.length, 8);
        }

        public Preparation(Template template, double[] coefficientsC, double[] materialH,
                byte[] materialBranches, boolean[] materialWaiting,
                ThermalCellArena arena, double referenceC, long sampleTick, long topologyGeneration,
                RegionalQuerySnapshot previous) {
            this.template = template; sourceC = coefficientsC; sourceH = materialH;
            sourceBranches = materialBranches; sourceWaiting = materialWaiting;
            this.arena = arena; this.referenceC = referenceC; this.sampleTick = sampleTick; this.topologyGeneration = topologyGeneration;
            this.previous = previous; airChanged = previous == null || previous.template != template;
            changedBrickMasks = new long[template.distinctPages.length];
            airC = new double[template.air.coefficientCount()];
            int bodies = template.materialSlots.length, components = template.air.componentCount();
            enthalpyJ = new double[bodies]; branches = new byte[bodies]; waiting = new boolean[bodies];
            requests = new long[bodies]; hotMaterials = new boolean[bodies];
            airMeanC = new double[components]; airMinimumC = new double[components];
            airMaximumC = new double[components]; airOffsetSumC = new double[components];
        }

        public boolean run(long deadline) {
            do {
                if (phase == 0) {
                    int begin = cursor;
                    int end = Math.min(airC.length, cursor + 4096);
                    System.arraycopy(sourceC, cursor, airC, cursor, end - cursor); cursor = end;
                    if (!airChanged) for (int index = begin; index < end; index++)
                        if (airC[index] != previous.airC[index]) { airChanged = true; break; }
                    if (cursor == airC.length) { phase++; cursor = 0; }
                } else if (phase == 1) {
                    int end = Math.min(enthalpyJ.length, cursor + BLOCK);
                    for (; cursor < end; cursor++) {
                        int slot = template.materialSlots[cursor];
                        enthalpyJ[cursor] = sourceH[cursor]; branches[cursor] = sourceBranches[cursor];
                        waiting[cursor] = sourceWaiting[cursor];
                        requests[cursor] = arena.hasMaterialTransition(slot) ? arena.phaseRequestSequence(slot) : 0;
                        var transition = template.laws[cursor].transition(branches[cursor]);
                        hotMaterials[cursor] = arena.materialLayoutPending(slot)
                                || transition != null && (arena.phaseRequestOutstanding(slot) || transition.progress(enthalpyJ[cursor]) > 0);
                    }
                    if (cursor == enthalpyJ.length) { phase++; cursor = 0; }
                } else if (phase == 2) {
                    int end = Math.min(template.air.componentCount(), cursor + 16);
                    for (; cursor < end; cursor++) {
                        var component = template.air.component(cursor);
                        long blocks = component.airBlocks; double total = 0;
                        while (blocks != 0) {
                            int block = Long.numberOfTrailingZeros(blocks); blocks &= blocks - 1;
                            double x = component.minX + (block & 3) + .5, y = component.minY + (block >>> 4) + .5;
                            double z = component.minZ + (block >>> 2 & 3) + .5;
                            total += component.isUnitGrid() ? component.temperatureOffsetAtAirCell(block, x, y, z, airC)
                                    : component.temperatureOffsetC(x, y, z, airC);
                        }
                        airMeanC[cursor] = referenceC + total / Long.bitCount(component.airBlocks); airOffsetSumC[cursor] = total;
                        airMinimumC[cursor] = referenceC + component.minimumOffsetC(airC);
                        airMaximumC[cursor] = referenceC + component.maximumOffsetC(airC);
                    }
                    if (cursor == template.air.componentCount()) { phase++; cursor = 0; }
                } else if (phase == 3) {
                    int end = Math.min(template.cells.length, cursor + BLOCK);
                    for (; cursor < end; cursor++) {
                        int slot = template.cells[cursor];
                        boolean material = bodyCursor < template.materialSlots.length && template.materialSlots[bodyCursor] == slot;
                        double temperature = material ? template.laws[bodyCursor].temperatureC(enthalpyJ[bodyCursor], branches[bodyCursor]) : airMeanC[airCursor];
                        double before = previous == null || previous.template != template ? Double.NaN : previous.temperatureC(slot);
                        if (previous == null || QueryPublication.quantizedInfrared(before) != QueryPublication.quantizedInfrared(temperature)
                                || !material && airChanged)
                            changedBrickMasks[template.cellPageIndexes[cursor]] |= 1L << Byte.toUnsignedInt(template.bricks[cursor]);
                        if (material) bodyCursor++; else airCursor++;
                    }
                    if (cursor == template.cells.length) { phase++; cursor = 0; }
                } else {
                    if (result == null) result = new RegionalQuerySnapshot(this);
                    return true;
                }
            } while (System.nanoTime() < deadline);
            return false;
        }
        private Preparation finish() { while (!run(Long.MAX_VALUE)) { } return this; }
        public RegionalQuerySnapshot result() {
            if (result == null) throw new IllegalStateException("Snapshot is still private preparation");
            return result;
        }
    }

    public RegionalQuerySnapshot withMinimumMaterialControl(int slot, double enthalpy, byte branch, boolean waiting, long request) {
        int body = template.bodyIndex(slot);
        if (body < 0) return this;
        int index = minimum.layout().phaseIndex(slot);
        double difference = enthalpy - materialEnthalpyJ(body);
        int pool = materialAnchor.pools[body];
        double capacityChange = (materialAnchor.controlWaiting(body) ? 1 : 0) - (waiting ? 1 : 0);
        var state = index < 0 ? minimum.withPoolControl(pool, difference, capacityChange * template.laws[body].capacityJPerK())
                : minimum.withPhaseControl(index, enthalpy, branch, waiting);
        double controlJ = index < 0 ? enthalpy - materialAnchor.enthalpyJ[body] - template.laws[body].capacityJPerK()
                * (minimum.nodeTemperatureC(pool) - materialAnchor.initialTemperatureC[pool - MinimumThermalState.AIR_NODES]) : 0;
        var anchor = materialAnchor.withControl(body, controlJ, enthalpy, branch, waiting, request);
        return new RegionalQuerySnapshot(template, state, anchor, sampleTick, topologyGeneration, referenceC);
    }

    /** Coordinator controls are immutable until published; helpers never see arrays being edited. */
    public RegionalQuerySnapshot withMaterialControl(int slot, double enthalpy, byte branch, boolean waiting, long request) {
        int body = template.bodyIndex(slot);
        if (body < 0) return this;
        if (minimum != null) return withMinimumMaterialControl(slot, enthalpy, branch, waiting, request);
        if (enthalpyJ[body] == enthalpy && branches[body] == branch && this.waiting[body] == waiting && requests[body] == request)
            return this;
        return new RegionalQuerySnapshot(this, body, enthalpy, branch, waiting, request);
    }

    private RegionalQuerySnapshot(RegionalQuerySnapshot previous, int body, double enthalpy, byte branch, boolean wait, long request) {
        minimumInfraredEpoch = previous.minimumInfraredEpoch;
        template = previous.template; sampleTick = previous.sampleTick; topologyGeneration = previous.topologyGeneration;
        referenceC = previous.referenceC; airC = previous.airC; airMeanC = previous.airMeanC;
        airMinimumC = previous.airMinimumC; airMaximumC = previous.airMaximumC; airOffsetSumC = previous.airOffsetSumC;
        enthalpyJ = previous.enthalpyJ.clone(); enthalpyJ[body] = enthalpy;
        branches = previous.branches.clone(); branches[body] = branch;
        waiting = previous.waiting.clone(); waiting[body] = wait;
        requests = previous.requests.clone(); requests[body] = request;
        hotMaterials = previous.hotMaterials.clone();
        var transition = template.laws[body].transition(branch);
        hotMaterials[body] = wait || transition != null && transition.progress(enthalpy) > 0;
        minimum = null; materialAnchor = null;
        if (previous.minimumSummary == null) { minimumSummary = null; minimumSummaryAnchor = null; }
        else {
            int explicit = previous.minimumSummary.layout().phaseIndex(template.materialSlots[body]);
            double difference = enthalpy - previous.enthalpyJ[body];
            int pool = previous.minimumSummaryAnchor.pools[body];
            double capacityChange = ((previous.minimumSummaryAnchor.controlWaiting(body) ? 1 : 0) - (wait ? 1 : 0))
                    * template.laws[body].capacityJPerK();
            minimumSummary = explicit < 0
                    ? previous.minimumSummary.withPoolControl(pool, difference, capacityChange)
                    : previous.minimumSummary.withPhaseControl(explicit, enthalpy, branch, wait);
            double controlJ = explicit < 0 ? previous.minimumSummaryAnchor.controlEnergyJ(body) + difference : 0;
            minimumSummaryAnchor = previous.minimumSummaryAnchor.withControl(body, controlJ, enthalpy, branch, wait, request);
        }
    }

    /** Read an installed body only while its arena identity still matches this frame. */
    public boolean readMaterial(int slot, int generation, MaterialSample target) {
        int body = template.bodyIndex(slot);
        if (body < 0 || template.generations[template.cellIndex(slot)] != generation) return false;
        target.setLive(materialEnthalpyJ(body), template.laws[body], materialBranch(body), sampleTick, materialRequest(body));
        return true;
    }
    public boolean readMaterial(int slot, int generation, long incarnation, MaterialSample target) {
        int body = template.bodyIndex(slot);
        return body >= 0 && template.materialIncarnations[body] == incarnation && readMaterial(slot, generation, target);
    }

    public double materialTemperatureAtSlotC(int slot) {
        int body = template.bodyIndex(slot);
        return body < 0 ? Double.NaN : materialTemperatureC(body);
    }

    private RegionalQuerySnapshot(RegionalQuerySnapshot previous, long tick, long topology) {
        minimumInfraredEpoch = previous.minimumInfraredEpoch;
        template = previous.template; sampleTick = tick; topologyGeneration = topology;
        minimum = previous.minimum; materialAnchor = previous.materialAnchor;
        minimumSummary = previous.minimumSummary; minimumSummaryAnchor = previous.minimumSummaryAnchor;
        referenceC = previous.referenceC;
        airC = previous.airC; enthalpyJ = previous.enthalpyJ; branches = previous.branches;
        waiting = previous.waiting; requests = previous.requests; hotMaterials = previous.hotMaterials;
        airMeanC = previous.airMeanC; airMinimumC = previous.airMinimumC;
        airMaximumC = previous.airMaximumC; airOffsetSumC = previous.airOffsetSumC;
    }

    public RegionalQuerySnapshot atTopology(long topology) {
        return topology == topologyGeneration ? this : new RegionalQuerySnapshot(this, sampleTick, topology);
    }

    /** Restore private work after cancellation; the published arrays are never modified. */
    public void copyStateTo(double[] stateC, double[] materialH, byte[] materialBranches,
            boolean[] materialWaiting) {
        System.arraycopy(airC, 0, stateC, 0, airC.length);
        System.arraycopy(enthalpyJ, 0, materialH, 0, enthalpyJ.length);
        System.arraycopy(branches, 0, materialBranches, 0, branches.length);
        System.arraycopy(waiting, 0, materialWaiting, 0, waiting.length);
    }

    /** Private cancellation copy; only its owner can resume work after both columns are restored. */
    public final class Restoration {
        private final double[] state, enthalpy;
        private final byte[] targetBranches;
        private final boolean[] targetWaiting;
        private int air, body;
        private Restoration(double[] state, double[] enthalpy, byte[] branches, boolean[] waiting) {
            this.state = state; this.enthalpy = enthalpy; targetBranches = branches; targetWaiting = waiting;
        }
        public boolean run(long deadline) {
            do {
                if (air < airC.length) {
                    int end = Math.min(airC.length, air + 4096);
                    System.arraycopy(airC, air, state, air, end - air); air = end;
                } else {
                    int end = Math.min(enthalpyJ.length, body + 512);
                    for (; body < end; body++) {
                        enthalpy[body] = enthalpyJ[body]; targetBranches[body] = branches[body];
                        targetWaiting[body] = waiting[body];
                    }
                    if (body == enthalpyJ.length) return true;
                }
            } while (System.nanoTime() < deadline);
            return false;
        }
    }
    public Restoration prepareRestoration(double[] state, double[] enthalpy, byte[] branches, boolean[] waiting) {
        return new Restoration(state, enthalpy, branches, waiting);
    }

    /** Internal borrowing for projection; callers retain this snapshot and only read the array. */
    public double[] borrowedAirCoefficients() { return airC; }

    public boolean minimumQuality() { return minimum != null; }
    public MinimumThermalState minimumState() { return minimum; }
    public MinimumThermalState minimumSummary() { return minimumSummary; }
    public AirFieldLayout airLayout() { return template.air; }
    public double referenceTemperatureC() { return referenceC; }
    public long sampleTick() { return sampleTick; }
    public double componentTemperatureC(int slot, double x, double y, double z) {
        var component = template.air.componentForSlot(slot);
        if (component == null) return Double.NaN;
        double temperature = minimum != null ? minimumTemperatureC(component, x, y, z)
                : referenceC + component.temperatureOffsetC(x, y, z, airC);
        return Double.isFinite(temperature) ? temperature : componentMeanC(airIndex(slot));
    }
    public int airAggregationSpan() { return minimum == null ? template.air.aggregationSpan() : 0; }

    /** Point consumers never expand a minimum state into a detailed coefficient vector. */
    public double airTemperatureC(double x, double y, double z) {
        var component = template.air.componentAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        return component == null ? Double.NaN : minimum != null ? minimumTemperatureC(component, x, y, z)
                : referenceC + component.temperatureOffsetC(x, y, z, airC);
    }

    private double minimumTemperatureC(AirFieldLayout.Component component, double x, double y, double z) {
        var background = minimum.layout().background();
        return background.referenceC + component.temperatureOffsetC(x, y, z, background.borrowedAirOffsetsC())
                + minimum.temperatureOffsetC(x, y, z);
    }

    /** The residency frontier must measure the same perturbation represented by the numerical state. */
    public double componentPerturbationC(int slot, double x, double y, double z, double naturalC) {
        if (minimum != null) return minimum.temperatureOffsetC(x, y, z);
        var component = template.air.componentForSlot(slot);
        if (component == null) return Double.NaN;
        if (minimumSummary == null) return referenceC + component.temperatureOffsetC(x, y, z, airC) - naturalC;
        var background = minimumSummary.layout().background();
        return referenceC - background.referenceC + component.temperatureOffsetC(x, y, z, airC)
                - component.temperatureOffsetC(x, y, z, background.borrowedAirOffsetsC());
    }

    /** Maintenance needs a threshold decision, not repeated material/Air searches and extrema. */
    public boolean activeAtSlot(int slot, double naturalC, double thresholdC) {
        var component = template.air.componentForSlot(slot);
        if (component == null) {
            int body = template.bodyIndex(slot);
            if (body < 0) return false;
            if (minimum == null) {
                if (hotMaterials[body]) return true;
            } else {
                int explicit = minimum.layout().phaseIndex(slot);
                if (explicit >= 0) {
                    if (minimum.phaseWaiting(explicit)) return true;
                } else {
                    var transition = template.laws[body].transition(materialBranch(body));
                    if (transition != null && transition.progress(materialEnthalpyJ(body)) > 0) return true;
                }
            }
            return Math.abs(materialTemperatureC(body) - naturalC) >= thresholdC;
        }
        if (minimum == null && minimumSummary != null) {
            var background = minimumSummary.layout().background();
            double[] values = background.borrowedAirOffsetsC();
            for (int local = 0; local < component.basisCount(); local++) {
                int coefficient = component.coefficientIndex(local);
                if (Math.abs(referenceC - background.referenceC + airC[coefficient] - values[coefficient]) >= thresholdC) return true;
            }
            return false;
        }
        for (int corner = 0; corner < 8; corner++) {
            double residual = Math.abs(componentPerturbationC(slot,
                    component.minX + ((corner & 1) == 0 ? 0 : 4), component.minY + ((corner & 4) == 0 ? 0 : 4),
                    component.minZ + ((corner & 2) == 0 ? 0 : 4), naturalC));
            if (residual >= thresholdC) return true;
        }
        return false;
    }

    public double vertexPerturbationC(AirFieldLayout.Component component, int cell, int corner, double naturalC) {
        if (minimum != null) return minimum.temperatureOffsetC(component.minX + (cell & 3) + (corner & 1),
                component.minY + (cell >>> 4) + (corner >>> 2), component.minZ + (cell >>> 2 & 3) + (corner >>> 1 & 1));
        double value = component.temperatureOffsetAtVertex(cell, corner, airC);
        if (minimumSummary == null) return referenceC + value - naturalC;
        var background = minimumSummary.layout().background();
        return referenceC - background.referenceC + value
                - component.temperatureOffsetAtVertex(cell, corner, background.borrowedAirOffsetsC());
    }

    public double materialEnthalpyJ(int body) {
        if (minimum == null) return enthalpyJ[body];
        int explicit = minimum.layout().phaseIndex(template.materialSlots[body]);
        if (explicit >= 0) return minimum.energyJ(MinimumThermalState.POOLED_NODES + explicit);
        if (materialAnchor.controlWaiting(body)) return materialAnchor.frozenEnthalpyJ(body);
        int pool = materialAnchor.pools[body];
        return materialAnchor.enthalpyJ[body] + materialAnchor.controlEnergyJ(body) + template.laws[body].capacityJPerK()
                * (minimum.nodeTemperatureC(pool) - materialAnchor.initialTemperatureC[pool - MinimumThermalState.AIR_NODES]);
    }

    public byte materialBranch(int body) {
        if (minimum == null) return branches[body];
        int explicit = minimum.layout().phaseIndex(template.materialSlots[body]);
        if (explicit >= 0) return minimum.phaseBranch(explicit);
        byte branch = materialAnchor.controlBranch(body);
        double initial = materialAnchor.enthalpyJ[body] + materialAnchor.controlEnergyJ(body);
        double energy = materialEnthalpyJ(body), direction = Math.signum(energy - initial);
        var law = template.laws[body];
        // Pooled bodies are inspected gradually by the existing bounded phase service.
        if (direction == 0) return branch;
        return law.selectBranch(energy, branch, direction);
    }

    public long materialRequest(int body) { return minimum == null ? requests[body] : materialAnchor.controlRequest(body); }
    public boolean materialWaiting(int body) {
        if (minimum == null) return waiting[body];
        int explicit = minimum.layout().phaseIndex(template.materialSlots[body]);
        return explicit >= 0 ? minimum.phaseWaiting(explicit) : materialAnchor.controlWaiting(body);
    }
    public long materialPosition(int body) { return template.materialPositions[body]; }
    public int materialSlot(int body) { return template.materialSlots[body]; }
    public int materialStateId(int body) { return template.materialStateIds[body]; }
    public long materialIncarnation(int body) { return template.materialIncarnations[body]; }
    public int materialCount() { return template.materialSlots.length; }
    public PageIndex checkpointPages() { return template.checkpointPages; }
    public long checkpointBrickMask(int page) { return template.pageBrickMasks[page]; }
    /** Bound the writer's arrays and metadata before borrowing a field for saving. */
    public long checkpointBytes(long chunk) {
        long bytes = 8192;
        var pages = template.checkpointPages;
        for (int page = 0; page < pages.size(); page++) {
            long section = pages.section(page);
            if (net.minecraft.world.level.ChunkPos.asLong(net.minecraft.core.SectionPos.x(section),
                    net.minecraft.core.SectionPos.z(section)) != chunk) continue;
            for (int index = 0; index < pages.airCount(page); index++) {
                var component = template.air.component(pages.airComponent(page, index));
                bytes += minimumQuality() ? 512 : 4608L + 32L * component.basisCount();
            }
            bytes += 256L * pages.materialCount(page);
        }
        return bytes;
    }

    public int materialBodyIndex(int slot, long incarnation) {
        int body = template.bodyIndex(slot);
        return body >= 0 && template.materialIncarnations[body] == incarnation ? body : -1;
    }
    public MaterialThermalLaw materialLaw(int body) { return template.laws[body]; }
    public MaterialAnchor materialAnchor(int[] pools, double[] initialTemperatureC) {
        return new MaterialAnchor(enthalpyJ, branches, waiting, requests, pools, initialTemperatureC);
    }

    /** Material anchors retain no old detailed Air state or numerical workspace. */
    public static final class MaterialAnchor {
        private final double[] enthalpyJ, initialTemperatureC;
        private final byte[] branches;
        private final boolean[] waiting;
        private final long[] requests;
        private final int[] pools;
        private final ControlBlock[] controls;
        private Object[] payload;

        private static final class ControlBlock {
            final double[] energy = new double[256];
            final double[] frozenH = new double[256];
            final byte[] branches = new byte[256];
            final long[] requests = new long[256];
            final boolean[] present = new boolean[256], waiting = new boolean[256];
            ControlBlock() { }
            ControlBlock(ControlBlock old) {
                System.arraycopy(old.energy, 0, energy, 0, 256); System.arraycopy(old.branches, 0, branches, 0, 256);
                System.arraycopy(old.requests, 0, requests, 0, 256); System.arraycopy(old.present, 0, present, 0, 256);
                System.arraycopy(old.frozenH, 0, frozenH, 0, 256); System.arraycopy(old.waiting, 0, waiting, 0, 256);
            }
        }

        public MaterialAnchor(double[] enthalpyJ, byte[] branches, boolean[] waiting, long[] requests,
                int[] pools, double[] initialTemperatureC) {
            this.enthalpyJ = enthalpyJ; this.branches = branches; this.requests = requests;
            this.waiting = waiting;
            this.pools = pools; this.initialTemperatureC = initialTemperatureC;
            controls = null;
        }
        private MaterialAnchor(MaterialAnchor old, int body, double controlJ, double enthalpy, byte branch, boolean wait, long request) {
            enthalpyJ = old.enthalpyJ; branches = old.branches; requests = old.requests;
            waiting = old.waiting;
            pools = old.pools; initialTemperatureC = old.initialTemperatureC;
            controls = old.controls == null ? new ControlBlock[(enthalpyJ.length + 255) >>> 8] : old.controls.clone();
            int page = body >>> 8, offset = body & 255;
            var previous = controls[page]; var next = previous == null ? new ControlBlock() : new ControlBlock(previous);
            controls[page] = next; next.present[offset] = true;
            next.energy[offset] = controlJ; next.branches[offset] = branch; next.requests[offset] = request;
            next.waiting[offset] = wait; next.frozenH[offset] = enthalpy;
        }
        private ControlBlock block(int body) { return controls == null ? null : controls[body >>> 8]; }
        private double controlEnergyJ(int body) { var block = block(body); return block == null ? 0 : block.energy[body & 255]; }
        private byte controlBranch(int body) {
            var block = block(body); int offset = body & 255;
            return block == null || !block.present[offset] ? branches[body] : block.branches[offset];
        }
        private long controlRequest(int body) {
            var block = block(body); int offset = body & 255;
            return block == null || !block.present[offset] ? requests[body] : block.requests[offset];
        }
        private boolean controlWaiting(int body) {
            var block = block(body); int offset = body & 255;
            return block == null || !block.present[offset] ? waiting[body] : block.waiting[offset];
        }
        private double frozenEnthalpyJ(int body) {
            var block = block(body); int offset = body & 255;
            return block == null || !block.present[offset] ? enthalpyJ[body] : block.frozenH[offset];
        }
        private MaterialAnchor withControl(int body, double controlJ, double enthalpy, byte branch, boolean wait, long request) {
            if (controlJ == controlEnergyJ(body) && branch == controlBranch(body) && wait == controlWaiting(body)
                    && request == controlRequest(body) && (!wait || enthalpy == frozenEnthalpyJ(body))) return this;
            return new MaterialAnchor(this, body, controlJ, enthalpy, branch, wait, request);
        }

        Object[] payloadArrays() {
            if (payload != null) return payload;
            var arrays = new java.util.ArrayList<Object>();
            java.util.Collections.addAll(arrays, enthalpyJ, initialTemperatureC, branches, waiting, requests, pools);
            if (controls != null) {
                arrays.add(controls);
                for (var block : controls) if (block != null)
                    java.util.Collections.addAll(arrays, block.energy, block.frozenH, block.branches, block.waiting, block.requests, block.present);
            }
            return payload = arrays.toArray();
        }
    }

    /** Adopt a preadmitted bounded state; geometry is shared, full numerical arrays are not retained. */
    public RegionalQuerySnapshot(Template template, MinimumThermalState state, MaterialAnchor anchor,
            long tick, long topology, double referenceC) {
        this.template = template; minimum = state; materialAnchor = anchor;
        minimumSummary = null; minimumSummaryAnchor = null;
        sampleTick = tick; topologyGeneration = topology; this.referenceC = referenceC;
        airC = enthalpyJ = airMeanC = airMinimumC = airMaximumC = airOffsetSumC = new double[0];
        branches = new byte[0]; waiting = hotMaterials = new boolean[0]; requests = new long[0];
    }

    private RegionalQuerySnapshot(RegionalQuerySnapshot field, MinimumThermalState summary, MaterialAnchor anchor) {
        minimumInfraredEpoch = field.minimumInfraredEpoch;
        template = field.template; sampleTick = field.sampleTick; topologyGeneration = field.topologyGeneration;
        referenceC = field.referenceC; minimum = field.minimum; materialAnchor = field.materialAnchor;
        minimumSummary = summary; minimumSummaryAnchor = anchor;
        airC = field.airC; enthalpyJ = field.enthalpyJ; branches = field.branches;
        waiting = field.waiting; requests = field.requests; hotMaterials = field.hotMaterials;
        airMeanC = field.airMeanC; airMinimumC = field.airMinimumC; airMaximumC = field.airMaximumC;
        airOffsetSumC = field.airOffsetSumC; changedBrickMasks = field.changedBrickMasks;
        infraredPreviousId = field.infraredPreviousId;
    }

    public RegionalQuerySnapshot withMinimumSummary(MinimumThermalState summary, MaterialAnchor anchor) {
        return new RegionalQuerySnapshot(this, summary, anchor);
    }

    public RegionalQuerySnapshot activatePreparedMinimum() {
        return minimum != null || minimumSummary == null ? this
                : new RegionalQuerySnapshot(template, minimumSummary, minimumSummaryAnchor,
                        sampleTick, topologyGeneration, referenceC);
    }
    public RegionalQuerySnapshot activateMinimum(MinimumThermalState.Layout transport) {
        return new RegionalQuerySnapshot(template, minimumSummary.withTransport(transport), minimumSummaryAnchor,
                sampleTick, topologyGeneration, referenceC);
    }

    public RegionalQuerySnapshot advanceMinimum(MinimumThermalState state, long tick, long topology) {
        return new RegionalQuerySnapshot(template, state, materialAnchor, tick, topology, referenceC);
    }

    int airIndex(int slot) {
        int low = 0, high = template.air.componentCount() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int current = template.air.component(middle).arenaSlot;
            if (current == slot) return middle;
            if (current < slot) low = middle + 1;
            else high = middle - 1;
        }
        return -1;
    }

    double temperatureC(int slot) {
        int body = template.bodyIndex(slot);
        if (body >= 0) return materialTemperatureC(body);
        int index = airIndex(slot);
        return index < 0 ? Double.NaN : componentMeanC(index);
    }

    double materialTemperatureC(int body) {
        return template.laws[body].temperatureC(materialEnthalpyJ(body), materialBranch(body));
    }

    public double componentMeanC(int index) {
        if (minimum == null) return airMeanC[index];
        var component = template.air.component(index);
        return minimumTemperatureC(component, component.minX + 2, component.minY + 2, component.minZ + 2);
    }
    public Object geometryIdentity() { return template; }
    public AirFieldLayout.Component componentForSlot(int slot) { return template.air.componentForSlot(slot); }
    public double minimumAtSlotC(int slot) { int index = airIndex(slot); return index < 0 ? materialTemperatureAtSlotC(slot) : componentMinimumC(index); }
    public double maximumAtSlotC(int slot) { int index = airIndex(slot); return index < 0 ? materialTemperatureAtSlotC(slot) : componentMaximumC(index); }
    public double componentMinimumC(int index) { return minimum == null ? airMinimumC[index] : componentExtremeC(index, false); }
    public double componentMaximumC(int index) { return minimum == null ? airMaximumC[index] : componentExtremeC(index, true); }
    private double componentExtremeC(int index, boolean maximum) {
        var component = template.air.component(index);
        double value = maximum ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            double sample = minimumTemperatureC(component, component.minX + ((corner & 1) == 0 ? 0 : 4),
                    component.minY + ((corner & 4) == 0 ? 0 : 4), component.minZ + ((corner & 2) == 0 ? 0 : 4));
            value = maximum ? Math.max(value, sample) : Math.min(value, sample);
        }
        return value;
    }
    boolean hasPreparedInfraredDiff(RegionalQuerySnapshot previous) {
        return changedBrickMasks != null && infraredPreviousId == (previous == null ? 0 : previous.frameId);
    }
    int changedPageCount() { return changedBrickMasks == null ? 0 : changedBrickMasks.length; }
    int changedPage(int index) { return template.distinctPages[index]; }
    long changedBricks(int index) { return changedBrickMasks[index]; }
    public void recordGeometryBacking(AirBackingMeter meter) {
        template.air.recordBacking(meter);
        if (minimum != null) minimum.recordGeometryBacking(meter);
        if (minimumSummary != null) minimumSummary.recordGeometryBacking(meter);
    }

    void recordPayloadBacking(AirBackingMeter meter) {
        if (!meter.objectOnce(this, 192)) return;
        // Transport/layout storage has its own geometry owner. This scan measures
        // the same payload families that readers retain, without counting the graph twice.
        for (Object array : payloadArrays()) meter.array(array);
    }
}
