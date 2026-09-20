/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;

/** One spatial input identity per finite source face, irrespective of the number of basis terms. */
public final class AirLoadTable {
    private final Long2ObjectOpenHashMap<Load[]> faces = new Long2ObjectOpenHashMap<>();
    private final ArrayList<Load> loads = new ArrayList<>();
    private AirFieldLayout layout;
    private int generation;

    /** Called after the preceding numerical cut consumed all references to its old loads. */
    public void installLayout(AirFieldLayout layout) {
        this.layout = layout;
        generation = Math.incrementExact(generation);
        faces.clear();
        loads.clear();
    }

    public SourceBinding bind(int blockX, int blockY, int blockZ, BlockFace face) {
        AirFieldLayout.Component component = layout == null ? null : layout.componentAt(blockX, blockY, blockZ);
        if (component == null) return SourceBinding.degradedLoss(BlockPos.asLong(blockX, blockY, blockZ));
        long position = BlockPos.asLong(blockX, blockY, blockZ);
        Load[] atBlock = faces.computeIfAbsent(position, ignored -> new Load[6]);
        Load load = atBlock[face.ordinal()];
        if (load == null) {
            double x = blockX + 0.5 + face.stepX() * 0.5;
            double y = blockY + 0.5 + face.stepY() * 0.5;
            double z = blockZ + 0.5 + face.stepZ() * 0.5;
            int[] indices = new int[component.basisCount()];
            double[] weights = new double[component.basisCount()];
            for (int basis = 0; basis < component.basisCount(); basis++) {
                indices[basis] = component.coefficientIndex(basis);
                // A trilinear function's unit-face average is its face-center value.
                weights[basis] = component.weight(basis, x, y, z);
            }
            load = new Load(loads.size(), generation, indices, weights);
            loads.add(load);
            atBlock[face.ordinal()] = load;
        }
        return SourceBinding.airStencil(load.id, load.generation);
    }

    public Load get(int id, int expectedGeneration) {
        if (expectedGeneration != generation || id < 0 || id >= loads.size()) {
            throw new IllegalStateException("Air input no longer belongs to its numerical layout");
        }
        return loads.get(id);
    }

    public static final class Load {
        private final int id, generation;
        private final int[] indices;
        private final double[] weights;

        private Load(int id, int generation, int[] indices, double[] weights) {
            this.id = id;
            this.generation = generation;
            this.indices = indices;
            this.weights = weights;
        }

        public void add(double scalar, double[] destination) {
            for (int index = 0; index < indices.length; index++) destination[indices[index]] += scalar * weights[index];
        }
    }
}
