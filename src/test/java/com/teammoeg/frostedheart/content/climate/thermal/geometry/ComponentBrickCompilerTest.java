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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentBrickCompilerTest {
    @Test
    void fullAirBrickCompilesToOneComponentAndNinetySixPorts() {
        ComponentBrickCompiler.CompiledBrick brick = resolved(compile(
                repeated(air()), 4));

        assertEquals(1, brick.componentCount());
        assertEquals(64.0D, brick.componentVolume(0), 1.0e-12D);
        assertEquals(2.0D, brick.componentCentroidX(0), 1.0e-12D);
        assertEquals(2.0D, brick.componentCentroidY(0), 1.0e-12D);
        assertEquals(2.0D, brick.componentCentroidZ(0), 1.0e-12D);
        assertEquals(96, brick.facePortCount());
        for (ConservativeAirGeometry.Face face : ConservativeAirGeometry.Face.values()) {
            int count = 0;
            for (int port = 0; port < brick.facePortCount(); port++) {
                if (brick.facePortFace(port) == face) {
                    count++;
                }
            }
            assertEquals(16, count);
        }
        for (int port = 0; port < brick.facePortCount(); port++) {
            assertEquals(ConservativeAirGeometry.FULL_FACE_MASK,
                    brick.facePortApertureMask(port));
            assertEquals(0, brick.facePortComponentId(port));
        }
    }

    @Test
    void fullSolidBrickHasNoAtomsComponentsOrPorts() {
        ComponentBrickCompiler.CompiledBrick brick = resolved(compile(
                repeated(solid()), 4));

        assertEquals(0, brick.componentCount());
        assertEquals(0, brick.facePortCount());
        assertEquals(-1, brick.compiledComponentAt(0, 0));
    }

    @Test
    void solidBlockPlaneSeparatesBrickComponents() {
        List<ConservativeAirGeometry.Resolution> blocks = repeated(air());
        for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 4; z++) {
                blocks.set(ComponentBrickCompiler.blockIndex(1, y, z), solid());
            }
        }

        ComponentBrickCompiler.CompiledBrick brick = resolved(compile(blocks, 4));
        int left = brick.compiledComponentAt(ComponentBrickCompiler.blockIndex(0, 0, 0), 0);
        int right = brick.compiledComponentAt(ComponentBrickCompiler.blockIndex(2, 0, 0), 0);

        assertEquals(2, brick.componentCount());
        assertFalse(left == right);
        assertEquals(16.0D, brick.componentVolume(left), 1.0e-12D);
        assertEquals(32.0D, brick.componentVolume(right), 1.0e-12D);
        assertEquals(right,
                brick.compiledComponentAt(ComponentBrickCompiler.blockIndex(3, 3, 3), 0));
    }

    @Test
    void adjacentRegionsJoinOnlyWhenQuarterFaceBitsOverlap() {
        List<ConservativeAirGeometry.Resolution> nonOverlapping = repeated(solid());
        nonOverlapping.set(ComponentBrickCompiler.blockIndex(0, 0, 0), singleMicrocell(3, 0, 0));
        nonOverlapping.set(ComponentBrickCompiler.blockIndex(1, 0, 0), singleMicrocell(0, 1, 0));

        ComponentBrickCompiler.CompiledBrick separated = resolved(compile(nonOverlapping, 4));
        assertEquals(2, separated.componentCount());

        List<ConservativeAirGeometry.Resolution> overlapping = repeated(solid());
        overlapping.set(ComponentBrickCompiler.blockIndex(0, 0, 0), singleMicrocell(3, 0, 0));
        overlapping.set(ComponentBrickCompiler.blockIndex(1, 0, 0), singleMicrocell(0, 0, 0));

        ComponentBrickCompiler.CompiledBrick joined = resolved(compile(overlapping, 4));
        assertEquals(1, joined.componentCount());
        assertEquals(2.0D / 64.0D, joined.componentVolume(0), 1.0e-12D);
        assertEquals(1.0D, joined.componentCentroidX(0), 1.0e-12D);
    }

    @Test
    void facePortCarriesWorldAxisSlotAndLocalAperture() {
        List<ConservativeAirGeometry.Resolution> blocks = repeated(solid());
        blocks.set(ComponentBrickCompiler.blockIndex(0, 2, 3), singleMicrocell(0, 2, 3));

        ComponentBrickCompiler.CompiledBrick brick = resolved(compile(blocks, 4));

        assertEquals(2, brick.facePortCount());
        int negativeX = findPort(
                brick, ConservativeAirGeometry.Face.NEGATIVE_X);
        int positiveZ = findPort(
                brick, ConservativeAirGeometry.Face.POSITIVE_Z);
        assertEquals(11, brick.facePortBlockSlot(negativeX));
        assertEquals(1 << 11, brick.facePortApertureMask(negativeX));
        assertEquals(8, brick.facePortBlockSlot(positiveZ));
        assertEquals(1 << 8, brick.facePortApertureMask(positiveZ));
    }

    @Test
    void unsupportedBlockAndCompilerRegionLimitRemainObservable() {
        ConservativeAirGeometry.UnitBox slab = new ConservativeAirGeometry.UnitBox(
                0.49D, 0.0D, 0.0D, 0.51D, 1.0D, 1.0D);
        List<ConservativeAirGeometry.Resolution> unsupportedBlocks = repeated(air());
        unsupportedBlocks.set(9, ConservativeAirGeometry.resolve(List.of(slab), 1));

        assertNull(compile(unsupportedBlocks, 4));

        List<ConservativeAirGeometry.Resolution> twoRegions = repeated(air());
        twoRegions.set(12, ConservativeAirGeometry.resolve(List.of(slab), 4));
        assertNull(compile(twoRegions, 1));
    }

    @Test
    void invalidBrickInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ComponentBrickCompiler.compileResolved(
                        new ConservativeAirGeometry.Resolution[0],
                        1,
                        new ComponentBrickCompiler.Scratch()));
        assertThrows(IllegalArgumentException.class, () ->
                compile(repeated(air()), 0));
        assertThrows(IllegalArgumentException.class, () ->
                ComponentBrickCompiler.blockIndex(4, 0, 0));
    }

    private static ComponentBrickCompiler.CompiledBrick compile(
            List<ConservativeAirGeometry.Resolution> blocks,
            int maximumRegionsPerBlock
    ) {
        return ComponentBrickCompiler.compileResolved(
                blocks.toArray(ConservativeAirGeometry.Resolution[]::new),
                maximumRegionsPerBlock,
                new ComponentBrickCompiler.Scratch());
    }

    private static ComponentBrickCompiler.CompiledBrick resolved(
            ComponentBrickCompiler.CompiledBrick compilation
    ) {
        assertNotNull(compilation);
        return compilation;
    }

    private static ConservativeAirGeometry.Resolution air() {
        return ConservativeAirGeometry.resolve(List.of(), 4);
    }

    private static ConservativeAirGeometry.Resolution solid() {
        return ConservativeAirGeometry.resolve(
                List.of(ConservativeAirGeometry.UnitBox.fullBlock()), 4);
    }

    private static List<ConservativeAirGeometry.Resolution> repeated(
            ConservativeAirGeometry.Resolution resolution
    ) {
        return new ArrayList<>(java.util.Collections.nCopies(64, resolution));
    }

    private static ConservativeAirGeometry.Resolution singleMicrocell(int openX, int openY, int openZ) {
        List<ConservativeAirGeometry.UnitBox> blockers = new ArrayList<>();
        for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 4; z++) {
                for (int x = 0; x < 4; x++) {
                    if (x == openX && y == openY && z == openZ) {
                        continue;
                    }
                    blockers.add(microcell(x, y, z));
                }
            }
        }
        return ConservativeAirGeometry.resolve(blockers, 1);
    }

    private static ConservativeAirGeometry.UnitBox microcell(int x, int y, int z) {
        double size = 0.25D;
        return new ConservativeAirGeometry.UnitBox(
                x * size, y * size, z * size,
                (x + 1) * size, (y + 1) * size, (z + 1) * size);
    }

    private static int findPort(
            ComponentBrickCompiler.CompiledBrick brick,
            ConservativeAirGeometry.Face face
    ) {
        for (int port = 0; port < brick.facePortCount(); port++) {
            if (brick.facePortFace(port) == face) {
                return port;
            }
        }
        throw new AssertionError("missing face port " + face);
    }
}
