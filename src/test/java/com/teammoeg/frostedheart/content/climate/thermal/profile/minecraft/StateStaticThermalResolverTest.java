/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StateStaticThermalResolverTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void commonVanillaStaticShapesResolveConservatively() {
        StateStaticThermalResolver resolver = StateStaticThermalResolver.geometryOnly(8);

        ResolvedThermalSignature air = resolved(resolver.resolve(
                Blocks.AIR.defaultBlockState(),
                Fluids.EMPTY.defaultFluidState()));
        ResolvedThermalSignature stone = resolved(resolver.resolve(
                Blocks.STONE.defaultBlockState(),
                Fluids.EMPTY.defaultFluidState()));
        ResolvedThermalSignature slab = resolved(resolver.resolve(
                Blocks.OAK_SLAB.defaultBlockState(),
                Fluids.EMPTY.defaultFluidState()));
        ResolvedThermalSignature fence = resolved(resolver.resolve(
                Blocks.OAK_FENCE.defaultBlockState()
                        .setValue(BlockStateProperties.NORTH, true)
                        .setValue(BlockStateProperties.SOUTH, true),
                Fluids.EMPTY.defaultFluidState()));
        ResolvedThermalSignature stairs = resolved(resolver.resolve(
                Blocks.OAK_STAIRS.defaultBlockState(),
                Fluids.EMPTY.defaultFluidState()));
        ResolvedThermalSignature pane = resolved(resolver.resolve(
                Blocks.GLASS_PANE.defaultBlockState()
                        .setValue(BlockStateProperties.NORTH, true)
                        .setValue(BlockStateProperties.SOUTH, true),
                Fluids.EMPTY.defaultFluidState()));
        ResolvedThermalSignature snow = resolved(resolver.resolve(
                Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1),
                Fluids.EMPTY.defaultFluidState()));

        assertEquals(64, provenAirMicrocells(air));
        assertEquals(0, stone.airGeometry().components().size());
        assertEquals(32, provenAirMicrocells(slab));
        assertEquals(16, provenAirMicrocells(stairs));
        assertEquals(2, fence.airGeometry().components().size());
        assertEquals(2, pane.airGeometry().components().size());
        assertEquals(64, provenAirMicrocells(snow));
    }

    @Test
    void doorAndTrapdoorStoredStateChangesProduceDifferentGeometry() {
        StateStaticThermalResolver resolver = StateStaticThermalResolver.geometryOnly(8);
        BlockState closedDoor = Blocks.OAK_DOOR.defaultBlockState();
        BlockState openDoor = closedDoor.setValue(DoorBlock.OPEN, true);
        BlockState closedTrapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState();
        BlockState openTrapdoor = closedTrapdoor.setValue(TrapDoorBlock.OPEN, true);

        ResolvedThermalSignature closedDoorSignature = resolved(resolver.resolve(
                closedDoor, Fluids.EMPTY.defaultFluidState()));
        ResolvedThermalSignature openDoorSignature = resolved(resolver.resolve(
                openDoor, Fluids.EMPTY.defaultFluidState()));
        ResolvedThermalSignature closedTrapdoorSignature = resolved(resolver.resolve(
                closedTrapdoor, Fluids.EMPTY.defaultFluidState()));
        ResolvedThermalSignature openTrapdoorSignature = resolved(resolver.resolve(
                openTrapdoor, Fluids.EMPTY.defaultFluidState()));

        assertNotEquals(
                closedDoorSignature.airGeometry(), openDoorSignature.airGeometry());
        assertNotEquals(
                closedTrapdoorSignature.airGeometry(),
                openTrapdoorSignature.airGeometry());
    }

    @Test
    void nonEmptyFluidUsesFullBlockConservativeFallback() {
        StateStaticThermalResolver resolver = StateStaticThermalResolver.geometryOnly(8);

        ResolvedThermalSignature waterloggedAir = resolved(resolver.resolve(
                Blocks.AIR.defaultBlockState(),
                Fluids.WATER.defaultFluidState()));

        assertEquals(0, waterloggedAir.airGeometry().components().size());
    }

    @Test
    void movingPistonIsExplicitlyUnresolvedBeforeGenericDynamicCheck() {
        StateStaticThermalResolver resolver = StateStaticThermalResolver.geometryOnly(8);

        ResolvedThermalSignature result = resolver.resolve(
                Blocks.MOVING_PISTON.defaultBlockState(),
                Fluids.EMPTY.defaultFluidState());

        assertNull(result);
    }

    @Test
    void otherDynamicShapeIsConservativelyUnsupported() {
        ResolvedThermalSignature result =
                StateStaticThermalResolver.geometryOnly(8).resolve(
                        Blocks.BAMBOO.defaultBlockState(),
                        Fluids.EMPTY.defaultFluidState());

        assertNull(result);
    }

    @Test
    void fixedEmptyCollisionContextDoesNotInventLiquidCollision() {
        ResolvedThermalSignature signature = resolved(
                StateStaticThermalResolver.geometryOnly(8).resolve(
                        Blocks.WATER.defaultBlockState(),
                        Fluids.EMPTY.defaultFluidState()));

        assertEquals(64, provenAirMicrocells(signature));
    }

    @Test
    void regionLimitRemainsObservable() {
        ResolvedThermalSignature regionOverflow =
                StateStaticThermalResolver.geometryOnly(1).resolve(
                        Blocks.OAK_FENCE_GATE.defaultBlockState(),
                        Fluids.EMPTY.defaultFluidState());

        assertNull(regionOverflow);
    }

    @Test
    void injectedClassifierOwnsMaterialIdentity() {
        StateStaticThermalResolver resolver = new StateStaticThermalResolver(
                8,
                (blockState, fluidState) -> new StateStaticThermalResolver.SignatureMetadata(
                        2, 3)
        );

        ResolvedThermalSignature signature = resolved(resolver.resolve(
                Blocks.AIR.defaultBlockState(),
                Fluids.EMPTY.defaultFluidState()));

        assertEquals(2, signature.materialProfileId());
        assertEquals(3, signature.materialContactPatternId());
    }

    @Test
    void geometryAwareClassifierReusesTheConservativeMaterialMask() {
        StateStaticThermalResolver resolver =
                StateStaticThermalResolver.withMaterialMask(
                        8,
                        (blockState, fluidState, materialMask) ->
                                new StateStaticThermalResolver.SignatureMetadata(
                                        Long.bitCount(materialMask),
                                        materialMask == -1L ? 1 : 2));

        ResolvedThermalSignature stone = resolved(resolver.resolve(
                Blocks.STONE.defaultBlockState(), Fluids.EMPTY.defaultFluidState()));
        ResolvedThermalSignature slab = resolved(resolver.resolve(
                Blocks.OAK_SLAB.defaultBlockState(), Fluids.EMPTY.defaultFluidState()));

        assertEquals(64, stone.materialProfileId());
        assertEquals(1, stone.materialContactPatternId());
        assertEquals(32, slab.materialProfileId());
        assertEquals(2, slab.materialContactPatternId());
    }

    private static ResolvedThermalSignature resolved(
            ResolvedThermalSignature result
    ) {
        assertNotNull(result);
        return result;
    }

    private static int provenAirMicrocells(ResolvedThermalSignature signature) {
        return Long.bitCount(
                signature.airGeometry().provenAirMicrocellMask());
    }
}
