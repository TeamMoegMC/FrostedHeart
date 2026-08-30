/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalBrickCellLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment;

import java.util.Arrays;

/** Reusable primitive material/phase contact and operation kernel. */
final class BrickMaterialKernel {
    private static final double PATCH_AREA = 1.0D / 16.0D;
    private static final ConservativeAirGeometry.Face[] FACES =
            ConservativeAirGeometry.Face.values();

    private final ThermalSignatureCatalog signatures;
    private final MaterialBoundaryRegistry materials;
    private final int[] surfaceBlockX = new int[64];
    private final int[] surfaceBlockY = new int[64];
    private final int[] surfaceBlockZ = new int[64];
    private final int[] surfaceProfileId = new int[64];
    private final int[] surfacePatchCount = new int[64];
    private final int[] surfacePoleOrdinal = new int[64];
    private final int[] phaseProfileId = new int[64];
    private final long[] phaseCandidateMask = new long[64];
    private final int[] phaseReservoirOrdinal = new int[64];
    private final PrimitiveTopologyScratch.OwnerLongInt surfaceContacts =
            new PrimitiveTopologyScratch.OwnerLongInt();
    private final PrimitiveTopologyScratch.OwnerLongInt phaseContacts =
            new PrimitiveTopologyScratch.OwnerLongInt();
    private final PrimitiveTopologyScratch.LongPairDouble pairs =
            new PrimitiveTopologyScratch.LongPairDouble();
    private final WorkerBrickTopology.MaterialContacts.Builder contactBuilder =
            new WorkerBrickTopology.MaterialContacts.Builder();
    private int surfaceCount;
    private int phaseCount;
    private int[] phaseAir = new int[16];
    private int[] phaseReservoir = new int[16];
    private double[] phaseConductance = new double[16];
    private int phaseOperationCount;

    BrickMaterialKernel(
            ThermalSignatureCatalog signatures,
            MaterialBoundaryRegistry materials
    ) {
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
            ThermalBrickCellLayout cells
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
                    surfaceBlockZ[surface],
                    profile.surfaceCapacityJPerK() * area,
                    profile.poleInitialTemperatureC(
                            view.naturalTemperature(page)));
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
        return contactBuilder.surfaces(
                        Arrays.copyOf(surfaceBlockX, surfaceCount),
                        Arrays.copyOf(surfaceBlockY, surfaceCount),
                        Arrays.copyOf(surfaceBlockZ, surfaceCount),
                        Arrays.copyOf(surfaceProfileId, surfaceCount),
                        Arrays.copyOf(surfacePoleOrdinal, surfaceCount),
                        surfaces.start, surfaces.count,
                        surfaces.reference, surfaces.patches)
                .phases(
                        Arrays.copyOf(phaseProfileId, phaseCount),
                        Arrays.copyOf(phaseCandidateMask, phaseCount),
                        Arrays.copyOf(phaseReservoirOrdinal, phaseCount),
                        phases.start, phases.count,
                        phases.reference, phases.patches)
                .build();
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
        for (int surface = 0; surface < contacts.surfaceCount(); surface++) {
            int ordinal = contacts.surfacePoleOrdinal()[surface];
            x[ordinal] = contacts.surfaceBlockX()[surface];
            y[ordinal] = contacts.surfaceBlockY()[surface];
            z[ordinal] = contacts.surfaceBlockZ()[surface];
            profile[ordinal] = contacts.surfaceProfileId()[surface];
        }
        return new WorkerBrickTopology.MaterialPoles(
                x, y, z, profile, slots);
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
        phaseOperationCount = 0;
        boolean resolved = true;
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
        return new Operations(freezePairs(), freezePhases(), resolved);
    }

    private void resetLayout() {
        surfaceCount = 0;
        phaseCount = 0;
        surfaceContacts.reset();
        phaseContacts.reset();
        Arrays.fill(surfacePoleOrdinal, -1);
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
            pairs.add(first, second, conductance);
        }
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
        double[] conductance = new double[count];
        for (int index = 0; index < count; index++) {
            first[index] = (int) pairs.first(index);
            second[index] = (int) pairs.second(index);
            conductance[index] = pairs.value(index);
        }
        return new ThermalFragment.MaterialContributions(
                first, second, conductance);
    }

    private ThermalFragment.PhaseContacts freezePhases() {
        if (phaseOperationCount == 0) {
            return ThermalFragment.PhaseContacts.EMPTY;
        }
        int[] air = Arrays.copyOf(phaseAir, phaseOperationCount);
        int[] reservoir = Arrays.copyOf(
                phaseReservoir, phaseOperationCount);
        return new ThermalFragment.PhaseContacts(
                air, reservoir,
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
