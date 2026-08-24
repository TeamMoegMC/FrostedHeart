/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.geometry;

import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VoxelShapeUnitBoxAdapterTest {
    @Test
    void emptyAndFullShapesPreserveTheirBlockLocalMeaning() {
        VoxelShapeUnitBoxAdapter.Adaptation empty =
                VoxelShapeUnitBoxAdapter.adapt(Shapes.empty());
        VoxelShapeUnitBoxAdapter.Adaptation full =
                VoxelShapeUnitBoxAdapter.adapt(Shapes.block());

        assertEquals(VoxelShapeUnitBoxAdapter.Status.RESOLVED, empty.status());
        assertEquals(List.of(), empty.blockers());
        assertEquals(List.of(ConservativeAirGeometry.UnitBox.fullBlock()), full.blockers());
    }

    @Test
    void outOfBlockShapeIsClippedBeforeUnitBoxConstruction() {
        VoxelShape fenceLike = Shapes.box(-0.25D, -0.5D, 0.25D, 1.25D, 1.5D, 0.75D);

        VoxelShapeUnitBoxAdapter.Adaptation adaptation =
                VoxelShapeUnitBoxAdapter.adapt(fenceLike);

        assertEquals(VoxelShapeUnitBoxAdapter.Status.RESOLVED, adaptation.status());
        assertEquals(List.of(new ConservativeAirGeometry.UnitBox(
                0.0D, 0.0D, 0.25D, 1.0D, 1.0D, 0.75D)), adaptation.blockers());
    }

    @Test
    void boxWithNoUnitBlockVolumeIsIgnored() {
        VoxelShape outside = Shapes.box(2.0D, 2.0D, 2.0D, 3.0D, 3.0D, 3.0D);

        VoxelShapeUnitBoxAdapter.Adaptation adaptation =
                VoxelShapeUnitBoxAdapter.adapt(outside);

        assertEquals(VoxelShapeUnitBoxAdapter.Status.RESOLVED, adaptation.status());
        assertEquals(List.of(), adaptation.blockers());
    }

    @Test
    void adaptedShapeNeverManufacturesAnOpenMicrocell() {
        Random random = new Random(0x564f58454c534850L);
        for (int fixture = 0; fixture < 100; fixture++) {
            VoxelShape source = Shapes.empty();
            int boxes = 1 + random.nextInt(6);
            for (int box = 0; box < boxes; box++) {
                double minX = coordinate(random);
                double minY = coordinate(random);
                double minZ = coordinate(random);
                double maxX = minX + extent(random);
                double maxY = minY + extent(random);
                double maxZ = minZ + extent(random);
                source = Shapes.or(source, Shapes.box(minX, minY, minZ, maxX, maxY, maxZ));
            }

            VoxelShapeUnitBoxAdapter.Adaptation adaptation =
                    VoxelShapeUnitBoxAdapter.adapt(source);
            ConservativeAirGeometry.Resolution resolution =
                    ConservativeAirGeometry.resolve(adaptation.blockers(), 64);

            for (int y = 0; y < ConservativeAirGeometry.GRID_SIZE; y++) {
                for (int z = 0; z < ConservativeAirGeometry.GRID_SIZE; z++) {
                    for (int x = 0; x < ConservativeAirGeometry.GRID_SIZE; x++) {
                        if (resolution.componentAt(x, y, z) >= 0) {
                            assertFalse(Shapes.joinIsNotEmpty(
                                    source,
                                    microcell(x, y, z),
                                    BooleanOp.AND
                            ), "proven-open microcell intersects the source shape");
                        }
                    }
                }
            }
        }
    }

    @Test
    void nullShapeIsRejectedAsCallerError() {
        assertThrows(IllegalArgumentException.class,
                () -> VoxelShapeUnitBoxAdapter.adapt(null));
    }

    private static double coordinate(Random random) {
        return (random.nextInt(13) - 2) / 8.0D;
    }

    private static double extent(Random random) {
        return (1 + random.nextInt(8)) / 8.0D;
    }

    private static VoxelShape microcell(int x, int y, int z) {
        double size = 1.0D / ConservativeAirGeometry.GRID_SIZE;
        return Shapes.box(
                x * size,
                y * size,
                z * size,
                (x + 1) * size,
                (y + 1) * size,
                (z + 1) * size
        );
    }
}
