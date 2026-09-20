/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalBrickCellLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirMixingRegion;

import net.minecraft.core.SectionPos;

import java.util.Arrays;
import java.util.function.Consumer;

/** Whole-block connectivity compiler with one-node full-Air fast paths. */
public final class BrickTopologyCompiler {
    private final ThermalCellArena arena;
    private final ThermalSignatureTable signatures;
    private final MaterialBoundaryRegistry materials;
    private final ThermalTopologyParameters parameters;
    private final FarFieldSettings farField;
    private final int maximumArenaSlots;
    private final ThermalBrickCellLayout cells = new ThermalBrickCellLayout();
    private final int[] signatureIds = new int[64];
    private final byte[] blockToNode = new byte[64];
    private final long[] nodeBlockMasks = new long[64];
    private final PrimitiveTopologyScratch.LongPairDouble[] airPairs = {
        new PrimitiveTopologyScratch.LongPairDouble(),
        new PrimitiveTopologyScratch.LongPairDouble(),
        new PrimitiveTopologyScratch.LongPairDouble()
    };
    private final AirMixingRegion mixing = new AirMixingRegion();
    private final Consumer<AirMixingRegion> mixingSources;
    private final ThermalFragment.RoutedContacts.Builder routedContacts =
            new ThermalFragment.RoutedContacts.Builder();
    private final PrimitiveTopologyScratch.LongPairDouble materialPairs =
            new PrimitiveTopologyScratch.LongPairDouble();
    private final PrimitiveTopologyScratch.LongPairDouble farBoundaries =
            new PrimitiveTopologyScratch.LongPairDouble();
    private boolean fragmentResolved;
    private boolean spatialAir;
    private final it.unimi.dsi.fastutil.ints.IntArrayList farPatchCells = new it.unimi.dsi.fastutil.ints.IntArrayList();
    private final it.unimi.dsi.fastutil.longs.LongArrayList farPatchBlocks = new it.unimi.dsi.fastutil.longs.LongArrayList();
    private final it.unimi.dsi.fastutil.doubles.DoubleArrayList farPatchConductances = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();

    public void captureSpatialAirContacts() { spatialAir = true; }
    private static final long[] NEIGHBORS = new long[64];

    static {
        for (int blockIndex = 0; blockIndex < 64; blockIndex++) {
            long neighborMask = 0;
            if ((blockIndex & 3) > 0) neighborMask |= 1L << (blockIndex - 1);
            if ((blockIndex & 3) < 3) neighborMask |= 1L << (blockIndex + 1);
            if ((blockIndex >>> 2 & 3) > 0) neighborMask |= 1L << (blockIndex - 4);
            if ((blockIndex >>> 2 & 3) < 3) neighborMask |= 1L << (blockIndex + 4);
            if ((blockIndex >>> 4) > 0) neighborMask |= 1L << (blockIndex - 16);
            if ((blockIndex >>> 4) < 3) neighborMask |= 1L << (blockIndex + 16);
            NEIGHBORS[blockIndex] = neighborMask;
        }
    }

    public BrickTopologyCompiler(
            ThermalCellArena arena,
            ThermalSignatureTable signatures,
            MaterialBoundaryRegistry materials,
            ThermalTopologyParameters parameters,
            FarFieldSettings farField,
            int maximumArenaSlots,
            Consumer<AirMixingRegion> mixingSources) {
        this.arena = arena;
        this.signatures = signatures;
        this.materials = materials;
        this.parameters = parameters;
        this.farField = farField;
        this.maximumArenaSlots = maximumArenaSlots;
        this.mixingSources = mixingSources;
    }

    WorkerBrickTopology compileCells(
            WorkerPageStore.PageState page,
            PageSignatures nextSignatures,
            int brick,
            TopologyView view) {
        int x = brickMinX(page, brick), y = brickMinY(page, brick), z = brickMinZ(page, brick);
        cells.reset(x, y, z);
        long mergeableAirBlocks = 0;
        for (int blockIndex = 0; blockIndex < 64; blockIndex++) {
            signatureIds[blockIndex] =
                    nextSignatures.get(BlockBrickLayout.pageBlock(brick, blockIndex));
            if (!signatures.valid(signatureIds[blockIndex])) return WorkerBrickTopology.EMPTY;
            if (signatures.mergeable(signatureIds[blockIndex]))
                mergeableAirBlocks |= 1L << blockIndex;
        }
        int nodeCount = 0, airNodeCount = 0;
        int phaseMaterialCount = 0;
        long materialNodeMask = 0;
        BlockBrickLayout layout = null;
        if (mergeableAirBlocks == -1L) {
            cells.setRegularAir(parameters.effectiveAirCapacityJPerBlockK());
            airNodeCount = 1;
        } else {
            Arrays.fill(blockToNode, (byte) 255);
            nodeCount = compileConnectedAir(mergeableAirBlocks);
            // Only actual Air owns air capacity. Ventilated materials
            // participate in geometric routes and retain one separate body H.
            for (int blockIndex = 0; blockIndex < 64; blockIndex++) {
                if (!signatures.isAir(signatureIds[blockIndex])
                        || blockToNode[blockIndex] != (byte) 255
                        || signatures.ventilation(signatureIds[blockIndex]) == 0) continue;
                mapNode(nodeCount, 1L << blockIndex);
                cells.setAirCapacity(nodeCount++, parameters.effectiveAirCapacityJPerBlockK());
            }
            airNodeCount = nodeCount;
            // Material nodes follow Air nodes. Each block keeps its own H and whole-body C.
            for (int blockIndex = 0; blockIndex < 64; blockIndex++) {
                var profile =
                        materials.profileOrNull(
                                signatures.materialProfileId(signatureIds[blockIndex]));
                if (profile == null) continue;
                materialNodeMask |= 1L << nodeCount;
                mapNode(nodeCount++, 1L << blockIndex);
                cells.addMaterialCell(
                        x + (blockIndex & 3),
                        y + (blockIndex >>> 4),
                        z + (blockIndex >>> 2 & 3),
                        profile.thermalLaw().capacityJPerK(),
                        view.naturalTemperature(page));
                if (profile.thermalLaw().heating() != null
                        || profile.thermalLaw().cooling() != null) phaseMaterialCount++;
            }
            if (nodeCount > 0) {
                layout =
                        new BlockBrickLayout(
                                blockToNode.clone(),
                                Arrays.copyOf(nodeBlockMasks, nodeCount),
                                airNodeCount,
                                materialNodeMask);
                if (airNodeCount > 0)
                    cells.setMixedAir(layout, parameters.effectiveAirCapacityJPerBlockK());
            }
        }
        // Allocate only after the complete layout is known. Nothing is live until commit.
        var allocation =
                arena.stageBrickCells(
                        page.pageSlot,
                        page.lifecycleGeneration,
                        cells,
                        view.naturalTemperature(page),
                        parameters.referenceTemperatureC(),
                        maximumArenaSlots);
        if (allocation == null)
            throw new TopologyUpdatePlanner.WorkLimitedException(
                    "thermal arena slot limit reached");
        try {
            // Register only phase-capable material slots; this does not merge material bodies.
            int[] phaseSlots = new int[phaseMaterialCount];
            int phaseSlotCount = 0;
            if (layout != null)
                for (int block = 0; block < 64; block++) {
                    var profile =
                            materials.profileOrNull(
                                    signatures.materialProfileId(signatureIds[block]));
                    if (profile == null || profile.thermalLaw() == null) continue;
                    int slot = allocation.firstSlot() + layout.nodeAt(block);
                    arena.stageMaterialLaw(
                            slot,
                            profile.id(),
                            profile.thermalLaw(),
                            view.naturalTemperature(page));
                    if (profile.thermalLaw().heating() == null
                            && profile.thermalLaw().cooling() == null) continue;
                    phaseSlots[phaseSlotCount++] = slot;
                }
            int coverage = airNodeCount == 0 ? -1 : allocation.firstSlot();
            return new WorkerBrickTopology(
                    allocation,
                    coverage,
                    allocation.count() == 0 ? 0 : arena.lifecycleGeneration(allocation.firstSlot()),
                    layout,
                    airNodeCount,
                    phaseSlots,
                    true,
                    true);
        } catch (RuntimeException | Error failure) {
            arena.discardStagedCells(allocation);
            throw failure;
        }
    }

    /** Flood-fill real Air using x/z/y bits 0..1/2..3/4..5 within this Brick. */
    private int compileConnectedAir(long remainingBlocks) {
        int nodeCount = 0;
        while (remainingBlocks != 0) {
            long pendingBlocks = Long.lowestOneBit(remainingBlocks);
            long memberBlocks = 0;
            while (pendingBlocks != 0) {
                int blockIndex = Long.numberOfTrailingZeros(pendingBlocks);
                pendingBlocks &= pendingBlocks - 1;
                if ((remainingBlocks & 1L << blockIndex) == 0) continue;
                remainingBlocks &= ~(1L << blockIndex);
                memberBlocks |= 1L << blockIndex;
                pendingBlocks |= NEIGHBORS[blockIndex] & remainingBlocks;
            }
            mapNode(nodeCount, memberBlocks);
            cells.setAirCapacity(
                    nodeCount++,
                    Long.bitCount(memberBlocks) * parameters.effectiveAirCapacityJPerBlockK());
        }
        return nodeCount;
    }

    private void mapNode(int node, long members) {
        nodeBlockMasks[node] = members;
        while (members != 0) {
            int blockIndex = Long.numberOfTrailingZeros(members);
            members &= members - 1;
            blockToNode[blockIndex] = (byte) node;
        }
    }

    CompiledFragment compileFragment(WorkerPageStore.PageState page, int brick, TopologyView view) {
        for (var pairs : airPairs) pairs.reset();
        materialPairs.reset();
        farBoundaries.reset();
        farPatchCells.clear();
        farPatchBlocks.clear();
        farPatchConductances.clear();
        fragmentResolved = true;
        routedContacts.clear();
        var owner = view.brick(page, brick);
        if (!owner.cellsResolved) return new CompiledFragment(ThermalFragment.EMPTY, false);
        int x = brickMinX(page, brick), y = brickMinY(page, brick), z = brickMinZ(page, brick);
        mixing.reset(x, y, z);
        mixingSources.accept(mixing);
        PageSignatures cut = view.signatures(page);
        if (owner.blockLayout != null) {
            for (int blockIndex = 0; blockIndex < 64; blockIndex++)
                signatureIds[blockIndex] = cut.get(BlockBrickLayout.pageBlock(brick, blockIndex));
            for (int blockIndex = 0; blockIndex < 64; blockIndex++) {
                int bx = x + (blockIndex & 3),
                        by = y + (blockIndex >>> 4),
                        bz = z + (blockIndex >>> 2 & 3);
                if ((blockIndex & 3) < 3)
                    connectFace(
                            owner,
                            blockIndex,
                            owner,
                            blockIndex + 1,
                            0,
                            bx,
                            by,
                            bz,
                            signatureIds[blockIndex],
                            signatureIds[blockIndex + 1],
                            view);
                if ((blockIndex >>> 4) < 3)
                    connectFace(
                            owner,
                            blockIndex,
                            owner,
                            blockIndex + 16,
                            1,
                            bx,
                            by,
                            bz,
                            signatureIds[blockIndex],
                            signatureIds[blockIndex + 16],
                            view);
                if ((blockIndex >>> 2 & 3) < 3)
                    connectFace(
                            owner,
                            blockIndex,
                            owner,
                            blockIndex + 4,
                            2,
                            bx,
                            by,
                            bz,
                            signatureIds[blockIndex],
                            signatureIds[blockIndex + 4],
                            view);
            }
        }
        for (int axis = 0; axis < 3; axis++) {
            int nx = x + (axis == 0 ? 4 : 0);
            int ny = y + (axis == 1 ? 4 : 0);
            int nz = z + (axis == 2 ? 4 : 0);
            var neighborPage =
                    view.page(
                            SectionPos.asLong(
                                    SectionPos.blockToSectionCoord(nx),
                                    SectionPos.blockToSectionCoord(ny),
                                    SectionPos.blockToSectionCoord(nz)));
            if (neighborPage == null) continue;
            int neighborBrick = (nx & 15) >>> 2 | ((nz & 15) >>> 2) << 2 | ((ny & 15) >>> 2) << 4;
            if (!view.resident(neighborPage, neighborBrick)) continue;
            var neighbor = view.brick(neighborPage, neighborBrick);
            if (!neighbor.cellsResolved) {
                fragmentResolved = false;
                continue;
            }
            int plane = axis == 0 ? nx : axis == 1 ? ny : nz;
            if (owner.blockLayout == null
                    && neighbor.blockLayout == null
                    && owner.coverageSlot >= 0
                    && neighbor.coverageSlot >= 0) {
                addAirPair(
                        owner.coverageSlot,
                        neighbor.coverageSlot,
                        axis,
                        plane,
                        mixing.brickFaceArea(axis, x, y, z),
                        100,
                        100);
                continue;
            }
            PageSignatures neighborCut = neighborPage == page ? cut : view.signatures(neighborPage);
            for (int i = 0; i < 16; i++) {
                int left = BlockBrickLayout.faceBlock(axis, 3, i);
                int right = BlockBrickLayout.faceBlock(axis, 0, i);
                int leftId = cut.get(BlockBrickLayout.pageBlock(brick, left));
                int rightId = neighborCut.get(BlockBrickLayout.pageBlock(neighborBrick, right));
                connectFace(
                        owner,
                        left,
                        neighbor,
                        right,
                        axis,
                        x + (left & 3),
                        y + (left >>> 4),
                        z + (left >>> 2 & 3),
                        leftId,
                        rightId,
                        view);
            }
        }
        // Existing FarField eligibility is direct sky at the absent upper Page.
        if ((brick >>> 4) == 3
                && view.brickAtWorld(x, y + 4, z) == null
                && owner.coverageSlot >= 0) {
            for (int i = 0; i < 16; i++) {
                int blockIndex = BlockBrickLayout.faceBlock(1, 3, i),
                        slot = owner.airSlotAt(blockIndex);
                int column =
                        ((x + (blockIndex & 3)) & 15) | ((z + (blockIndex >>> 2 & 3)) & 15) << 4;
                if (slot < 0 || view.firstExposedLocalY(page, column) > 15) continue;
                int v =
                        signatures.ventilation(
                                cut.get(BlockBrickLayout.pageBlock(brick, blockIndex)));
                double conductance = farField.conductanceForPatches(16, true) * v / 100.0;
                farBoundaries.add(slot, 0, conductance);
                if (spatialAir) {
                    farPatchCells.add(slot);
                    farPatchBlocks.add(net.minecraft.core.BlockPos.asLong(x + (blockIndex & 3),
                            y + (blockIndex >>> 4), z + (blockIndex >>> 2 & 3)));
                    farPatchConductances.add(conductance);
                }
            }
        }
        long origin = net.minecraft.core.BlockPos.asLong(x, y, z);
        view.airRoutes()
                .appendEdges(origin, view, parameters.effectiveMixingWPerBlockK(), routedContacts);
        BlockBrickLayout routedLayout = view.airRoutes().publishRoutes(origin, owner.blockLayout);
        long airContactBlocks = 0;
        if (routedLayout != null)
            for (int block = 0; block < 64; block++) {
                int slot = owner.slotAt(block);
                long position = AirRouteCompiler.blockPosition(origin, block);
                int bx = net.minecraft.core.BlockPos.getX(position),
                        by = net.minecraft.core.BlockPos.getY(position),
                        bz = net.minecraft.core.BlockPos.getZ(position);
                if (slot < 0 || arena.materialLaw(slot) == null) continue;
                if (view.materialSurfaceAt(bx, by, bz, signatures)) airContactBlocks |= 1L << block;
                if (!view.airRoutes().hasRegion(position)
                        || view.hasDirectAir(bx, by, bz, signatures)) continue;
                int air = view.airSlotAt(view.airRoutes().region(position));
                if (air < 0) continue;
                double surfaceG =
                        materials
                                .profileOrNull(arena.materialProfileId(slot))
                                .faceConductanceWPerK();
                double conductance =
                        1
                                / (view.airRoutes().normalizedResistance(position)
                                                / parameters.effectiveMixingWPerBlockK()
                                        + 1 / surfaceG);
                routedContacts.add(slot, air, conductance, view.airRoutes().validity(position),
                        null, view.airRoutes().outletTrace(position));
            }
        if (routedLayout != null)
            routedLayout = routedLayout.withAirContactBlocks(airContactBlocks);
        return new CompiledFragment(
                new ThermalFragment(
                        Integer.toUnsignedLong(page.fragmentIndex(brick)),
                        freezeAirPairs(),
                        freezeMaterialPairs(),
                        routedContacts.build(),
                        freezeFarBoundaries(page.pageSlot)),
                fragmentResolved,
                routedLayout);
    }

    private void connectFace(
            WorkerBrickTopology firstBrick,
            int firstBlock,
            WorkerBrickTopology secondBrick,
            int secondBlock,
            int axis,
            int x,
            int y,
            int z,
            int firstSignature,
            int secondSignature,
            TopologyView view) {
        int firstSlot = firstBrick.slotAt(firstBlock), secondSlot = secondBrick.slotAt(secondBlock);
        if (firstSlot < 0 || secondSlot < 0 || firstSlot == secondSlot) return;
        boolean firstBody = arena.materialLaw(firstSlot) != null;
        boolean secondBody = arena.materialLaw(secondSlot) != null;
        if (firstBody && secondBody) {
            int firstFace = axis * 2 + 1, secondFace = axis * 2;
            if ((signatures.fullContactFaces(firstSignature) & 1 << firstFace) == 0
                    || (signatures.fullContactFaces(secondSignature) & 1 << secondFace) == 0
                    || !view.materialContactAllowed(
                            arena.minimum(firstSlot, 0),
                            arena.minimum(firstSlot, 1),
                            arena.minimum(firstSlot, 2),
                            arena.minimum(secondSlot, 0),
                            arena.minimum(secondSlot, 1),
                            arena.minimum(secondSlot, 2),
                            signatures)) return;
            double firstG =
                    materials
                            .profileOrNull(arena.materialProfileId(firstSlot))
                            .faceConductanceWPerK();
            double secondG =
                    materials
                            .profileOrNull(arena.materialProfileId(secondSlot))
                            .faceConductanceWPerK();
            materialPairs.add(
                    Math.min(firstSlot, secondSlot),
                    Math.max(firstSlot, secondSlot),
                    2 * firstG * secondG / (firstG + secondG));
            return;
        }
        if (firstBody || secondBody) {
            int materialSlot = firstBody ? firstSlot : secondSlot;
            int airSlot = firstBody ? secondSlot : firstSlot;
            if (!arena.isAirCell(airSlot)) return;
            materialPairs.add(
                    Math.min(airSlot, materialSlot),
                    Math.max(airSlot, materialSlot),
                    materials
                            .profileOrNull(arena.materialProfileId(materialSlot))
                            .faceConductanceWPerK());
            return;
        }
        int firstVentilation = signatures.ventilation(firstSignature),
                secondVentilation = signatures.ventilation(secondSignature);
        if (firstVentilation > 0 && secondVentilation > 0) {
            int plane = (axis == 0 ? x : axis == 1 ? y : z) + 1;
            addAirPair(
                    firstSlot,
                    secondSlot,
                    axis,
                    plane,
                    mixing.faceArea(axis, x, y, z),
                    firstVentilation,
                    secondVentilation);
        }
    }

    private void addAirPair(
            int first,
            int second,
            int axis,
            int plane,
            double area,
            int firstVentilation,
            int secondVentilation) {
        double firstDistance = Math.max(0.5, plane - arena.center(first, axis));
        double secondDistance = Math.max(0.5, arena.center(second, axis) - plane);
        byte direction =
                axis != 1
                        ? ThermalFragment.AirPairs.HORIZONTAL
                        : first < second
                                ? ThermalFragment.AirPairs.FIRST_BELOW
                                : ThermalFragment.AirPairs.SECOND_BELOW;
        airPairs[direction].add(
                Math.min(first, second),
                Math.max(first, second),
                area
                        / (firstDistance * 100.0 / firstVentilation
                                + secondDistance * 100.0 / secondVentilation));
    }

    private ThermalFragment.MaterialContributions freezeMaterialPairs() {
        int count = materialPairs.size();
        if (count == 0) return ThermalFragment.MaterialContributions.EMPTY;
        int[] firstSlots = new int[count];
        int[] secondSlots = new int[count];
        double[] conductancesWPerK = new double[count];
        for (int index = 0; index < count; index++) {
            firstSlots[index] = (int) materialPairs.first(index);
            secondSlots[index] = (int) materialPairs.second(index);
            conductancesWPerK[index] = materialPairs.value(index);
        }
        return new ThermalFragment.MaterialContributions(
                firstSlots, secondSlots, conductancesWPerK);
    }

    private ThermalFragment.AirPairs freezeAirPairs() {
        int count = 0;
        for (var pairs : airPairs) count += pairs.size();
        if (count == 0) {
            return ThermalFragment.AirPairs.EMPTY;
        }
        int[] first = new int[count];
        int[] second = new int[count];
        double[] conductance = new double[count];
        byte[] directions = new byte[count];
        int index = 0;
        for (byte direction = 0; direction < airPairs.length; direction++) {
            var pairs = airPairs[direction];
            for (int i = 0; i < pairs.size(); i++, index++) {
                first[index] = (int) pairs.first(i);
                second[index] = (int) pairs.second(i);
                conductance[index] = parameters.effectiveMixingWPerBlockK() * pairs.value(i);
                directions[index] = direction;
            }
        }
        return new ThermalFragment.AirPairs(first, second, conductance, directions);
    }

    private ThermalFragment.FarBoundaries freezeFarBoundaries(int ownerPageSlot) {
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
        return new ThermalFragment.FarBoundaries(cell, ownerPageSlot, conductance, coefficient,
                spatialAir ? farPatchCells.toIntArray() : null,
                spatialAir ? farPatchBlocks.toLongArray() : null,
                spatialAir ? farPatchConductances.toDoubleArray() : null);
    }

    private static int brickMinX(WorkerPageStore.PageState page, int brick) {
        return SectionPos.sectionToBlockCoord(SectionPos.x(page.handle.sectionKey()))
                + ((brick & 3) << 2);
    }

    private static int brickMinY(WorkerPageStore.PageState page, int brick) {
        return SectionPos.sectionToBlockCoord(SectionPos.y(page.handle.sectionKey()))
                + ((brick >>> 4 & 3) << 2);
    }

    private static int brickMinZ(WorkerPageStore.PageState page, int brick) {
        return SectionPos.sectionToBlockCoord(SectionPos.z(page.handle.sectionKey()))
                + ((brick >>> 2 & 3) << 2);
    }

    record CompiledFragment(ThermalFragment fragment, boolean resolved, BlockBrickLayout layout) {
        CompiledFragment(ThermalFragment fragment, boolean resolved) {
            this(fragment, resolved, null);
        }
    }
}
