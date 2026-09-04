/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ComponentBrickCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalBrickCellLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment;

import net.minecraft.core.SectionPos;

import java.util.Arrays;
import java.util.Objects;

/** Reusable primitive Air geometry, adjacency, and FarField Brick compiler. */
public final class BrickTopologyCompiler {
    private static final long FULL_AIR = -1L;
    private static final double PATCH_AREA = 1.0D / 16.0D;

    private final ThermalCellArena arena;
    private final ThermalSignatureTable signatures;
    private final ThermalTopologyParameters parameters;
    private final FarFieldSettings farField;
    private final int maximumArenaSlots;
    private final BrickMaterialKernel material;
    private final ComponentBrickCompiler.Scratch componentScratch =
            new ComponentBrickCompiler.Scratch();
    private final ThermalBrickCellLayout cellLayout =
            new ThermalBrickCellLayout();
    private final ConservativeAirGeometry.Resolution[] geometry =
            new ConservativeAirGeometry.Resolution[64];
    private final int[] signatureIds = new int[64];
    private final PrimitiveTopologyScratch.LongPairDouble airPairs =
            new PrimitiveTopologyScratch.LongPairDouble();
    private final PrimitiveTopologyScratch.LongPairDouble farBoundaries =
            new PrimitiveTopologyScratch.LongPairDouble();
    private boolean fragmentResolved;

    public BrickTopologyCompiler(
            ThermalCellArena arena,
            ThermalSignatureTable signatures,
            MaterialBoundaryRegistry materials,
            ThermalTopologyParameters parameters,
            FarFieldSettings farField,
            int maximumArenaSlots
    ) {
        this.arena = Objects.requireNonNull(arena, "arena");
        this.signatures = Objects.requireNonNull(signatures, "signatures");
        this.parameters = Objects.requireNonNull(parameters, "parameters");
        this.farField = Objects.requireNonNull(farField, "farField");
        this.maximumArenaSlots = maximumArenaSlots;
        material = new BrickMaterialKernel(
                signatures, Objects.requireNonNull(materials, "materials"));
    }

    WorkerBrickTopology compileCells(
            WorkerPageStore.PageState page,
            PageSignatures nextSignatures,
            int brickIndex,
            TopologyView view
    ) {
        int sectionMinX = SectionPos.sectionToBlockCoord(
                SectionPos.x(page.handle.sectionKey()));
        int sectionMinY = SectionPos.sectionToBlockCoord(
                SectionPos.y(page.handle.sectionKey()));
        int sectionMinZ = SectionPos.sectionToBlockCoord(
                SectionPos.z(page.handle.sectionKey()));
        int brickX = brickIndex & 3;
        int brickZ = brickIndex >>> 2 & 3;
        int brickY = brickIndex >>> 4 & 3;
        int minX = sectionMinX + (brickX << 2);
        int minY = sectionMinY + (brickY << 2);
        int minZ = sectionMinZ + (brickZ << 2);
        cellLayout.reset(minX, minY, minZ);

        int airMicrocells = 0;
        boolean fullAir = true;
        for (int localY = 0; localY < 4; localY++) {
            for (int localZ = 0; localZ < 4; localZ++) {
                for (int localX = 0; localX < 4; localX++) {
                    int block = localX | localZ << 2 | localY << 4;
                    int pageBlock = brickX * 4 + localX
                            | (brickZ * 4 + localZ) << 4
                            | (brickY * 4 + localY) << 8;
                    int signatureId = nextSignatures.get(pageBlock);
                    signatureIds[block] = signatureId;
                    ConservativeAirGeometry.Resolution resolved =
                            signatures.geometry(signatureId);
                    geometry[block] = resolved;
                    if (resolved == null) {
                        return unresolved();
                    }
                    long airMask = signatures.airMask(signatureId);
                    airMicrocells += Long.bitCount(airMask);
                    fullAir &= airMask == FULL_AIR
                            && resolved.components().size() == 1;
                }
            }
        }

        ComponentBrickCompiler.CompiledBrick mixed = null;
        if (airMicrocells == 0) {
        } else if (fullAir && airMicrocells == 64 * 64) {
            cellLayout.setRegularAir(
                    parameters.effectiveAirCapacityJPerBlockK());
        } else {
            mixed = ComponentBrickCompiler.compileResolved(
                    geometry,
                    parameters.maximumRegionsPerBlock(),
                    componentScratch);
            if (mixed == null || mixed.componentCount() == 0) {
                return unresolved();
            }
            cellLayout.setMixedAir(
                    mixed,
                    parameters.effectiveAirCapacityJPerBlockK());
        }
        if (!material.compileLayout(
                page, minX, minY, minZ, signatureIds,
                nextSignatures, view, cellLayout)) {
            return unresolved();
        }
        WorkerBrickTopology.MaterialContacts contacts =
                material.freezeContacts();
        ThermalCellArena.BrickAllocation allocation = arena.stageBrickCells(
                page.pageSlot,
                page.lifecycleGeneration,
                cellLayout,
                view.naturalTemperature(page),
                parameters.referenceTemperatureC(),
                maximumArenaSlots);
        if (allocation == null) {
            throw new TopologyPlan.WorkLimitedException(
                    "thermal arena slot limit reached");
        }
        try {
            int coverage = airMicrocells == 0
                    ? -1 : allocation.cellSpan().firstSlot();
            int coverageGeneration = coverage < 0
                    ? 0 : arena.lifecycleGeneration(coverage);
            PagePublication.PhaseCandidates phaseCandidates =
                    material.phaseCandidates(contacts);
            return new WorkerBrickTopology(
                    allocation.cellSpan(),
                    coverage,
                    coverageGeneration,
                    mixed,
                    phaseCandidates,
                    material.materialPoles(contacts, allocation),
                    material.phaseReservoirs(
                            contacts, allocation, minX, minY, minZ),
                    contacts,
                    true,
                    true);
        } catch (RuntimeException | Error failure) {
            arena.discardStagedCells(allocation.cellSpan());
            throw failure;
        }
    }

    CompiledFragment compileFragment(
            WorkerPageStore.PageState page,
            int brickIndex,
            TopologyView view
    ) {
        airPairs.reset();
        farBoundaries.reset();
        fragmentResolved = true;
        WorkerBrickTopology owner = view.brick(page, brickIndex);
        if (!owner.cellsResolved) {
            return new CompiledFragment(ThermalFragment.EMPTY, false);
        }
        int minX = brickMinX(page, brickIndex);
        int minY = brickMinY(page, brickIndex);
        int minZ = brickMinZ(page, brickIndex);
        if (owner.coverageSlot >= 0) {
            compilePositive(
                    owner, view.brickAtWorld(minX + 4, minY, minZ),
                    ConservativeAirGeometry.Face.POSITIVE_X,
                    ConservativeAirGeometry.Face.NEGATIVE_X, 0, minX + 4);
            compilePositive(
                    owner, view.brickAtWorld(minX, minY + 4, minZ),
                    ConservativeAirGeometry.Face.POSITIVE_Y,
                    ConservativeAirGeometry.Face.NEGATIVE_Y, 1, minY + 4);
            compilePositive(
                    owner, view.brickAtWorld(minX, minY, minZ + 4),
                    ConservativeAirGeometry.Face.POSITIVE_Z,
                    ConservativeAirGeometry.Face.NEGATIVE_Z, 2, minZ + 4);
            compileExternalBoundaries(page, owner, brickIndex, view);
        }
        BrickMaterialKernel.Operations materialOperations =
                material.compileOperations(owner.materialContacts, owner, view);
        ThermalFragment fragment = new ThermalFragment(
                Integer.toUnsignedLong(page.fragmentIndex(brickIndex)),
                freezeAirPairs(),
                materialOperations.pairs(),
                materialOperations.phases(),
                freezeFarBoundaries(page.pageSlot));
        return new CompiledFragment(
                fragment,
                fragmentResolved && materialOperations.resolved());
    }

    private WorkerBrickTopology unresolved() {
        return new WorkerBrickTopology(
                com.teammoeg.frostedheart.content.climate.thermal.mesh
                        .ArenaSpan.EMPTY,
                -1, 0, null,
                PagePublication.PhaseCandidates.EMPTY,
                WorkerBrickTopology.MaterialPoles.EMPTY,
                WorkerBrickTopology.PhaseReservoirs.EMPTY,
                WorkerBrickTopology.MaterialContacts.EMPTY,
                false, false);
    }

    private void compilePositive(
            WorkerBrickTopology negative,
            WorkerBrickTopology positive,
            ConservativeAirGeometry.Face negativeFace,
            ConservativeAirGeometry.Face positiveFace,
            int axis,
            int plane
    ) {
        if (positive == null || !positive.cellsResolved) {
            if (positive != null) {
                fragmentResolved = false;
            }
            return;
        }
        if (positive.coverageSlot < 0) {
            return;
        }
        ComponentBrickCompiler.CompiledBrick left = negative.mixedGeometry;
        ComponentBrickCompiler.CompiledBrick right = positive.mixedGeometry;
        if (left == null && right == null) {
            addAirPair(
                    negative.coverageSlot, positive.coverageSlot,
                    axis, plane, 16.0D);
            return;
        }
        if (left != null && right != null) {
            for (int a = 0; a < left.facePortCount(); a++) {
                if (left.facePortFace(a) != negativeFace) {
                    continue;
                }
                for (int b = 0; b < right.facePortCount(); b++) {
                    if (right.facePortFace(b) != positiveFace
                            || left.facePortBlockSlot(a)
                                    != right.facePortBlockSlot(b)) {
                        continue;
                    }
                    int aperture = left.facePortApertureMask(a)
                            & right.facePortApertureMask(b);
                    if (aperture != 0) {
                        addAirPair(
                                negative.coverageSlot
                                        + left.facePortComponentId(a),
                                positive.coverageSlot
                                        + right.facePortComponentId(b),
                                axis, plane,
                                Integer.bitCount(aperture) * PATCH_AREA);
                    }
                }
            }
            return;
        }
        ComponentBrickCompiler.CompiledBrick mixed = left != null ? left : right;
        ConservativeAirGeometry.Face face =
                left != null ? negativeFace : positiveFace;
        for (int port = 0; port < mixed.facePortCount(); port++) {
            if (mixed.facePortFace(port) != face) {
                continue;
            }
            addAirPair(
                    left != null
                            ? negative.coverageSlot
                                    + mixed.facePortComponentId(port)
                            : negative.coverageSlot,
                    right != null
                            ? positive.coverageSlot
                                    + mixed.facePortComponentId(port)
                            : positive.coverageSlot,
                    axis, plane,
                    Integer.bitCount(mixed.facePortApertureMask(port))
                            * PATCH_AREA);
        }
    }

    private void addAirPair(
            int first,
            int second,
            int axis,
            int plane,
            double area
    ) {
        double firstDistance = plane - center(first, axis);
        double secondDistance = center(second, axis) - plane;
        if (arena.isMixedComponent(first)) {
            firstDistance = Math.max(
                    parameters.minimumMixedFaceDistanceBlocks(),
                    firstDistance);
        }
        if (arena.isMixedComponent(second)) {
            secondDistance = Math.max(
                    parameters.minimumMixedFaceDistanceBlocks(),
                    secondDistance);
        }
        double distance = firstDistance + secondDistance;
        if (!Double.isFinite(distance) || distance <= 0.0D) {
            throw new IllegalStateException(
                    "Air pair center distance is invalid");
        }
        airPairs.add(first, second, area / distance);
    }

    private double center(int slot, int axis) {
        return arena.center(slot, axis);
    }

    private void compileExternalBoundaries(
            WorkerPageStore.PageState page,
            WorkerBrickTopology owner,
            int brickIndex,
            TopologyView view
    ) {
        int minX = brickMinX(page, brickIndex);
        int minY = brickMinY(page, brickIndex);
        int minZ = brickMinZ(page, brickIndex);
        int x = brickIndex & 3;
        int z = brickIndex >>> 2 & 3;
        int y = brickIndex >>> 4 & 3;
        if (x == 0 && view.brickAtWorld(minX - 4, minY, minZ) == null) {
            addFarFace(
                    page, owner, brickIndex,
                    ConservativeAirGeometry.Face.NEGATIVE_X, view);
        }
        if (x == 3 && view.brickAtWorld(minX + 4, minY, minZ) == null) {
            addFarFace(
                    page, owner, brickIndex,
                    ConservativeAirGeometry.Face.POSITIVE_X, view);
        }
        if (y == 0 && view.brickAtWorld(minX, minY - 4, minZ) == null) {
            addFarFace(
                    page, owner, brickIndex,
                    ConservativeAirGeometry.Face.NEGATIVE_Y, view);
        }
        if (y == 3 && view.brickAtWorld(minX, minY + 4, minZ) == null) {
            addFarFace(
                    page, owner, brickIndex,
                    ConservativeAirGeometry.Face.POSITIVE_Y, view);
        }
        if (z == 0 && view.brickAtWorld(minX, minY, minZ - 4) == null) {
            addFarFace(
                    page, owner, brickIndex,
                    ConservativeAirGeometry.Face.NEGATIVE_Z, view);
        }
        if (z == 3 && view.brickAtWorld(minX, minY, minZ + 4) == null) {
            addFarFace(
                    page, owner, brickIndex,
                    ConservativeAirGeometry.Face.POSITIVE_Z, view);
        }
    }

    private void addFarFace(
            WorkerPageStore.PageState page,
            WorkerBrickTopology owner,
            int brickIndex,
            ConservativeAirGeometry.Face face,
            TopologyView view
    ) {
        ComponentBrickCompiler.CompiledBrick mixed = owner.mixedGeometry;
        if (mixed == null) {
            int direct = directSkyColumns(page, brickIndex, face, -1, view)
                    * 16;
            if (direct != 0) {
                farBoundaries.add(
                        owner.coverageSlot, 0L,
                        farField.conductanceForPatches(direct, true));
            }
            return;
        }
        for (int port = 0; port < mixed.facePortCount(); port++) {
            if (mixed.facePortFace(port) != face) {
                continue;
            }
            int patches = Integer.bitCount(
                    mixed.facePortApertureMask(port));
            int direct = directSkyColumns(
                    page, brickIndex, face,
                    mixed.facePortBlockSlot(port), view) > 0
                    ? patches : 0;
            if (direct != 0) {
                farBoundaries.add(
                        owner.coverageSlot + mixed.facePortComponentId(port),
                        0L,
                        farField.conductanceForPatches(direct, true));
            }
        }
    }

    private static int directSkyColumns(
            WorkerPageStore.PageState page,
            int brickIndex,
            ConservativeAirGeometry.Face face,
            int faceBlockSlot,
            TopologyView view
    ) {
        if (face != ConservativeAirGeometry.Face.POSITIVE_Y) {
            return 0;
        }
        int firstX = (brickIndex & 3) << 2;
        int firstZ = (brickIndex >>> 2 & 3) << 2;
        if (faceBlockSlot >= 0) {
            int column = firstX + (faceBlockSlot & 3)
                    | (firstZ + (faceBlockSlot >>> 2)) << 4;
            return view.firstExposedLocalY(page, column) <= 15 ? 1 : 0;
        }
        int direct = 0;
        for (int z = firstZ; z < firstZ + 4; z++) {
            for (int x = firstX; x < firstX + 4; x++) {
                if (view.firstExposedLocalY(page, x | z << 4) <= 15) {
                    direct++;
                }
            }
        }
        return direct;
    }

    private ThermalFragment.AirPairs freezeAirPairs() {
        int count = airPairs.size();
        if (count == 0) {
            return ThermalFragment.AirPairs.EMPTY;
        }
        int[] first = new int[count];
        int[] second = new int[count];
        double[] conductance = new double[count];
        double[] firstY = new double[count];
        double[] secondY = new double[count];
        for (int index = 0; index < count; index++) {
            first[index] = (int) airPairs.first(index);
            second[index] = (int) airPairs.second(index);
            conductance[index] = parameters.effectiveMixingWPerBlockK()
                    * airPairs.value(index);
            firstY[index] = arena.center(first[index], 1);
            secondY[index] = arena.center(second[index], 1);
        }
        return new ThermalFragment.AirPairs(
                first, second, conductance, firstY, secondY);
    }

    private ThermalFragment.FarBoundaries freezeFarBoundaries(
            int ownerPageSlot
    ) {
        int count = farBoundaries.size();
        if (count == 0) {
            return ThermalFragment.FarBoundaries.EMPTY;
        }
        int[] cell = new int[count];
        double[] conductance = new double[count];
        double[] coefficient = new double[count];
        for (int index = 0; index < count; index++) {
            cell[index] = (int) farBoundaries.first(index);
            conductance[index] = farBoundaries.value(index);
        }
        return new ThermalFragment.FarBoundaries(
                cell, ownerPageSlot, conductance, coefficient);
    }

    private static int brickMinX(
            WorkerPageStore.PageState page,
            int brick
    ) {
        return SectionPos.sectionToBlockCoord(
                SectionPos.x(page.handle.sectionKey()))
                + ((brick & 3) << 2);
    }

    private static int brickMinY(
            WorkerPageStore.PageState page,
            int brick
    ) {
        return SectionPos.sectionToBlockCoord(
                SectionPos.y(page.handle.sectionKey()))
                + ((brick >>> 4 & 3) << 2);
    }

    private static int brickMinZ(
            WorkerPageStore.PageState page,
            int brick
    ) {
        return SectionPos.sectionToBlockCoord(
                SectionPos.z(page.handle.sectionKey()))
                + ((brick >>> 2 & 3) << 2);
    }

    record CompiledFragment(
            ThermalFragment fragment,
            boolean resolved
    ) {
    }
}
