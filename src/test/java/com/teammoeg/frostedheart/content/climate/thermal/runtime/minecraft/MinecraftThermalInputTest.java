/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPage;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometryDeltaRing;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureResolution;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftThermalInputTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void airNaturallyHasNoGameplayMaterial() {
        assertEquals(null, MinecraftThermalInput.classifyGameplayMaterial(
                Blocks.AIR.defaultBlockState()));
    }

    @Test
    void gameplayRadiationKeepsPlayerAndItemBudgetsIndependent() {
        assertEquals(64,
                MinecraftThermalInput.GAMEPLAY_ITEM_ENVIRONMENT_SAMPLES_PER_TICK);
        assertEquals(128,
                MinecraftThermalInput.GAMEPLAY_RADIATION_PARAMETERS.maximumReceivers());
        assertEquals(64, MinecraftThermalInput.GAMEPLAY_RADIATION_PARAMETERS
                .maximumCandidateVisits());
        assertEquals(8, MinecraftThermalInput.GAMEPLAY_RADIATION_PARAMETERS
                .maximumCandidatesPerReceiver());
        assertEquals(24, MinecraftThermalInput.GAMEPLAY_RADIATION_PARAMETERS
                .maximumRaysPerReceiver());

        assertEquals(64, MinecraftThermalInput.GAMEPLAY_RADIATION_PARAMETERS
                .itemReceiverLimits().maximumReceivers());
        assertEquals(32, MinecraftThermalInput.GAMEPLAY_RADIATION_PARAMETERS
                .itemReceiverLimits().maximumCandidateVisits());
        assertEquals(4, MinecraftThermalInput.GAMEPLAY_RADIATION_PARAMETERS
                .itemReceiverLimits().maximumCandidatesPerReceiver());
        assertEquals(4, MinecraftThermalInput.GAMEPLAY_RADIATION_PARAMETERS
                .itemReceiverLimits().maximumRaysPerReceiver());
    }

    @Test
    void itemEnvironmentCacheIsFixedCapacityTickGenerationState() {
        MinecraftThermalInput.ItemEnvironmentSampleCache cache =
                new MinecraftThermalInput.ItemEnvironmentSampleCache(2);
        assertEquals(2, cache.capacity());
        MinecraftThermalInput.MutableEnvironmentSample first =
                new MinecraftThermalInput.MutableEnvironmentSample();
        first.setFallbackAir(-12.5D, 40L);
        first.setObservationTick(40L);

        assertEquals(-1, cache.find(40L, 1, 2, 3));
        assertTrue(cache.store(1, 2, 3, first));
        assertEquals(0, cache.find(40L, 1, 2, 3));
        MinecraftThermalInput.MutableEnvironmentSample copied =
                new MinecraftThermalInput.MutableEnvironmentSample();
        cache.copyTo(0, copied);
        assertEquals(-12.5D, copied.airTemperatureC());
        assertEquals(40L, copied.sampleTick());
        assertEquals(40L, copied.observationTick());

        assertTrue(cache.store(4, 5, 6, first));
        assertFalse(cache.canAdmit());
        assertFalse(cache.store(7, 8, 9, first));
        assertEquals(2, cache.size());

        assertEquals(-1, cache.find(41L, 1, 2, 3));
        assertEquals(41L, cache.generationTick());
        assertEquals(0, cache.size());
        assertTrue(cache.canAdmit());
        assertTrue(cache.store(7, 8, 9, first));
        cache.close();
        assertEquals(Long.MIN_VALUE, cache.generationTick());
        assertEquals(0, cache.size());
    }

    @Test
    void itemAirBackendKeepsPublicationHitAndFallsBackOnMiss() {
        MinecraftThermalInput.MutableEnvironmentSample published =
                new MinecraftThermalInput.MutableEnvironmentSample();
        published.setAir(8.5D, 7, 0x12, 38L);

        assertTrue(MinecraftThermalInput.selectItemAirBackend(
                -12.5D, 40L, published));
        assertTrue(published.airAvailable());
        assertEquals(8.5D, published.airTemperatureC());
        assertEquals(7, published.mediumId());
        assertEquals(0x12, published.cellFlags());
        assertEquals(38L, published.sampleTick());

        MinecraftThermalInput.MutableEnvironmentSample missed =
                new MinecraftThermalInput.MutableEnvironmentSample();
        assertFalse(MinecraftThermalInput.selectItemAirBackend(
                -12.5D, 40L, missed));
        assertTrue(missed.airAvailable());
        assertEquals(-12.5D, missed.airTemperatureC());
        assertEquals(-1, missed.mediumId());
        assertEquals(0, missed.cellFlags());
        assertEquals(40L, missed.sampleTick());
    }

    @Test
    void resolvedInputRingPreservesPrimitiveEnvelopeAndDoesNotConsumeOnOverflow() {
        ResolvedGeometryInputRing ring = new ResolvedGeometryInputRing(1);
        assertTrue(ring.offerResolvedCenter(
                17L,
                3L,
                5L,
                20L,
                0x123,
                ThermalSignatureResolution.resolved(9)));
        assertFalse(ring.offerResolvedCenter(
                18L,
                4L,
                6L,
                20L,
                0x124,
                ThermalSignatureResolution.resolved(10)));
        assertEquals(1L, ring.latestOfferedWatermark());

        ResolvedGeometryInputRing.MutableInput out =
                new ResolvedGeometryInputRing.MutableInput();
        assertTrue(ring.poll(out));
        assertEquals(
                ResolvedGeometryInputRing.Kind.RESOLVED_CENTER,
                out.kind());
        assertEquals(1L, out.watermark());
        assertEquals(17L, out.sectionKey());
        assertEquals(3L, out.lifecycleGeneration());
        assertEquals(5L, out.geometryRevision());
        assertEquals(20L, out.effectiveTick());
        assertEquals(0x123, out.blockIndex());
        assertEquals(ThermalResolution.Status.RESOLVED, out.status());
        assertEquals(ThermalResolution.Reason.NONE, out.reason());
        assertEquals(9, out.signatureId());
        assertFalse(ring.poll(out));

        int[] fullSnapshot = new int[ResolvedGeometryInputRing.BLOCKS_PER_PAGE];
        fullSnapshot[17] = 23;
        assertTrue(ring.offerFullResync(
                17L,
                3L,
                7L,
                21L,
                ThermalPage.GeometryResyncReason.RING_OVERFLOW,
                fullSnapshot));
        fullSnapshot[17] = 99;
        assertEquals(2L, ring.latestOfferedWatermark());
        assertTrue(ring.poll(out));
        assertEquals(
                ResolvedGeometryInputRing.Kind.FULL_RESYNC_REQUIRED,
                out.kind());
        assertEquals(-1, out.blockIndex());
        assertEquals(ThermalPage.GeometryResyncReason.RING_OVERFLOW,
                out.geometryResyncReason());
        assertEquals(23, out.fullPageSignatureIds()[17]);
    }

    @Test
    void ringRejectsInvalidPrimitiveCoordinates() {
        ResolvedGeometryInputRing ring = new ResolvedGeometryInputRing(1);
        assertThrows(IllegalArgumentException.class, () -> ring.offerResolvedCenter(
                1L,
                1L,
                1L,
                1L,
                4096,
                ThermalSignatureResolution.resolved(0)));
    }

    @Test
    void fullSnapshotsHaveAnIndependentBoundAndRecoverAfterConsumption() {
        ResolvedGeometryInputRing ring = new ResolvedGeometryInputRing(4, 1);
        int[] snapshot = new int[ResolvedGeometryInputRing.BLOCKS_PER_PAGE];
        assertTrue(ring.offerFullResync(
                1L, 1L, 1L, 1L,
                ThermalPage.GeometryResyncReason.EXPLICIT_INVALIDATION,
                snapshot));
        assertFalse(ring.canOfferFullResync());
        assertFalse(ring.offerFullResync(
                2L, 1L, 1L, 1L,
                ThermalPage.GeometryResyncReason.EXPLICIT_INVALIDATION,
                snapshot));
        assertTrue(ring.poll(new ResolvedGeometryInputRing.MutableInput()));
        assertTrue(ring.canOfferFullResync());
    }

    @Test
    void sealedCutLeavesFutureResolvedAndDeltaEntriesQueued() {
        ResolvedGeometryInputRing resolved = new ResolvedGeometryInputRing(2);
        assertTrue(resolved.offerResolvedCenter(
                1L, 1L, 1L, 5L, 0, ThermalSignatureResolution.resolved(0)));
        assertTrue(resolved.offerResolvedCenter(
                1L, 1L, 2L, 6L, 1, ThermalSignatureResolution.resolved(0)));
        ResolvedGeometryInputRing.MutableInput input =
                new ResolvedGeometryInputRing.MutableInput();
        assertTrue(resolved.pollThroughWatermark(1L, input));
        assertFalse(resolved.pollThroughWatermark(1L, input));
        assertTrue(resolved.poll(input));
        assertEquals(2L, input.watermark());
        assertEquals(2L, input.geometryRevision());
        assertFalse(resolved.poll(input));

        GeometryDeltaRing deltas = new GeometryDeltaRing(2);
        assertTrue(deltas.offer(1L, 1L, 1L, 5L, 0));
        assertTrue(deltas.offer(1L, 1L, 2L, 6L, 0));
        GeometryDeltaRing.MutableGeometryDelta delta =
                new GeometryDeltaRing.MutableGeometryDelta();
        assertTrue(deltas.pollThroughTick(5L, delta));
        assertFalse(deltas.pollThroughTick(5L, delta));
        assertTrue(deltas.poll(delta));
        assertEquals(2L, delta.geometryRevision());
        assertEquals(6L, delta.effectiveTick());
        assertFalse(deltas.poll(delta));
    }
}
