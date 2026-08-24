/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase0aMutationAdapterContractTest {
    @Test
    void sameTickTransitionsNormalizeToFirstOldAndFinalNew() {
        Phase0aMutationAdapterContract.NormalizationResult result =
                Phase0aMutationAdapterContract.normalize(
                        Phase0aMutationWriterCensus.current(),
                        List.of(
                                blockObservation(
                                        2L,
                                        Phase0aMutationWriterCensus.VANILLA_SECTION_WRITE,
                                        "block:1,64,1",
                                        40L,
                                        "minecraft:stone",
                                        "minecraft:water"
                                ),
                                blockObservation(
                                        1L,
                                        Phase0aMutationWriterCensus.VANILLA_LEVEL_WRITE,
                                        "block:1,64,1",
                                        40L,
                                        "minecraft:air",
                                        "minecraft:stone"
                                )
                        )
                );

        assertTrue(result.normalizationClean());
        assertEquals(1, result.canonicalEvents().size());
        Phase0aMutationAdapterContract.CanonicalEvent event = result.canonicalEvents().get(0);
        assertEquals("minecraft:air", event.beforeToken());
        assertEquals("minecraft:water", event.afterToken());
        assertEquals(1L, event.firstSequence());
        assertEquals(2L, event.lastSequence());
        assertEquals(2, event.observationCount());
        assertEquals(Set.of(
                Phase0aMutationWriterCensus.VANILLA_LEVEL_WRITE,
                Phase0aMutationWriterCensus.VANILLA_SECTION_WRITE
        ), event.writerIds());
    }

    @Test
    void duplicateCaptureIsReportedWithoutApplyingTheTransitionTwice() {
        Phase0aMutationAdapterContract.NormalizationResult result =
                Phase0aMutationAdapterContract.normalize(
                        Phase0aMutationWriterCensus.current(),
                        List.of(
                                blockObservation(
                                        1L,
                                        Phase0aMutationWriterCensus.FORGE_ITEM_PLACEMENT,
                                        "block:2,64,2",
                                        50L,
                                        "minecraft:air",
                                        "minecraft:oak_door"
                                ),
                                blockObservation(
                                        2L,
                                        Phase0aMutationWriterCensus.VANILLA_LEVEL_WRITE,
                                        "block:2,64,2",
                                        50L,
                                        "minecraft:air",
                                        "minecraft:oak_door"
                                )
                        )
                );

        assertEquals(1, result.canonicalEvents().size());
        assertEquals(1, result.duplicateObservations().size());
        assertEquals(1, result.canonicalEvents().get(0).observationCount());
        assertEquals("minecraft:oak_door", result.canonicalEvents().get(0).afterToken());
        assertFalse(result.normalizationClean());
    }

    @Test
    void discontinuityAndWrongRoutesBecomeObservableResyncTargets() {
        Phase0aMutationAdapterContract.Observation wrongCapture = new Phase0aMutationAdapterContract.Observation(
                3L,
                Phase0aMutationWriterCensus.VANILLA_LEVEL_WRITE,
                Phase0aMutationWriterCensus.ACTIVE_SECTION_FINGERPRINT,
                Phase0aMutationWriterCensus.EventFamily.BLOCK_STATE,
                "minecraft:overworld",
                "block:4,64,4",
                7L,
                60L,
                "minecraft:air",
                "minecraft:stone"
        );
        Phase0aMutationAdapterContract.Observation unknownWriter = new Phase0aMutationAdapterContract.Observation(
                4L,
                "targetmod:unregistered-raw-writer",
                Phase0aMutationWriterCensus.SECTION_HOOK,
                Phase0aMutationWriterCensus.EventFamily.BLOCK_STATE,
                "minecraft:overworld",
                "block:5,64,5",
                7L,
                60L,
                "minecraft:air",
                "minecraft:stone"
        );

        Phase0aMutationAdapterContract.NormalizationResult result =
                Phase0aMutationAdapterContract.normalize(
                        Phase0aMutationWriterCensus.current(),
                        List.of(
                                blockObservation(
                                        1L,
                                        Phase0aMutationWriterCensus.VANILLA_LEVEL_WRITE,
                                        "block:3,64,3",
                                        60L,
                                        "minecraft:air",
                                        "minecraft:stone"
                                ),
                                blockObservation(
                                        2L,
                                        Phase0aMutationWriterCensus.VANILLA_SECTION_WRITE,
                                        "block:3,64,3",
                                        60L,
                                        "minecraft:water",
                                        "minecraft:ice"
                                ),
                                wrongCapture,
                                unknownWriter
                        )
                );

        assertEquals(1, result.discontinuities().size());
        assertEquals("minecraft:stone", result.discontinuities().get(0).expectedBeforeToken());
        assertEquals("minecraft:water", result.discontinuities().get(0).observedBeforeToken());
        assertEquals(2, result.routeViolations().size());
        assertTrue(result.routeViolations().stream().anyMatch(violation ->
                violation.reason()
                        == Phase0aMutationAdapterContract.RouteViolationReason.CAPTURE_POINT_MISMATCH));
        assertTrue(result.routeViolations().stream().anyMatch(violation ->
                violation.reason()
                        == Phase0aMutationAdapterContract.RouteViolationReason.UNKNOWN_WRITER));
        assertEquals(3, result.fullResyncTargets().size());
        assertEquals(0, result.canonicalEvents().size());
        assertEquals(1, result.suppressedCanonicalGroups());
    }

    @Test
    void recoveryAndUnmappedPathsDoNotMasqueradeAsDeltas() {
        Phase0aMutationAdapterContract.Observation rawBypass = new Phase0aMutationAdapterContract.Observation(
                1L,
                Phase0aMutationWriterCensus.PROBE_RAW_PALETTE_SENTINEL,
                Phase0aMutationWriterCensus.ACTIVE_SECTION_FINGERPRINT,
                Phase0aMutationWriterCensus.EventFamily.BLOCK_STATE,
                "minecraft:overworld",
                "section:0,4,0",
                7L,
                70L,
                "fingerprint:1",
                "fingerprint:2"
        );
        Phase0aMutationAdapterContract.Observation worldgen = new Phase0aMutationAdapterContract.Observation(
                2L,
                Phase0aMutationWriterCensus.CREATE_LAYERED_ORE_WRITE,
                Phase0aMutationWriterCensus.SECTION_HOOK,
                Phase0aMutationWriterCensus.EventFamily.BLOCK_STATE,
                "minecraft:overworld",
                "unmapped-section:8,0,8",
                0L,
                70L,
                "minecraft:stone",
                "create:veridium"
        );
        Phase0aMutationAdapterContract.Observation fastNoise = new Phase0aMutationAdapterContract.Observation(
                3L,
                Phase0aMutationWriterCensus.FROSTED_HEART_FAST_NOISE_RAW_SECTION_WRITE,
                Phase0aMutationWriterCensus.RAW_BLOCK_DIAGNOSTIC_FALLBACK,
                Phase0aMutationWriterCensus.EventFamily.BLOCK_STATE,
                "minecraft:overworld",
                "section:1,4,1",
                7L,
                70L,
                "fingerprint:3",
                "fingerprint:4"
        );

        Phase0aMutationAdapterContract.NormalizationResult result =
                Phase0aMutationAdapterContract.normalize(
                        Phase0aMutationWriterCensus.current(),
                        List.of(rawBypass, worldgen, fastNoise)
                );

        assertEquals(0, result.canonicalEvents().size());
        assertEquals(2, result.fullResyncTargets().size());
        assertEquals(2, result.recoveryObservations());
        assertEquals(1, result.ignoredObservations());
        assertEquals(0, result.routeViolations().size());
    }

    @Test
    void layeredOreNormalizesOnlyWhenTheSectionHasALoadedOwner() {
        Phase0aMutationAdapterContract.Observation mapped = new Phase0aMutationAdapterContract.Observation(
                1L,
                Phase0aMutationWriterCensus.CREATE_LAYERED_ORE_WRITE,
                Phase0aMutationWriterCensus.SECTION_HOOK,
                Phase0aMutationWriterCensus.EventFamily.BLOCK_STATE,
                "minecraft:overworld",
                "block:8,64,8",
                7L,
                75L,
                "minecraft:stone",
                "create:veridium"
        );
        Phase0aMutationAdapterContract.Observation unmapped = new Phase0aMutationAdapterContract.Observation(
                2L,
                Phase0aMutationWriterCensus.CREATE_LAYERED_ORE_WRITE,
                Phase0aMutationWriterCensus.SECTION_HOOK,
                Phase0aMutationWriterCensus.EventFamily.BLOCK_STATE,
                "minecraft:overworld",
                "block:128,32,128",
                0L,
                75L,
                "minecraft:stone",
                "create:veridium"
        );

        Phase0aMutationAdapterContract.NormalizationResult result =
                Phase0aMutationAdapterContract.normalize(
                        Phase0aMutationWriterCensus.current(),
                        List.of(mapped, unmapped)
                );

        assertEquals(1, result.canonicalEvents().size());
        assertEquals("block:8,64,8", result.canonicalEvents().get(0).key().subjectKey());
        assertEquals(1, result.ignoredObservations());
        assertEquals(0, result.fullResyncTargets().size());
        assertTrue(result.normalizationClean());
    }

    /*
     * Retired pre-adapter expectation, retained as a non-executable investigation note:
     * section replacement used to report KNOWN_ADAPTER_GAP and normalizationClean=false.
     */

    @Test
    void knownSectionReplacementAdapterRequiresResyncWithoutRouteViolation() {
        Phase0aMutationAdapterContract.Observation replacement =
                new Phase0aMutationAdapterContract.Observation(
                        1L,
                        Phase0aMutationWriterCensus.FROSTED_HEART_DEBUG_RESTORE_SECTION_REPLACEMENT,
                        Phase0aMutationWriterCensus.SECTION_REPLACEMENT_ADAPTER,
                        Phase0aMutationWriterCensus.EventFamily.BLOCK_STATE,
                        "minecraft:overworld",
                        "section:0,4,0",
                        7L,
                        76L,
                        "section-identity:old",
                        "section-identity:new"
                );

        Phase0aMutationAdapterContract.NormalizationResult result =
                Phase0aMutationAdapterContract.normalize(
                        Phase0aMutationWriterCensus.current(),
                        List.of(replacement)
                );

        assertEquals(0, result.canonicalEvents().size());
        assertEquals(1, result.fullResyncTargets().size());
        assertEquals(1, result.recoveryObservations());
        assertEquals(0, result.routeViolations().size());
        assertFalse(result.normalizationClean());
    }

    @Test
    void cleanRoundTripWithinOneTickCollapsesToNetNoop() {
        Phase0aMutationAdapterContract.NormalizationResult result =
                Phase0aMutationAdapterContract.normalize(
                        Phase0aMutationWriterCensus.current(),
                        List.of(
                                blockObservation(
                                        1L,
                                        Phase0aMutationWriterCensus.VANILLA_LEVEL_WRITE,
                                        "block:6,64,6",
                                        80L,
                                        "minecraft:air",
                                        "minecraft:stone"
                                ),
                                blockObservation(
                                        2L,
                                        Phase0aMutationWriterCensus.VANILLA_LEVEL_WRITE,
                                        "block:6,64,6",
                                        80L,
                                        "minecraft:stone",
                                        "minecraft:air"
                                )
                        )
                );

        assertEquals(0, result.canonicalEvents().size());
        assertEquals(1, result.netNoopGroups());
        assertTrue(result.normalizationClean());
    }

    private static Phase0aMutationAdapterContract.Observation blockObservation(
            long sequence,
            String writerId,
            String subject,
            long tick,
            String before,
            String after) {
        return new Phase0aMutationAdapterContract.Observation(
                sequence,
                writerId,
                Phase0aMutationWriterCensus.SECTION_HOOK,
                Phase0aMutationWriterCensus.EventFamily.BLOCK_STATE,
                "minecraft:overworld",
                subject,
                7L,
                tick,
                before,
                after
        );
    }
}
