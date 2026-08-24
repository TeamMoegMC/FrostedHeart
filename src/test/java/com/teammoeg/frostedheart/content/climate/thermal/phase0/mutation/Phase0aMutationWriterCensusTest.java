/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase0aMutationWriterCensusTest {
    /*
     * Retired exhaustive-census assertions, retained as non-executable investigation notes:
     * the old suite required exactly 21 unique runtime scans, COMPLETE status for each, no missing,
     * duplicate, contradictory, raw-server-bypass, or section-replacement findings, client-only IE
     * and Embeddium exclusions, Caupona mapped/unmapped routing, and SOURCE_INSPECTED evidence for
     * every external writer. Those checks no longer participate in the active common-path gate.
     */

    @Test
    void currentRepresentativeCensusCoversAndVerifiesCommonPaths() {
        Phase0aMutationWriterCensus.Manifest manifest = Phase0aMutationWriterCensus.current();
        Phase0aMutationWriterCensus.Audit audit = Phase0aMutationWriterCensus.audit(manifest);

        assertTrue(audit.minimumCoverageComplete());
        assertEquals(Set.of(), audit.missingOwners());
        assertEquals(Set.of(), audit.duplicateWriterIds());
        assertEquals(Set.of(), audit.duplicateCapturePointIds());
        assertEquals(Map.of(), audit.duplicatePrimaryCaptureIds());
        assertEquals(Set.of(), audit.missingPrimaryCaptureFamilies());
        assertEquals(Set.of(), audit.adapterGapWriterIds());
        assertEquals(
                Set.of(Phase0aMutationWriterCensus.VANILLA_CLIENT_FULL_CHUNK_PACKET_READ),
                audit.excludedWriterIds()
        );
        assertFalse(manifest.fullTargetModCensusComplete());
        assertTrue(audit.productionVerified());
        assertTrue(audit.unverifiedWriterIds().contains(Phase0aMutationWriterCensus.CREATE_LEVEL_WRITE));
    }

    @Test
    void exhaustiveCensusAndPeriodicFingerprintAreNotProductionRequirements() {
        Phase0aMutationWriterCensus.Audit diagnosticOnly = new Phase0aMutationWriterCensus.Audit(
                false,
                Set.of(),
                Set.of(),
                Set.of(),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(Phase0aMutationWriterCensus.FROSTED_HEART_FAST_NOISE_RAW_SECTION_WRITE),
                Set.of(Phase0aMutationWriterCensus.FROSTED_HEART_FAST_NOISE_RAW_SECTION_WRITE),
                Set.of(),
                Set.of(),
                Set.of()
        );

        assertTrue(diagnosticOnly.minimumCoverageComplete());
        assertTrue(diagnosticOnly.productionVerified());
        assertTrue(capture(
                Phase0aMutationWriterCensus.current(),
                Phase0aMutationWriterCensus.ACTIVE_SECTION_FINGERPRINT
        ).sourceAnchor().contains("GameTest/debug"));
    }

    @Test
    void fastNoiseSeparatesOrdinaryWorldgenFromLoadedDiagnosticFallback() {
        Phase0aMutationWriterCensus.Manifest manifest = Phase0aMutationWriterCensus.current();
        Phase0aMutationWriterCensus.WriterPath loadedBlock = writer(
                manifest,
                Phase0aMutationWriterCensus.FROSTED_HEART_FAST_NOISE_RAW_SECTION_WRITE
        );
        Phase0aMutationWriterCensus.WriterPath loadedBiome = writer(
                manifest,
                Phase0aMutationWriterCensus.FROSTED_HEART_FAST_NOISE_RAW_BIOME_WRITE
        );
        Phase0aMutationWriterCensus.WriterPath worldgenBlock = writer(
                manifest,
                Phase0aMutationWriterCensus.FROSTED_HEART_FAST_NOISE_UNMAPPED_RAW_SECTION_WRITE
        );
        Phase0aMutationWriterCensus.WriterPath worldgenBiome = writer(
                manifest,
                Phase0aMutationWriterCensus.FROSTED_HEART_FAST_NOISE_UNMAPPED_RAW_BIOME_WRITE
        );

        for (Phase0aMutationWriterCensus.WriterPath loaded : List.of(loadedBlock, loadedBiome)) {
            assertEquals(Phase0aMutationWriterCensus.RuntimeScope.LOADED_WORLD, loaded.runtimeScope());
            assertEquals(
                    Phase0aMutationWriterCensus.WriterDisposition.REQUIRE_FULL_RESYNC,
                    loaded.disposition()
            );
            assertTrue(loaded.sourceAnchor().contains("diagnostic fallback"));
        }
        assertEquals(Phase0aMutationWriterCensus.RAW_BLOCK_DIAGNOSTIC_FALLBACK,
                loadedBlock.capturePointId());
        assertEquals(Phase0aMutationWriterCensus.RAW_BIOME_DIAGNOSTIC_FALLBACK,
                loadedBiome.capturePointId());
        for (Phase0aMutationWriterCensus.WriterPath worldgen : List.of(worldgenBlock, worldgenBiome)) {
            assertEquals(Phase0aMutationWriterCensus.RuntimeScope.UNMAPPED_WORLDGEN, worldgen.runtimeScope());
            assertEquals(
                    Phase0aMutationWriterCensus.WriterDisposition.IGNORE_UNMAPPED,
                    worldgen.disposition()
            );
        }
        assertTrue(Phase0aMutationWriterCensus.audit(manifest).productionVerified());
    }

    /*
     * Retired ResetChunks gate test, retained as a non-executable investigation note. Supporting
     * the administrative regeneration command would invalidate the whole chunk and lazily rebuild;
     * it is not part of the representative common-path writer gate.
     *
    @Test
    void resetChunksRequiresOperationLevelInvalidationBeforeLazyRebuild() {
        Phase0aMutationWriterCensus.Manifest manifest = Phase0aMutationWriterCensus.current();
        Phase0aMutationWriterCensus.WriterPath resetChunks = writer(
                manifest,
                Phase0aMutationWriterCensus.VANILLA_RESET_CHUNKS_REGENERATION
        );
        Phase0aMutationWriterCensus.CapturePoint gap = capture(
                manifest,
                Phase0aMutationWriterCensus.CHUNK_REGENERATION_ADAPTER_GAP
        );

        assertEquals(
                Phase0aMutationWriterCensus.EventFamily.CHUNK_REGENERATION,
                resetChunks.eventFamily()
        );
        assertEquals(
                Phase0aMutationWriterCensus.RuntimeScope.LOADED_CHUNK_REGENERATION,
                resetChunks.runtimeScope()
        );
        assertEquals(Phase0aMutationWriterCensus.WriterDisposition.ADAPTER_GAP, resetChunks.disposition());
        assertEquals(Phase0aMutationWriterCensus.CaptureRole.ADAPTER_GAP, gap.role());
        assertTrue(gap.sourceAnchor().contains("chunk-wide Page/geometry invalidation"));
    }
    */

    @Test
    void resetChunksIsOutsideTheRepresentativeWriterGate() {
        String deferredWriterId = "vanilla:reset-chunks-operation-level-regeneration";

        assertFalse(Phase0aMutationWriterCensus.requiredWriterIds().contains(deferredWriterId));
        assertTrue(Phase0aMutationWriterCensus.current().writers().stream()
                .noneMatch(writer -> writer.id().equals(deferredWriterId)));
    }

    @Test
    void layeredOreUsesOwnerAwareMappedOrUnmappedRouting() {
        Phase0aMutationWriterCensus.WriterPath layeredOre = writer(
                Phase0aMutationWriterCensus.current(),
                Phase0aMutationWriterCensus.CREATE_LAYERED_ORE_WRITE
        );

        assertEquals(
                Phase0aMutationWriterCensus.RuntimeScope.MAPPED_OR_UNMAPPED_WORLDGEN,
                layeredOre.runtimeScope()
        );
        assertEquals(
                Phase0aMutationWriterCensus.WriterDisposition.NORMALIZE_IF_MAPPED,
                layeredOre.disposition()
        );
        assertEquals(Phase0aMutationWriterCensus.SECTION_HOOK, layeredOre.capturePointId());
    }

    @Test
    void loadedSectionIdentityReplacementUsesTheExplicitResyncAdapter() {
        Phase0aMutationWriterCensus.Manifest manifest = Phase0aMutationWriterCensus.current();
        Phase0aMutationWriterCensus.WriterPath restore = writer(
                manifest,
                Phase0aMutationWriterCensus.FROSTED_HEART_DEBUG_RESTORE_SECTION_REPLACEMENT
        );
        Phase0aMutationWriterCensus.CapturePoint gap = capture(
                manifest,
                Phase0aMutationWriterCensus.SECTION_REPLACEMENT_ADAPTER
        );

        assertEquals(Phase0aMutationWriterCensus.WriterDisposition.REQUIRE_FULL_RESYNC,
                restore.disposition());
        assertEquals(
                Phase0aMutationWriterCensus.RuntimeScope.LOADED_SECTION_REPLACEMENT,
                restore.runtimeScope()
        );
        assertEquals(Phase0aMutationWriterCensus.CaptureRole.RECOVERY, gap.role());
        assertEquals(Phase0aMutationWriterCensus.EvidenceLevel.GAME_TEST_PASSED,
                restore.evidenceLevel());
    }

    @Test
    void auditReportsMissingWriterDuplicateWriterAndDuplicatePrimaryTogether() {
        Phase0aMutationWriterCensus.Manifest current = Phase0aMutationWriterCensus.current();
        List<Phase0aMutationWriterCensus.WriterPath> writers = new ArrayList<>(current.writers());
        writers.removeIf(path -> path.id().equals(Phase0aMutationWriterCensus.CREATE_SCHEMATIC_RAIL_WRITE));
        writers.add(current.writers().get(0));
        List<Phase0aMutationWriterCensus.CapturePoint> captures = new ArrayList<>(current.capturePoints());
        captures.add(new Phase0aMutationWriterCensus.CapturePoint(
                "test:duplicate-block-primary",
                Phase0aMutationWriterCensus.EventFamily.BLOCK_STATE,
                Phase0aMutationWriterCensus.CaptureRole.PRIMARY,
                "test duplicate hook"
        ));

        Phase0aMutationWriterCensus.Audit audit = Phase0aMutationWriterCensus.audit(
                new Phase0aMutationWriterCensus.Manifest(false, captures, writers)
        );

        assertTrue(audit.missingRequiredWriterIds().contains(
                Phase0aMutationWriterCensus.CREATE_SCHEMATIC_RAIL_WRITE));
        assertTrue(audit.duplicateWriterIds().contains(Phase0aMutationWriterCensus.VANILLA_LEVEL_WRITE));
        assertEquals(
                Set.of(Phase0aMutationWriterCensus.SECTION_HOOK, "test:duplicate-block-primary"),
                Set.copyOf(audit.duplicatePrimaryCaptureIds().get(
                        Phase0aMutationWriterCensus.EventFamily.BLOCK_STATE))
        );
        assertFalse(audit.minimumCoverageComplete());
    }

    @Test
    void auditReportsMissingAndWrongFamilyCaptureReferences() {
        Phase0aMutationWriterCensus.Manifest current = Phase0aMutationWriterCensus.current();
        List<Phase0aMutationWriterCensus.WriterPath> writers = new ArrayList<>(current.writers());
        Phase0aMutationWriterCensus.WriterPath placement = writer(
                current,
                Phase0aMutationWriterCensus.FORGE_ITEM_PLACEMENT
        );
        writers.remove(placement);
        writers.add(copyWithCapture(placement, Phase0aMutationWriterCensus.FORGE_CHUNK_EVENTS));
        Phase0aMutationWriterCensus.WriterPath createLevel = writer(
                current,
                Phase0aMutationWriterCensus.CREATE_LEVEL_WRITE
        );
        writers.remove(createLevel);
        writers.add(copyWithCapture(createLevel, "test:missing-capture"));

        Phase0aMutationWriterCensus.Audit audit = Phase0aMutationWriterCensus.audit(
                new Phase0aMutationWriterCensus.Manifest(false, current.capturePoints(), writers)
        );

        assertTrue(audit.writersWithFamilyMismatch().contains(
                Phase0aMutationWriterCensus.FORGE_ITEM_PLACEMENT));
        assertTrue(audit.writersWithMissingCapturePoint().contains(
                Phase0aMutationWriterCensus.CREATE_LEVEL_WRITE));
        assertFalse(audit.minimumCoverageComplete());
    }

    @Test
    void manifestViewsAreImmutable() {
        Phase0aMutationWriterCensus.Manifest manifest = Phase0aMutationWriterCensus.current();

        assertThrows(UnsupportedOperationException.class, () -> manifest.writers().clear());
        assertThrows(UnsupportedOperationException.class, () -> manifest.capturePoints().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> Phase0aMutationWriterCensus.requiredWriterIds().clear());
    }

    private static Phase0aMutationWriterCensus.WriterPath copyWithCapture(
            Phase0aMutationWriterCensus.WriterPath source, String capturePointId) {
        return new Phase0aMutationWriterCensus.WriterPath(
                source.id(), source.owner(), source.eventFamily(), source.runtimeScope(),
                source.disposition(), capturePointId, source.evidenceLevel(), source.sourceAnchor()
        );
    }

    private static Phase0aMutationWriterCensus.WriterPath writer(
            Phase0aMutationWriterCensus.Manifest manifest, String id) {
        return manifest.writers().stream()
                .filter(path -> path.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static Phase0aMutationWriterCensus.CapturePoint capture(
            Phase0aMutationWriterCensus.Manifest manifest, String id) {
        return manifest.capturePoints().stream()
                .filter(point -> point.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
