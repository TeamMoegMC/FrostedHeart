/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.LocalAirShape;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;

/** Static local response preparation and footprint lifetime, confined to the thermal worker. */
public final class AirShapeStore {
    private static final int WIDTH = LocalAirShape.WIDTH;
    private static final int CELLS = WIDTH * WIDTH * WIDTH;
    private static final int[] DX = {-1, 1, 0, 0, 0, 0};
    private static final int[] DY = {0, 0, -1, 1, 0, 0};
    private static final int[] DZ = {0, 0, 0, 0, -1, 1};
    private final ThermalSignatureTable signatures;
    private final Long2ObjectOpenHashMap<Footprint[]> footprints = new Long2ObjectOpenHashMap<>();
    private final Long2LongOpenHashMap supportRequests = new Long2LongOpenHashMap();

    public AirShapeStore(ThermalSignatureTable signatures) { this.signatures = signatures; }

    public void restore(ThermalInputBatch.DormantAirCut cut) {
        var checkpoint = cut.entry().spatialAir();
        if (checkpoint == null) return;
        Set<Footprint> replaced = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int index = 0; index < checkpoint.shapeCount(); index++) {
            LocalAirShape shape = checkpoint.shape(index).sectionSlice(checkpoint.minX(), checkpoint.minY(), checkpoint.minZ());
            Footprint[] atBlock = footprints.computeIfAbsent(shape.sourceBlock, ignored -> new Footprint[6]);
            if (atBlock[shape.sourceFace] == null) atBlock[shape.sourceFace] = new Footprint(shape.sourceBlock, shape.sourceFace);
            Footprint footprint = atBlock[shape.sourceFace];
            if (replaced.add(footprint)) footprint.savedPieces.removeIf(old ->
                    old.minX >= checkpoint.minX() && old.minX < checkpoint.minX() + 16
                            && old.minY >= checkpoint.minY() && old.minY < checkpoint.minY() + 16
                            && old.minZ >= checkpoint.minZ() && old.minZ < checkpoint.minZ() + 16);
            footprint.savedPieces.add(shape);
            footprint.restoredAmplitudeC = Math.max(footprint.restoredAmplitudeC, Math.abs(checkpoint.amplitudeC(index) * cut.decayFactor()));
            footprint.shapes = null;
        }
    }

    public List<LocalAirShape> prepare(AirFieldLayout coarse, AirFieldLayout previous, double[] previousC,
            WorkerPageStore pages, WorkerPhysicalSourceBindings sources) {
        for (Footprint[] atBlock : footprints.values()) for (Footprint footprint : atBlock) {
            if (footprint != null) { footprint.active = false; footprint.amplitudeC = 0; }
        }
        if (previous != null) {
            for (int index = 0; index < previous.localShapeCount(); index++) {
                LocalAirShape shape = previous.localShape(index);
                Footprint[] atBlock = footprints.get(shape.sourceBlock);
                if (atBlock != null && atBlock[shape.sourceFace] != null)
                    atBlock[shape.sourceFace].amplitudeC += Math.abs(previousC[previous.coarseCount() + index]);
            }
        }
        sources.collectAirPorts((x, y, z, face) -> {
            long position = BlockPos.asLong(x, y, z);
            Footprint[] atBlock = footprints.computeIfAbsent(position, ignored -> new Footprint[6]);
            if (atBlock[face.ordinal()] == null) atBlock[face.ordinal()] = new Footprint(position, (byte) face.ordinal());
            atBlock[face.ordinal()].active = true;
        });
        supportRequests.clear();
        ArrayList<LocalAirShape> result = new ArrayList<>();
        long[] positions = footprints.keySet().toLongArray();
        Arrays.sort(positions);
        for (long position : positions) {
            Footprint[] atBlock = footprints.get(position);
            boolean retained = false;
            for (int face = 0; face < atBlock.length; face++) {
                Footprint footprint = atBlock[face];
                if (footprint == null) continue;
                int x = BlockPos.getX(position), y = BlockPos.getY(position), z = BlockPos.getZ(position);
                if ((!footprint.active && footprint.amplitudeC < 0.01 && footprint.restoredAmplitudeC < 0.01)
                        || coarse.componentAt(x, y, z) == null && footprint.savedPieces.isEmpty()) {
                    atBlock[face] = null;
                    continue;
                }
                retained = true;
                requestSupport(x, y, z);
                if (!footprint.savedPieces.isEmpty()) footprint.shapes = restorePieces(footprint, coarse, pages);
                else if (!current(footprint.shapes, pages)) footprint.shapes = compile(footprint, coarse, pages);
                if (footprint.shapes != null) {
                    result.addAll(Arrays.asList(footprint.shapes));
                    if (footprint.shapes.length > 0) footprint.restoredAmplitudeC = 0;
                }
            }
            if (!retained) footprints.remove(position);
        }
        pages.setAirSupportRequests(supportRequests);
        result.sort(Comparator.comparingLong((LocalAirShape shape) -> shape.sourceBlock)
                .thenComparingInt(shape -> shape.sourceFace).thenComparingLong(shape -> shape.branch).thenComparingInt(shape -> shape.scale));
        return result;
    }

    private static boolean current(LocalAirShape[] shapes, WorkerPageStore pages) {
        if (shapes == null || shapes.length == 0) return false;
        for (LocalAirShape shape : shapes) {
            for (int index = 0; index < shape.dependencyCount(); index++) {
                var page = pages.find(shape.dependency(index).sectionKey());
                if (page == null || page.handle != shape.dependency(index) || page.geometryRevision != shape.revision(index)) return false;
            }
        }
        return true;
    }

    private void requestSupport(int x, int y, int z) {
        for (int bx = (x - LocalAirShape.RADIUS) & ~3; bx <= x + LocalAirShape.RADIUS; bx += 4) {
            for (int by = (y - LocalAirShape.RADIUS) & ~3; by <= y + LocalAirShape.RADIUS; by += 4) {
                for (int bz = (z - LocalAirShape.RADIUS) & ~3; bz <= z + LocalAirShape.RADIUS; bz += 4) {
                    long section = SectionPos.asLong(bx >> 4, by >> 4, bz >> 4);
                    int brick = (bx & 15) >>> 2 | ((bz & 15) >>> 2) << 2 | ((by & 15) >>> 2) << 4;
                    supportRequests.put(section, supportRequests.get(section) | 1L << brick);
                }
            }
        }
    }

    private LocalAirShape[] compile(Footprint footprint, AirFieldLayout coarse, WorkerPageStore pages) {
        int minX = BlockPos.getX(footprint.position) - LocalAirShape.RADIUS;
        int minY = BlockPos.getY(footprint.position) - LocalAirShape.RADIUS;
        int minZ = BlockPos.getZ(footprint.position) - LocalAirShape.RADIUS;
        boolean[] air = new boolean[CELLS];
        LinkedHashMap<ThermalPageHandle, Long> dependencies = new LinkedHashMap<>();
        for (int cell = 0; cell < CELLS; cell++) {
            int x = minX + cell % WIDTH, y = minY + cell / (WIDTH * WIDTH), z = minZ + cell / WIDTH % WIDTH;
            int signature = pages.capturedSignatureAt(x, y, z);
            if (!signatures.valid(signature)) return null;
            var page = pages.find(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
            if (page == null) return null;
            dependencies.put(page.handle, page.geometryRevision);
            air[cell] = signatures.isAir(signature) && signatures.ventilation(signature) > 0;
            if (air[cell] && coarse.componentAt(x, y, z) == null) return null;
        }
        int source = LocalAirShape.RADIUS * (1 + WIDTH + WIDTH * WIDTH);
        ThermalPageHandle[] handles = dependencies.keySet().toArray(ThermalPageHandle[]::new);
        long[] revisions = new long[dependencies.size()];
        int revisionIndex = 0;
        for (long revision : dependencies.values()) revisions[revisionIndex++] = revision;
        if (footprint.shapes != null && Arrays.equals(footprint.airGeometry, air)) {
            LocalAirShape[] retained = new LocalAirShape[footprint.shapes.length];
            for (int index = 0; index < retained.length; index++) retained[index] = footprint.shapes[index].withDependencies(handles, revisions);
            return retained;
        }
        footprint.airGeometry = air;
        if (!air[source]) return new LocalAirShape[0];
        // Retain only the source's direct-Air branch within the finite support.
        boolean[] connected = new boolean[CELLS];
        int[] queue = new int[CELLS];
        int head = 0, tail = 0;
        queue[tail++] = source;
        connected[source] = true;
        while (head < tail) {
            int cell = queue[head++];
            for (int face = 0; face < 6; face++) {
                int neighbor = neighbor(cell, face);
                if (neighbor >= 0 && air[neighbor] && !connected[neighbor]) {
                    connected[neighbor] = true;
                    queue[tail++] = neighbor;
                }
            }
        }
        if (tail < 8) return new LocalAirShape[0];
        VertexLayout vertices = vertices(connected);
        short[] cellCorners = vertices.corners;
        int vertexCount = vertices.count;
        ArrayList<LocalAirShape> shapes = new ArrayList<>();
        float[] firstValues = null;
        for (int scale : new int[]{1, 3}) {
            double[] response = response(connected, source, scale);
            float[] values = new float[vertexCount];
            int[] samples = new int[vertexCount];
            boolean[] shell = new boolean[vertexCount];
            for (int cell = 0; cell < CELLS; cell++) {
                if (!connected[cell]) continue;
                int x = cell % WIDTH, y = cell / (WIDTH * WIDTH), z = cell / WIDTH % WIDTH;
                for (int corner = 0; corner < 8; corner++) {
                    int vertex = Short.toUnsignedInt(cellCorners[cell * 8 + corner]);
                    values[vertex] += (float) response[cell];
                    samples[vertex]++;
                    int vx = x + (corner & 1), vy = y + (corner >>> 2), vz = z + (corner >>> 1 & 1);
                    shell[vertex] |= vx == 0 || vy == 0 || vz == 0 || vx == WIDTH || vy == WIDTH || vz == WIDTH;
                }
            }
            double maximum = 0, minimum = Double.POSITIVE_INFINITY;
            for (int index = 0; index < values.length; index++) {
                values[index] = shell[index] ? 0 : values[index] / samples[index];
                maximum = Math.max(maximum, Math.abs(values[index]));
                minimum = Math.min(minimum, values[index]);
            }
            if (!(maximum > 0) || maximum - minimum < maximum * 1e-7) continue;
            for (int index = 0; index < values.length; index++) values[index] /= (float) maximum;
            if (firstValues != null) {
                double firstNorm = 0, secondNorm = 0, product = 0;
                for (int index = 0; index < values.length; index++) {
                    firstNorm += firstValues[index] * firstValues[index];
                    secondNorm += values[index] * values[index];
                    product += firstValues[index] * values[index];
                }
                if (product * product >= 0.999999 * firstNorm * secondNorm) continue;
            }
            shapes.add(new LocalAirShape(minX, minY, minZ, footprint.position, footprint.face, scale,
                    cellCorners, values, handles, revisions));
            firstValues = values;
        }
        return shapes.toArray(LocalAirShape[]::new);
    }

    /** Saved section fragments share current open-face vertices and split at real walls. */
    private LocalAirShape[] restorePieces(Footprint footprint, AirFieldLayout coarse, WorkerPageStore pages) {
        int minX = BlockPos.getX(footprint.position) - LocalAirShape.RADIUS;
        int minY = BlockPos.getY(footprint.position) - LocalAirShape.RADIUS;
        int minZ = BlockPos.getZ(footprint.position) - LocalAirShape.RADIUS;
        boolean[] present = new boolean[CELLS], visited = new boolean[CELLS];
        LinkedHashMap<ThermalPageHandle, Long> dependencies = new LinkedHashMap<>();
        for (int cell = 0; cell < CELLS; cell++) {
            int x = minX + cell % WIDTH, y = minY + cell / (WIDTH * WIDTH), z = minZ + cell / WIDTH % WIDTH;
            var page = pages.find(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
            if (page != null) dependencies.put(page.handle, page.geometryRevision);
            present[cell] = coarse.componentAt(x, y, z) != null;
        }
        ThermalPageHandle[] handles = dependencies.keySet().toArray(ThermalPageHandle[]::new);
        long[] revisions = new long[handles.length];
        int at = 0;
        for (long revision : dependencies.values()) revisions[at++] = revision;
        if (footprint.shapes != null && Arrays.equals(footprint.airGeometry, present)) {
            LocalAirShape[] retained = new LocalAirShape[footprint.shapes.length];
            for (int index = 0; index < retained.length; index++) retained[index] = footprint.shapes[index].withDependencies(handles, revisions);
            return retained;
        }
        footprint.airGeometry = present;
        int[] queue = new int[CELLS];
        ArrayList<LocalAirShape> result = new ArrayList<>();
        for (int seed = 0; seed < CELLS; seed++) {
            if (!present[seed] || visited[seed]) continue;
            boolean[] connected = new boolean[CELLS];
            int head = 0, tail = 0;
            queue[tail++] = seed;
            connected[seed] = visited[seed] = true;
            while (head < tail) {
                int cell = queue[head++];
                for (int face = 0; face < 6; face++) {
                    int neighbor = neighbor(cell, face);
                    if (neighbor >= 0 && present[neighbor] && !visited[neighbor]) {
                        connected[neighbor] = visited[neighbor] = true;
                        queue[tail++] = neighbor;
                    }
                }
            }
            VertexLayout vertices = vertices(connected);
            long branch = BlockPos.asLong(minX + seed % WIDTH, minY + seed / (WIDTH * WIDTH), minZ + seed / WIDTH % WIDTH);
            for (int scale : new int[]{1, 3}) {
                float[] values = new float[vertices.count];
                int[] counts = new int[vertices.count];
                for (int item = 0; item < tail; item++) {
                    int cell = queue[item];
                    int x = minX + cell % WIDTH, y = minY + cell / (WIDTH * WIDTH), z = minZ + cell / WIDTH % WIDTH;
                    LocalAirShape piece = null;
                    for (int savedIndex = footprint.savedPieces.size() - 1; savedIndex >= 0; savedIndex--) {
                        LocalAirShape saved = footprint.savedPieces.get(savedIndex);
                        if (saved.scale == scale && saved.containsCell(x, y, z)) { piece = saved; break; }
                    }
                    if (piece == null) continue;
                    for (int corner = 0; corner < 8; corner++) {
                        int vertex = Short.toUnsignedInt(vertices.corners[cell * 8 + corner]);
                        values[vertex] += (float) piece.value(x, y, z, x + (corner & 1), y + (corner >>> 2), z + (corner >>> 1 & 1));
                        counts[vertex]++;
                    }
                }
                double maximum = 0;
                for (int vertex = 0; vertex < values.length; vertex++) {
                    if (counts[vertex] > 0) values[vertex] /= counts[vertex];
                    maximum = Math.max(maximum, Math.abs(values[vertex]));
                }
                if (maximum > 0) result.add(new LocalAirShape(minX, minY, minZ, footprint.position, footprint.face, scale,
                        vertices.corners, values, handles, revisions).withBranch(branch));
            }
        }
        return result.toArray(LocalAirShape[]::new);
    }

    private static VertexLayout vertices(boolean[] connected) {
        int[] parents = new int[CELLS * 8];
        for (int index = 0; index < parents.length; index++) parents[index] = index;
        for (int cell = 0; cell < CELLS; cell++) {
            if (!connected[cell]) continue;
            for (int face = 1; face < 6; face += 2) {
                int neighbor = neighbor(cell, face);
                if (neighbor < 0 || !connected[neighbor]) continue;
                int bit = face == 1 ? 1 : face == 3 ? 4 : 2;
                for (int corner = 0; corner < 8; corner++) if ((corner & bit) != 0) {
                    int a = root(parents, cell * 8 + corner), b = root(parents, neighbor * 8 + (corner ^ bit));
                    parents[Math.max(a, b)] = Math.min(a, b);
                }
            }
        }
        int[] indices = new int[parents.length];
        Arrays.fill(indices, -1);
        short[] corners = new short[parents.length];
        Arrays.fill(corners, (short) -1);
        int count = 0;
        for (int cell = 0; cell < CELLS; cell++) if (connected[cell]) {
            for (int corner = 0; corner < 8; corner++) {
                int root = root(parents, cell * 8 + corner);
                if (indices[root] < 0) indices[root] = count++;
                corners[cell * 8 + corner] = (short) indices[root];
            }
        }
        return new VertexLayout(corners, count);
    }

    private record VertexLayout(short[] corners, int count) {}

    /** Screened local diffusion prepares a static response; this is not a second running field. */
    private static double[] response(boolean[] air, int source, int scale) {
        double screen = 1.0 / (scale * scale);
        double[] value = new double[CELLS], residual = new double[CELLS], direction = new double[CELLS], applied = new double[CELLS];
        double[] diagonal = new double[CELLS];
        for (int cell = 0; cell < CELLS; cell++) {
            if (!air[cell]) continue;
            diagonal[cell] = screen;
            for (int face = 0; face < 6; face++) {
                int neighbor = neighbor(cell, face);
                if (neighbor < 0) diagonal[cell] += 2;
                else if (air[neighbor]) diagonal[cell]++;
            }
        }
        residual[source] = 1;
        direction[source] = 1 / diagonal[source];
        double product = direction[source];
        for (int iteration = 0; iteration < 256; iteration++) {
            double curvature = 0;
            for (int cell = 0; cell < CELLS; cell++) {
                if (!air[cell]) continue;
                double result = diagonal[cell] * direction[cell];
                for (int face = 0; face < 6; face++) {
                    int neighbor = neighbor(cell, face);
                    if (neighbor >= 0 && air[neighbor]) result -= direction[neighbor];
                }
                applied[cell] = result;
                curvature += direction[cell] * result;
            }
            if (!(curvature > 0)) break;
            double alpha = product / curvature, nextProduct = 0;
            for (int cell = 0; cell < CELLS; cell++) if (air[cell]) {
                value[cell] += alpha * direction[cell];
                residual[cell] -= alpha * applied[cell];
                nextProduct += residual[cell] * residual[cell] / diagonal[cell];
            }
            if (nextProduct < 1e-22) break;
            double beta = nextProduct / product;
            for (int cell = 0; cell < CELLS; cell++) if (air[cell]) direction[cell] = residual[cell] / diagonal[cell] + beta * direction[cell];
            product = nextProduct;
        }
        return value;
    }

    private static int neighbor(int cell, int face) {
        int x = cell % WIDTH + DX[face], y = cell / (WIDTH * WIDTH) + DY[face], z = cell / WIDTH % WIDTH + DZ[face];
        return x < 0 || x >= WIDTH || y < 0 || y >= WIDTH || z < 0 || z >= WIDTH ? -1 : x + WIDTH * (z + WIDTH * y);
    }

    private static int root(int[] parents, int index) {
        while (parents[index] != index) { parents[index] = parents[parents[index]]; index = parents[index]; }
        return index;
    }

    private static final class Footprint {
        final long position;
        final byte face;
        boolean active;
        double amplitudeC;
        double restoredAmplitudeC;
        final ArrayList<LocalAirShape> savedPieces = new ArrayList<>();
        LocalAirShape[] shapes;
        boolean[] airGeometry;
        Footprint(long position, byte face) { this.position = position; this.face = face; }
    }
}
