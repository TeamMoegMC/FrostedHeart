/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;

/** One spatial input identity per finite source face, irrespective of the number of basis terms. */
public final class AirLoadTable {
    private final Long2ObjectOpenHashMap<Load[]> faces = new Long2ObjectOpenHashMap<>();
    private final ArrayList<Load> loads = new ArrayList<>();
    private final it.unimi.dsi.fastutil.ints.IntArrayList free = new it.unimi.dsi.fastutil.ints.IntArrayList();
    private final it.unimi.dsi.fastutil.ints.IntArrayList versions = new it.unimi.dsi.fastutil.ints.IntArrayList();
    private final java.util.ArrayDeque<Load> unusedWorldBindings = new java.util.ArrayDeque<>();
    private static final int[] NO_INDICES = new int[0];
    private static final double[] NO_WEIGHTS = new double[0];
    private int faceHighWater, loadHighWater;

    public void recordBacking(AirBackingMeter meter) {
        meter.container(faces, faceHighWater, 16);
        meter.container(loads, loadHighWater, 8);
        meter.container(unusedWorldBindings, loadHighWater, 8);
        meter.array(free.elements()); meter.array(versions.elements());
        for (Load[] atBlock : faces.values()) meter.array(atBlock);
        for (Load load : loads) {
            if (load == null) continue;
            meter.objectOnce(load, 96);
            meter.array(load.indices);
            meter.array(load.weights);
        }
    }

    public SourceBinding bindWorldIdentity(int x, int y, int z, BlockFace face, int pageGeneration) {
        return bindWorldFace(x, y, z, face, pageGeneration);
    }

    private static Load compileLoad(int id, int generation, AirFieldLayout owner, long block, BlockFace face) {
        int blockX = BlockPos.getX(block), blockY = BlockPos.getY(block), blockZ = BlockPos.getZ(block);
        var component = owner.componentAt(blockX, blockY, blockZ);
        double x = blockX + .5 + face.stepX() * .5, y = blockY + .5 + face.stepY() * .5,
                z = blockZ + .5 + face.stepZ() * .5;
        int[] indices = new int[component.basisCount()]; double[] weights = new double[indices.length];
        for (int basis = 0; basis < indices.length; basis++) {
            indices[basis] = component.coefficientIndex(basis);
            // A trilinear function's unit-face average is its face-center value.
            weights[basis] = component.weight(basis, x, y, z);
        }
        return new Load(id, generation, indices, weights, owner, block, face);
    }

    /** Numerical addresses exist only for a frozen regional execution. */
    public static Load compileWorldFace(AirFieldLayout layout, long block, BlockFace face) {
        if (layout == null || layout.componentAt(BlockPos.getX(block), BlockPos.getY(block), BlockPos.getZ(block)) == null)
            return null;
        return compileLoad(0, 0, layout, block, face);
    }

    public Load get(int id, int expectedGeneration) {
        if (id < 0 || id >= loads.size() || loads.get(id) == null
                || loads.get(id).generation != expectedGeneration) {
            throw new IllegalStateException("Air input identity has retired");
        }
        return loads.get(id);
    }

    private SourceBinding bindWorldFace(int x, int y, int z, BlockFace face, int pageGeneration) {
        long position = BlockPos.asLong(x, y, z);
        var atBlock = faces.computeIfAbsent(position, ignored -> new Load[6]);
        faceHighWater = Math.max(faceHighWater, faces.size());
        int key = face.ordinal();
        var load = atBlock[key];
        if (load == null || load.pageGeneration != pageGeneration) {
            int id;
            if (free.isEmpty()) { id = loads.size(); loads.add(null); versions.add(0); }
            else id = free.removeInt(free.size() - 1);
            int version = Math.incrementExact(versions.getInt(id)); versions.set(id, version);
            load = new Load(id, version, NO_INDICES, NO_WEIGHTS, null, position, face, pageGeneration);
            loads.set(id, load); atBlock[key] = load;
            loadHighWater = Math.max(loadHighWater, loads.size());
        }
        return SourceBinding.airStencil(load.id, load.generation);
    }

    public void retainWorldBinding(SourceBinding binding) {
        if (binding != null && binding.kind() == SourceBinding.Kind.AIR_STENCIL)
            get((int) binding.targetId(), binding.lifecycleGeneration()).references++;
    }
    public void releaseWorldBinding(SourceBinding binding) {
        if (binding == null || binding.kind() != SourceBinding.Kind.AIR_STENCIL) return;
        var load = get((int) binding.targetId(), binding.lifecycleGeneration());
        if (--load.references < 0) throw new IllegalStateException("Air binding reference underflow");
        if (load.references == 0 && !load.queuedUnused) { load.queuedUnused = true; unusedWorldBindings.addLast(load); }
    }
    public void reclaimWorldBindings(int maximum) {
        while (maximum-- > 0 && !unusedWorldBindings.isEmpty()) {
            var load = unusedWorldBindings.removeFirst(); load.queuedUnused = false;
            if (load.references != 0 || loads.get(load.id) != load) continue;
            retireWorldBinding(load);
        }
    }
    private void retireWorldBinding(Load load) {
        var atBlock = faces.get(load.block);
        int key = load.face.ordinal();
        if (atBlock != null && atBlock[key] == load) atBlock[key] = null;
        boolean any = false;
        if (atBlock != null) for (var candidate : atBlock) if (candidate != null) { any = true; break; }
        if (!any) faces.remove(load.block);
        loads.set(load.id, null); free.add(load.id);
    }

    public static final class Load {
        private final int id, generation, pageGeneration;
        private final int[] indices;
        private final double[] weights;
        private final AirFieldLayout owner;
        private final long block;
        private final BlockFace face;
        private int references;
        private boolean queuedUnused;

        private Load(int id, int generation, int[] indices, double[] weights, AirFieldLayout owner,
                long block, BlockFace face) {
            this(id, generation, indices, weights, owner, block, face, 0);
        }

        private Load(int id, int generation, int[] indices, double[] weights, AirFieldLayout owner,
                long block, BlockFace face, int pageGeneration) {
            this.id = id;
            this.generation = generation;
            this.indices = indices;
            this.weights = weights;
            this.owner = owner;
            this.block = block;
            this.face = face;
            this.pageGeneration = pageGeneration;
        }

        public AirFieldLayout owner() { return owner; }
        public long block() { return block; }
        public BlockFace face() { return face; }
        public int pageGeneration() { return pageGeneration; }

        public void recordBacking(AirBackingMeter meter) {
            meter.array(indices); meter.array(weights);
            if (owner != null) owner.recordBacking(meter);
        }

        public void add(double scalar, double[] destination) {
            for (int index = 0; index < indices.length; index++) destination[indices[index]] += scalar * weights[index];
        }

        public void addLumped(double joules, double[] massJPerK, double[] temperatures) {
            for (int index = 0; index < indices.length; index++)
                temperatures[indices[index]] += joules * weights[index] / massJPerK[indices[index]];
        }
    }
}
