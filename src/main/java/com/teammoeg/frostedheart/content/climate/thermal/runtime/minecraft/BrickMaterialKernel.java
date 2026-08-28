/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalExchangeKernel;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment;

import java.util.Arrays;

/** Reusable primitive material/phase contact and operation kernel. */
final class BrickMaterialKernel {
    private static final double PATCH_AREA = 1.0D / 16.0D;
    private static final ConservativeAirGeometry.Face[] FACES =
            ConservativeAirGeometry.Face.values();

    private final ThermalCellArena arena;
    private final ThermalSignatureCatalog signatures;
    private final MaterialBoundaryRegistry materials;
    private final int[] surfaceBlockX = new int[64];
    private final int[] surfaceBlockY = new int[64];
    private final int[] surfaceBlockZ = new int[64];
    private final int[] surfaceProfileId = new int[64];
    private final int[] surfacePatchCount = new int[64];
    private final int[] surfacePoleOrdinal = new int[64];
    private final int[] deepPoleOrdinal = new int[64];
    private final int[] phaseProfileId = new int[64];
    private final long[] phaseCandidateMask = new long[64];
    private final int[] phaseReservoirOrdinal = new int[64];
    private final PrimitiveTopologyScratch.OwnerLongInt surfaceContacts =
            new PrimitiveTopologyScratch.OwnerLongInt();
    private final PrimitiveTopologyScratch.OwnerLongInt phaseContacts =
            new PrimitiveTopologyScratch.OwnerLongInt();
    private final PrimitiveTopologyScratch.LongPairDouble bridges =
            new PrimitiveTopologyScratch.LongPairDouble();
    private final PrimitiveTopologyScratch.LongPairDouble pairs =
            new PrimitiveTopologyScratch.LongPairDouble();
    private int surfaceCount;
    private int phaseCount;
    private int[] fixedCell = new int[16];
    private double[] fixedTemperature = new double[16];
    private double[] fixedConductance = new double[16];
    private int fixedCount;
    private int[] phaseAir = new int[16];
    private int[] phaseReservoir = new int[16];
    private double[] phaseConductance = new double[16];
    private int phaseOperationCount;

    BrickMaterialKernel(
            ThermalCellArena arena,
            ThermalSignatureCatalog signatures,
            MaterialBoundaryRegistry materials
    ) {
        this.arena = arena;
        this.signatures = signatures;
        this.materials = materials;
    }

    boolean compileLayout(
            WorkerPageStore.PageState page,
            int minX,
            int minY,
            int minZ,
            int[] signatureIds,
            TopologyView view,
            ThermalCellArena.BrickCellLayout cells
    ) {
        resetLayout();
        for (int block = 0; block < 64; block++) {
            ResolvedThermalSignature signature =
                    signatures.signature(signatureIds[block]);
            int profileId = signature.materialProfileId();
            int patternId = signature.materialContactPatternId();
            if (profileId == 0 && patternId == 0) {
                continue;
            }
            MaterialBoundaryRegistry.Profile profile =
                    materials.profileOrNull(profileId);
            MaterialBoundaryRegistry.ContactPattern pattern =
                    materials.contactPatternOrNull(patternId);
            if (profile == null || pattern == null
                    || (pattern.materialMicrocellMask()
                    & signatures.airMask(signatureIds[block])) != 0L) {
                return false;
            }
            int blockX = minX + (block & 3);
            int blockZ = minZ + (block >>> 2 & 3);
            int blockY = minY + (block >>> 4 & 3);
            if (profile.model()
                    == MaterialBoundaryRegistry.Model.STATELESS_CONDUCTANCE) {
                if (pattern.materialMicrocellMask() != -1L) {
                    return false;
                }
                compileBridges(blockX, blockY, blockZ, profile, view);
                continue;
            }
            int surface = -1;
            int phase = -1;
            for (int microY = 0; microY < 4; microY++) {
                for (int microZ = 0; microZ < 4; microZ++) {
                    for (int microX = 0; microX < 4; microX++) {
                        if (!pattern.contains(microX, microY, microZ)) {
                            continue;
                        }
                        for (ConservativeAirGeometry.Face face : FACES) {
                            long air = adjacentAir(
                                    blockX, blockY, blockZ,
                                    microX, microY, microZ, face, view);
                            if (air == PackedAirReference.NONE) {
                                continue;
                            }
                            if (profile.model()
                                    == MaterialBoundaryRegistry.Model.PHASE_RESERVOIR) {
                                if (phase < 0) {
                                    phase = phase(profile.id());
                                }
                                phaseCandidateMask[phase] |= 1L << block;
                                phaseContacts.add(phase, air, 1);
                            } else {
                                if (surface < 0) {
                                    surface = surface(
                                            blockX, blockY, blockZ, profile.id());
                                }
                                surfacePatchCount[surface]++;
                                surfaceContacts.add(surface, air, 1);
                            }
                        }
                    }
                }
            }
        }
        int materialOrdinal = 0;
        for (int surface = 0; surface < surfaceCount; surface++) {
            MaterialBoundaryRegistry.Profile profile =
                    materials.profileOrNull(surfaceProfileId[surface]);
            double area = surfacePatchCount[surface] * PATCH_AREA;
            surfacePoleOrdinal[surface] = materialOrdinal++;
            cells.addMaterialPole(
                    surfaceBlockX[surface], surfaceBlockY[surface],
                    surfaceBlockZ[surface], profile.id(),
                    ThermalCellArena.MaterialPoleDepth.SURFACE,
                    profile.surfaceCapacityJPerK() * area,
                    profile.poleInitialTemperatureC(
                            surfaceBlockY[surface],
                            view.naturalTemperature(page)));
            if (profile.model() == MaterialBoundaryRegistry.Model.NATURAL_ROCK
                    && profile.deepCapacityJPerK() > 0.0D) {
                deepPoleOrdinal[surface] = materialOrdinal++;
                cells.addMaterialPole(
                        surfaceBlockX[surface], surfaceBlockY[surface],
                        surfaceBlockZ[surface], profile.id(),
                        ThermalCellArena.MaterialPoleDepth.DEEP,
                        profile.deepCapacityJPerK() * area,
                        profile.poleInitialTemperatureC(
                                surfaceBlockY[surface],
                                view.naturalTemperature(page)));
            }
        }
        for (int phase = 0; phase < phaseCount; phase++) {
            MaterialBoundaryRegistry.Profile profile =
                    materials.profileOrNull(phaseProfileId[phase]);
            phaseReservoirOrdinal[phase] = phase;
            cells.addPhaseReservoir(
                    minX, minY, minZ, profile.id(),
                    phaseCandidateMask[phase],
                    profile.transitionTemperatureC(),
                    profile.transitionEnergyJPerUnit());
        }
        return true;
    }

    WorkerBrickTopology.MaterialContacts freezeContacts() {
        GroupedContacts surfaces = group(surfaceContacts, surfaceCount);
        GroupedContacts phases = group(phaseContacts, phaseCount);
        long[] bridgeFirst = new long[bridges.size()];
        long[] bridgeSecond = new long[bridges.size()];
        double[] bridgeConductance = new double[bridges.size()];
        for (int index = 0; index < bridges.size(); index++) {
            bridgeFirst[index] = bridges.first(index);
            bridgeSecond[index] = bridges.second(index);
            bridgeConductance[index] = bridges.value(index);
        }
        double[] area = new double[surfaceCount];
        for (int index = 0; index < surfaceCount; index++) {
            area[index] = surfacePatchCount[index] * PATCH_AREA;
        }
        return new WorkerBrickTopology.MaterialContacts(
                Arrays.copyOf(surfaceBlockX, surfaceCount),
                Arrays.copyOf(surfaceBlockY, surfaceCount),
                Arrays.copyOf(surfaceBlockZ, surfaceCount),
                Arrays.copyOf(surfaceProfileId, surfaceCount), area,
                Arrays.copyOf(surfacePoleOrdinal, surfaceCount),
                Arrays.copyOf(deepPoleOrdinal, surfaceCount),
                surfaces.start, surfaces.count,
                surfaces.reference, surfaces.patches,
                Arrays.copyOf(phaseProfileId, phaseCount),
                Arrays.copyOf(phaseCandidateMask, phaseCount),
                Arrays.copyOf(phaseReservoirOrdinal, phaseCount),
                phases.start, phases.count, phases.reference, phases.patches,
                bridgeFirst, bridgeSecond, bridgeConductance);
    }

    WorkerBrickTopology.MaterialPoles materialPoles(
            WorkerBrickTopology.MaterialContacts contacts,
            ThermalCellArena.BrickAllocation allocation
    ) {
        int[] slots = allocation.materialPoleSlots();
        int[] x = new int[slots.length];
        int[] y = new int[slots.length];
        int[] z = new int[slots.length];
        int[] profile = new int[slots.length];
        byte[] depth = new byte[slots.length];
        for (int surface = 0; surface < contacts.surfaceCount(); surface++) {
            int ordinal = contacts.surfacePoleOrdinal()[surface];
            x[ordinal] = contacts.surfaceBlockX()[surface];
            y[ordinal] = contacts.surfaceBlockY()[surface];
            z[ordinal] = contacts.surfaceBlockZ()[surface];
            profile[ordinal] = contacts.surfaceProfileId()[surface];
            depth[ordinal] =
                    (byte) ThermalCellArena.MaterialPoleDepth.SURFACE.ordinal();
            int deep = contacts.deepPoleOrdinal()[surface];
            if (deep >= 0) {
                x[deep] = x[ordinal];
                y[deep] = y[ordinal];
                z[deep] = z[ordinal];
                profile[deep] = profile[ordinal];
                depth[deep] =
                        (byte) ThermalCellArena.MaterialPoleDepth.DEEP.ordinal();
            }
        }
        return new WorkerBrickTopology.MaterialPoles(
                x, y, z, profile, depth, slots);
    }

    WorkerBrickTopology.PhaseReservoirs phaseReservoirs(
            WorkerBrickTopology.MaterialContacts contacts,
            ThermalCellArena.BrickAllocation allocation,
            int minX,
            int minY,
            int minZ
    ) {
        int[] slots = allocation.phaseReservoirSlots();
        int[] x = new int[slots.length];
        int[] y = new int[slots.length];
        int[] z = new int[slots.length];
        int[] profile = new int[slots.length];
        for (int phase = 0; phase < contacts.phaseCount(); phase++) {
            int ordinal = contacts.phaseReservoirOrdinal()[phase];
            x[ordinal] = minX;
            y[ordinal] = minY;
            z[ordinal] = minZ;
            profile[ordinal] = contacts.phaseProfileId()[phase];
        }
        return new WorkerBrickTopology.PhaseReservoirs(
                x, y, z, profile, slots);
    }

    PagePublication.PhaseCandidates phaseCandidates(
            WorkerBrickTopology.MaterialContacts contacts
    ) {
        return contacts.phaseCount() == 0
                ? PagePublication.PhaseCandidates.EMPTY
                : PagePublication.PhaseCandidates.owned(
                        contacts.phaseProfileId(),
                        contacts.phaseCandidateMask());
    }

    Operations compileOperations(
            WorkerBrickTopology.MaterialContacts contacts,
            WorkerBrickTopology owner,
            TopologyView view
    ) {
        pairs.reset();
        fixedCount = 0;
        phaseOperationCount = 0;
        boolean resolved = true;
        for (int bridge = 0; bridge < contacts.bridgeCount(); bridge++) {
            int first = view.resolveAirSlot(
                    contacts.bridgeNegativeAirReference()[bridge]);
            int second = view.resolveAirSlot(
                    contacts.bridgePositiveAirReference()[bridge]);
            if (first < 0 || second < 0) {
                resolved = false;
            } else if (first != second) {
                addPair(
                        first, second,
                        contacts.bridgeConductanceWPerK()[bridge]);
            }
        }
        for (int surface = 0; surface < contacts.surfaceCount(); surface++) {
            int surfaceSlot = owner.materialPoles.slot()[
                    contacts.surfacePoleOrdinal()[surface]];
            MaterialBoundaryRegistry.Profile profile =
                    materials.profileOrNull(
                            contacts.surfaceProfileId()[surface]);
            int start = contacts.surfaceContactStart()[surface];
            int end = start + contacts.surfaceContactCount()[surface];
            for (int contact = start; contact < end; contact++) {
                int air = view.resolveAirSlot(
                        contacts.surfaceContactAirReference()[contact]);
                if (air < 0) {
                    resolved = false;
                } else {
                    addPair(
                            air, surfaceSlot,
                            profile.faceConductanceWPerK()
                                    * contacts.surfaceContactPatches()[contact]
                                    / 16.0D);
                }
            }
            if (profile.model()
                    != MaterialBoundaryRegistry.Model.NATURAL_ROCK) {
                continue;
            }
            int deep = contacts.deepPoleOrdinal()[surface];
            double area = contacts.surfaceArea()[surface];
            double natural = profile.naturalTemperatureC(
                    contacts.surfaceBlockY()[surface]);
            if (deep < 0) {
                addFixed(
                        surfaceSlot, natural,
                        profile.deepConductanceWPerK() * area);
            } else {
                int deepSlot = owner.materialPoles.slot()[deep];
                addPair(
                        surfaceSlot, deepSlot,
                        profile.deepConductanceWPerK() * area);
                addFixed(
                        deepSlot, natural,
                        profile.naturalConductanceWPerK() * area);
            }
        }
        for (int phase = 0; phase < contacts.phaseCount(); phase++) {
            int reservoir = owner.phaseReservoirs.slot()[
                    contacts.phaseReservoirOrdinal()[phase]];
            MaterialBoundaryRegistry.Profile profile =
                    materials.profileOrNull(contacts.phaseProfileId()[phase]);
            int start = contacts.phaseContactStart()[phase];
            int end = start + contacts.phaseContactCount()[phase];
            for (int contact = start; contact < end; contact++) {
                int air = view.resolveAirSlot(
                        contacts.phaseContactAirReference()[contact]);
                if (air < 0) {
                    resolved = false;
                } else {
                    addPhase(
                            air, reservoir,
                            profile.faceConductanceWPerK()
                                    * contacts.phaseContactPatches()[contact]
                                    / 16.0D);
                }
            }
        }
        return new Operations(
                freezePairs(), freezeFixed(), freezePhases(), resolved);
    }

    private void resetLayout() {
        surfaceCount = 0;
        phaseCount = 0;
        surfaceContacts.reset();
        phaseContacts.reset();
        bridges.reset();
        Arrays.fill(surfacePoleOrdinal, -1);
        Arrays.fill(deepPoleOrdinal, -1);
        Arrays.fill(phaseReservoirOrdinal, -1);
    }

    private int surface(int x, int y, int z, int profileId) {
        int index = surfaceCount++;
        surfaceBlockX[index] = x;
        surfaceBlockY[index] = y;
        surfaceBlockZ[index] = z;
        surfaceProfileId[index] = profileId;
        surfacePatchCount[index] = 0;
        return index;
    }

    private int phase(int profileId) {
        for (int index = 0; index < phaseCount; index++) {
            if (phaseProfileId[index] == profileId) {
                return index;
            }
        }
        int index = phaseCount++;
        phaseProfileId[index] = profileId;
        phaseCandidateMask[index] = 0L;
        return index;
    }

    private void compileBridges(
            int x,
            int y,
            int z,
            MaterialBoundaryRegistry.Profile profile,
            TopologyView view
    ) {
        for (int axis = 0; axis < 3; axis++) {
            for (int v = 0; v < 4; v++) {
                for (int u = 0; u < 4; u++) {
                    long negative;
                    long positive;
                    if (axis == 0) {
                        negative = view.airReference(x - 1, y, z, 3, v, u);
                        positive = view.airReference(x + 1, y, z, 0, v, u);
                    } else if (axis == 1) {
                        negative = view.airReference(x, y - 1, z, u, 3, v);
                        positive = view.airReference(x, y + 1, z, u, 0, v);
                    } else {
                        negative = view.airReference(x, y, z - 1, u, v, 3);
                        positive = view.airReference(x, y, z + 1, u, v, 0);
                    }
                    if (negative != PackedAirReference.NONE
                            && positive != PackedAirReference.NONE) {
                        bridges.add(
                                negative, positive,
                                profile.faceConductanceWPerK() / 16.0D,
                                false);
                    }
                }
            }
        }
    }

    private static long adjacentAir(
            int blockX,
            int blockY,
            int blockZ,
            int microX,
            int microY,
            int microZ,
            ConservativeAirGeometry.Face face,
            TopologyView view
    ) {
        switch (face) {
            case NEGATIVE_X -> microX--;
            case POSITIVE_X -> microX++;
            case NEGATIVE_Y -> microY--;
            case POSITIVE_Y -> microY++;
            case NEGATIVE_Z -> microZ--;
            case POSITIVE_Z -> microZ++;
        }
        if (microX < 0) { blockX--; microX = 3; }
        else if (microX == 4) { blockX++; microX = 0; }
        if (microY < 0) { blockY--; microY = 3; }
        else if (microY == 4) { blockY++; microY = 0; }
        if (microZ < 0) { blockZ--; microZ = 3; }
        else if (microZ == 4) { blockZ++; microZ = 0; }
        return view.airReference(
                blockX, blockY, blockZ, microX, microY, microZ);
    }

    private static GroupedContacts group(
            PrimitiveTopologyScratch.OwnerLongInt contacts,
            int owners
    ) {
        int[] count = new int[owners];
        for (int index = 0; index < contacts.size(); index++) {
            count[contacts.owner(index)]++;
        }
        int[] start = new int[owners];
        int total = 0;
        for (int owner = 0; owner < owners; owner++) {
            start[owner] = total;
            total += count[owner];
        }
        int[] cursor = start.clone();
        long[] reference = new long[total];
        int[] patches = new int[total];
        for (int index = 0; index < contacts.size(); index++) {
            int target = cursor[contacts.owner(index)]++;
            reference[target] = contacts.key(index);
            patches[target] = contacts.value(index);
        }
        return new GroupedContacts(start, count, reference, patches);
    }

    private void addPair(int first, int second, double conductance) {
        if (first > second) {
            int swap = first; first = second; second = swap;
        }
        if (first != second) {
            pairs.add(first, second, conductance, false);
        }
    }

    private void addFixed(int cell, double temperature, double conductance) {
        if (fixedCount == fixedCell.length) {
            int capacity = grow(fixedCell.length, fixedCount + 1);
            fixedCell = Arrays.copyOf(fixedCell, capacity);
            fixedTemperature = Arrays.copyOf(fixedTemperature, capacity);
            fixedConductance = Arrays.copyOf(fixedConductance, capacity);
        }
        fixedCell[fixedCount] = cell;
        fixedTemperature[fixedCount] = temperature;
        fixedConductance[fixedCount++] = conductance;
    }

    private void addPhase(int air, int reservoir, double conductance) {
        if (phaseOperationCount == phaseAir.length) {
            int capacity = grow(phaseAir.length, phaseOperationCount + 1);
            phaseAir = Arrays.copyOf(phaseAir, capacity);
            phaseReservoir = Arrays.copyOf(phaseReservoir, capacity);
            phaseConductance = Arrays.copyOf(phaseConductance, capacity);
        }
        phaseAir[phaseOperationCount] = air;
        phaseReservoir[phaseOperationCount] = reservoir;
        phaseConductance[phaseOperationCount++] = conductance;
    }

    private ThermalFragment.MaterialContributions freezePairs() {
        int count = pairs.size();
        if (count == 0) {
            return ThermalFragment.MaterialContributions.EMPTY;
        }
        int[] first = new int[count];
        int[] second = new int[count];
        int[] firstGeneration = new int[count];
        int[] secondGeneration = new int[count];
        double[] conductance = new double[count];
        for (int index = 0; index < count; index++) {
            first[index] = (int) pairs.first(index);
            second[index] = (int) pairs.second(index);
            firstGeneration[index] = arena.lifecycleGeneration(first[index]);
            secondGeneration[index] = arena.lifecycleGeneration(second[index]);
            conductance[index] = pairs.value(index);
        }
        return new ThermalFragment.MaterialContributions(
                first, second, firstGeneration, secondGeneration, conductance);
    }

    private ThermalFragment.FixedBoundaries freezeFixed() {
        if (fixedCount == 0) {
            return ThermalFragment.FixedBoundaries.EMPTY;
        }
        int[] cell = Arrays.copyOf(fixedCell, fixedCount);
        int[] generation = new int[fixedCount];
        double[] conductance = Arrays.copyOf(fixedConductance, fixedCount);
        double[] coefficient = new double[fixedCount];
        for (int index = 0; index < fixedCount; index++) {
            generation[index] = arena.lifecycleGeneration(cell[index]);
            coefficient[index] =
                    ThermalExchangeKernel.compileBoundaryCoefficientJPerK(
                            arena.capacityJPerK(cell[index]),
                            conductance[index], 1.0D);
        }
        return new ThermalFragment.FixedBoundaries(
                cell, generation,
                Arrays.copyOf(fixedTemperature, fixedCount),
                conductance, coefficient);
    }

    private ThermalFragment.PhaseContacts freezePhases() {
        if (phaseOperationCount == 0) {
            return ThermalFragment.PhaseContacts.EMPTY;
        }
        int[] air = Arrays.copyOf(phaseAir, phaseOperationCount);
        int[] reservoir = Arrays.copyOf(
                phaseReservoir, phaseOperationCount);
        int[] airGeneration = new int[phaseOperationCount];
        int[] reservoirGeneration = new int[phaseOperationCount];
        for (int index = 0; index < phaseOperationCount; index++) {
            airGeneration[index] = arena.lifecycleGeneration(air[index]);
            reservoirGeneration[index] =
                    arena.lifecycleGeneration(reservoir[index]);
        }
        return new ThermalFragment.PhaseContacts(
                air, airGeneration, reservoir, reservoirGeneration,
                Arrays.copyOf(phaseConductance, phaseOperationCount));
    }

    private static int grow(int current, int required) {
        int capacity = Math.max(1, current);
        while (capacity < required) {
            capacity = Math.addExact(
                    capacity, Math.max(8, capacity >>> 1));
        }
        return capacity;
    }

    record Operations(
            ThermalFragment.MaterialContributions pairs,
            ThermalFragment.FixedBoundaries boundaries,
            ThermalFragment.PhaseContacts phases,
            boolean resolved
    ) {
    }

    private record GroupedContacts(
            int[] start,
            int[] count,
            long[] reference,
            int[] patches
    ) {
    }
}
