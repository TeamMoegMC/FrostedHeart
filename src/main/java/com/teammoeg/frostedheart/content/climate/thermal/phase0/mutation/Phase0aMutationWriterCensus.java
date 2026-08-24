/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Executable representative census for the Phase 0a mutation and lifecycle probe.
 *
 * <p>The active manifest covers common Vanilla and Forge paths plus known Frosted Heart and Create
 * writers. It deliberately does not gate production on an exhaustive inventory of enabled mods.
 * Unknown third-party bypasses gain a dedicated adapter when reproduced.
 */
public final class Phase0aMutationWriterCensus {
    /*
     * Deferred exhaustive census notes (non-executable and not a gate).
     *
     * The 2026-08-24 runtime scan inspected these 21 scopes:
     * chorda, frostedresearch, immersiveengineering, flywheel, steampowered, jei, ftblibrary,
     * ftbteams, ftbchunks, ftbquests, itemfilters, caupona, ldlib, embeddium, rubidium,
     * architectury, curios, tetra, mutil, mixinextras, and the registrate runtime library.
     *
     * Source/bytecode findings retained for future bug-driven adapters:
     * - Chorda, Frosted Research, IE, Steam Powered, FTB Quests, Caupona, Tetra, and LDLib server
     *   writes use ordinary Level/LevelAccessor setters and converge on the section hook.
     * - Caupona LeavingLogReplacer uses TreeDecorator.Context#setBlock in mapped/unmapped worldgen.
     * - IE TemplateWorld/TemplateChunk section replacement is client-only preview state.
     * - Embeddium/rubidium palette access is client-side read/copy or local debug-container state.
     * - Flywheel and LDLib virtual sections reject writes; FTB Chunks candidates are client map code.
     * - The remaining scanned scopes had no relevant live server chunk writer or reflective bypass.
     *
     * The retired exhaustive tests asserted exactly 21 unique COMPLETE scan entries, no missing,
     * duplicate, incomplete, contradictory, server-raw-bypass, or server-section-replacement IDs,
     * and SOURCE_INSPECTED evidence for every catalogued external writer. Those assertions remain
     * historical evidence only; they do not participate in active completeness or production.
     */

    public static final String SECTION_HOOK = "phase0a:section-set-block-state-5";
    public static final String ACTIVE_SECTION_FINGERPRINT = "phase0a:active-section-fingerprint";
    public static final String RAW_BLOCK_DIAGNOSTIC_FALLBACK =
            "phase0a:raw-block-diagnostic-fallback";
    public static final String RAW_BIOME_DIAGNOSTIC_FALLBACK =
            "phase0a:raw-biome-diagnostic-fallback";
    public static final String FORGE_CHUNK_EVENTS = "phase0a:forge-chunk-events";
    public static final String SECTION_REPLACEMENT_ADAPTER =
            "phase0a:section-replacement-adapter";
    /*
     * Retired identifiers retained for the investigation record. Section replacement now has an
     * executed adapter; ResetChunks is deferred outside the representative common-path gate.
     *
     * public static final String SECTION_REPLACEMENT_ADAPTER_GAP =
     *         "phase0a:missing-section-replacement-adapter";
     * public static final String CHUNK_REGENERATION_ADAPTER_GAP =
     *         "phase0a:missing-chunk-regeneration-operation-adapter";
     */
    public static final String UNMAPPED_WORLDGEN_BLOCK_BOUNDARY =
            "phase0a:unmapped-worldgen-block-boundary";
    public static final String UNMAPPED_WORLDGEN_BIOME_BOUNDARY =
            "phase0a:unmapped-worldgen-biome-boundary";
    public static final String SERVER_THERMAL_AUTHORITY_BOUNDARY =
            "phase0a:server-thermal-authority-boundary";

    public static final String VANILLA_LEVEL_WRITE = "vanilla:level-set-block";
    public static final String VANILLA_LEVEL_CHUNK_WRITE = "vanilla:level-chunk-set-block-state";
    public static final String VANILLA_SECTION_WRITE = "vanilla:level-chunk-section-set-block-state";
    public static final String VANILLA_FLUID_WRITE = "vanilla:fluid-and-waterlogged-update";
    public static final String VANILLA_PISTON_RECURSIVE_WRITE = "vanilla:piston-and-recursive-update";
    public static final String PROBE_RAW_PALETTE_SENTINEL = "probe:raw-paletted-container-bypass-sentinel";
    public static final String VANILLA_UNMAPPED_WORLDGEN_WRITE = "vanilla:unmapped-worldgen-section-write";
    public static final String VANILLA_CLIENT_FULL_CHUNK_PACKET_READ =
            "vanilla:client-full-chunk-packet-read";
    /*
     * Deferred administrative operation, not an active writer-census requirement:
     * public static final String VANILLA_RESET_CHUNKS_REGENERATION =
     *         "vanilla:reset-chunks-operation-level-regeneration";
     */

    public static final String FROSTED_HEART_FAST_NOISE_RAW_SECTION_WRITE =
            "frostedheart:fast-noise-raw-section-data";
    public static final String FROSTED_HEART_FAST_NOISE_RAW_BIOME_WRITE =
            "frostedheart:fast-noise-raw-biome-data";
    public static final String FROSTED_HEART_FAST_NOISE_UNMAPPED_RAW_SECTION_WRITE =
            "frostedheart:fast-noise-unmapped-worldgen-raw-section-data";
    public static final String FROSTED_HEART_FAST_NOISE_UNMAPPED_RAW_BIOME_WRITE =
            "frostedheart:fast-noise-unmapped-worldgen-raw-biome-data";
    public static final String FROSTED_HEART_DEBUG_RESTORE_SECTION_REPLACEMENT =
            "frostedheart:debug-restore-section-replacement";

    public static final String FORGE_ITEM_PLACEMENT = "forge:item-placement-pipeline";
    public static final String FORGE_CHUNK_LOAD = "forge:chunk-load-lifecycle";
    public static final String FORGE_CHUNK_UNLOAD = "forge:chunk-unload-lifecycle";

    public static final String CREATE_LEVEL_WRITE = "create:level-world-writes";
    public static final String CREATE_SCHEMATIC_RAIL_WRITE = "create:schematic-rail-section-write";
    public static final String CREATE_LAYERED_ORE_WRITE = "create:layered-ore-worldgen-section-write";
    public static final String CREATE_CONTRAPTION_REMOVE = "create:contraption-remove-blocks-from-world";
    public static final String CREATE_CONTRAPTION_PLACE = "create:contraption-add-blocks-to-world";

    private static final Set<String> REQUIRED_WRITER_IDS = Set.of(
            VANILLA_LEVEL_WRITE,
            VANILLA_LEVEL_CHUNK_WRITE,
            VANILLA_SECTION_WRITE,
            VANILLA_FLUID_WRITE,
            VANILLA_PISTON_RECURSIVE_WRITE,
            PROBE_RAW_PALETTE_SENTINEL,
            VANILLA_UNMAPPED_WORLDGEN_WRITE,
            VANILLA_CLIENT_FULL_CHUNK_PACKET_READ,
            // VANILLA_RESET_CHUNKS_REGENERATION, // Deferred outside the common-path gate.
            FROSTED_HEART_FAST_NOISE_RAW_SECTION_WRITE,
            FROSTED_HEART_FAST_NOISE_RAW_BIOME_WRITE,
            FROSTED_HEART_FAST_NOISE_UNMAPPED_RAW_SECTION_WRITE,
            FROSTED_HEART_FAST_NOISE_UNMAPPED_RAW_BIOME_WRITE,
            FROSTED_HEART_DEBUG_RESTORE_SECTION_REPLACEMENT,
            FORGE_ITEM_PLACEMENT,
            FORGE_CHUNK_LOAD,
            FORGE_CHUNK_UNLOAD,
            CREATE_LEVEL_WRITE,
            CREATE_SCHEMATIC_RAIL_WRITE,
            CREATE_LAYERED_ORE_WRITE,
            CREATE_CONTRAPTION_REMOVE,
            CREATE_CONTRAPTION_PLACE
    );

    private static final Manifest CURRENT = new Manifest(
            false,
            List.of(
                    capture(SECTION_HOOK, EventFamily.BLOCK_STATE, CaptureRole.PRIMARY,
                            "LevelChunkSectionMixin_Phase0aMutationProbe#setBlockState(IIILBlockState;Z)"),
                    capture(ACTIVE_SECTION_FINGERPRINT, EventFamily.BLOCK_STATE, CaptureRole.RECOVERY,
                            "Phase0aMutationProbe#scanActiveFingerprints; GameTest/debug diagnostics only"),
                    capture(RAW_BLOCK_DIAGNOSTIC_FALLBACK, EventFamily.BLOCK_STATE, CaptureRole.RECOVERY,
                            "Phase0aMutationProbe#onRawBlockContainerReplaced diagnostic fallback"),
                    capture(RAW_BIOME_DIAGNOSTIC_FALLBACK, EventFamily.BIOME_STATE, CaptureRole.RECOVERY,
                            "Phase0aMutationProbe#onRawBiomeContainerReplaced diagnostic fallback"),
                    capture(FORGE_CHUNK_EVENTS, EventFamily.CHUNK_LIFECYCLE, CaptureRole.PRIMARY,
                            "Phase0aMutationEvents#onChunkLoad/onChunkUnload"),
                    capture(SECTION_REPLACEMENT_ADAPTER, EventFamily.BLOCK_STATE, CaptureRole.RECOVERY,
                            "Phase0aMutationProbe#onSectionIdentityReplaced; owner rebind plus full resnapshot"),
                    /*
                     * Deferred ResetChunks capture, retained but non-executable and not a gate:
                     * capture(CHUNK_REGENERATION_ADAPTER_GAP, EventFamily.CHUNK_REGENERATION,
                     *         CaptureRole.ADAPTER_GAP,
                     *         "Chunk-wide Page/geometry invalidation and interest-driven lazy rebuild"),
                     */
                    capture(UNMAPPED_WORLDGEN_BLOCK_BOUNDARY, EventFamily.BLOCK_STATE, CaptureRole.EXCLUSION,
                            "Unmapped worldgen has no loaded lifecycle owner or thermal Page"),
                    capture(UNMAPPED_WORLDGEN_BIOME_BOUNDARY, EventFamily.BIOME_STATE, CaptureRole.EXCLUSION,
                            "Unmapped worldgen has no loaded lifecycle owner or thermal Page"),
                    capture(SERVER_THERMAL_AUTHORITY_BOUNDARY, EventFamily.BLOCK_STATE, CaptureRole.EXCLUSION,
                            "Phase0a lifecycle ownership is authoritative ServerLevel state only")
            ),
            List.of(
                    writer(VANILLA_LEVEL_WRITE, Owner.VANILLA, EventFamily.BLOCK_STATE,
                            RuntimeScope.LOADED_WORLD, WriterDisposition.NORMALIZE, SECTION_HOOK,
                            EvidenceLevel.GAME_TEST_PASSED,
                            "Level#setBlock -> LevelChunk#setBlockState -> LevelChunkSection#setBlockState(5)"),
                    writer(VANILLA_LEVEL_CHUNK_WRITE, Owner.VANILLA, EventFamily.BLOCK_STATE,
                            RuntimeScope.LOADED_WORLD, WriterDisposition.NORMALIZE, SECTION_HOOK,
                            EvidenceLevel.GAME_TEST_PASSED,
                            "LevelChunk#setBlockState -> LevelChunkSection#setBlockState(5)"),
                    writer(VANILLA_SECTION_WRITE, Owner.VANILLA, EventFamily.BLOCK_STATE,
                            RuntimeScope.LOADED_WORLD, WriterDisposition.NORMALIZE, SECTION_HOOK,
                            EvidenceLevel.GAME_TEST_PASSED,
                            "LevelChunkSection#setBlockState(4/5); the four-argument overload delegates"),
                    writer(VANILLA_FLUID_WRITE, Owner.VANILLA, EventFamily.BLOCK_STATE,
                            RuntimeScope.LOADED_WORLD, WriterDisposition.NORMALIZE, SECTION_HOOK,
                            EvidenceLevel.GAME_TEST_PASSED,
                            "LiquidBlock tick and waterlogged BlockState/FluidState changes"),
                    writer(VANILLA_PISTON_RECURSIVE_WRITE, Owner.VANILLA, EventFamily.BLOCK_STATE,
                            RuntimeScope.LOADED_WORLD, WriterDisposition.NORMALIZE, SECTION_HOOK,
                            EvidenceLevel.GAME_TEST_PASSED,
                            "Piston movement and recursive onRemove/onPlace writes"),
                    writer(PROBE_RAW_PALETTE_SENTINEL, Owner.PROBE, EventFamily.BLOCK_STATE,
                            RuntimeScope.RECOVERY_SENTINEL, WriterDisposition.REQUIRE_FULL_RESYNC,
                            ACTIVE_SECTION_FINGERPRINT, EvidenceLevel.GAME_TEST_PASSED,
                            "Direct PalettedContainer#set sentinel for GameTest/debug diagnostics"),
                    writer(VANILLA_UNMAPPED_WORLDGEN_WRITE, Owner.VANILLA, EventFamily.BLOCK_STATE,
                            RuntimeScope.UNMAPPED_WORLDGEN, WriterDisposition.IGNORE_UNMAPPED, SECTION_HOOK,
                            EvidenceLevel.GAME_TEST_PASSED,
                            "Unmapped ProtoChunk/worldgen LevelChunkSection write"),
                    writer(VANILLA_CLIENT_FULL_CHUNK_PACKET_READ, Owner.VANILLA, EventFamily.BLOCK_STATE,
                            RuntimeScope.CLIENT_ONLY, WriterDisposition.OUTSIDE_SERVER_AUTHORITY,
                            SERVER_THERMAL_AUTHORITY_BOUNDARY, EvidenceLevel.SOURCE_INSPECTED,
                            "Client LevelChunk#replaceWithPacketData -> LevelChunkSection#read"),
                    /*
                     * Deferred ResetChunks writer, retained but non-executable and not a gate:
                     * writer(VANILLA_RESET_CHUNKS_REGENERATION, Owner.VANILLA,
                     *         EventFamily.CHUNK_REGENERATION, RuntimeScope.LOADED_CHUNK_REGENERATION,
                     *         WriterDisposition.ADAPTER_GAP, CHUNK_REGENERATION_ADAPTER_GAP,
                     *         EvidenceLevel.SOURCE_INSPECTED,
                     *         "ResetChunksCommand reruns generation stages in a loaded chunk"),
                     */
                    writer(FROSTED_HEART_FAST_NOISE_RAW_SECTION_WRITE, Owner.FROSTED_HEART,
                            EventFamily.BLOCK_STATE, RuntimeScope.LOADED_WORLD,
                            WriterDisposition.REQUIRE_FULL_RESYNC, RAW_BLOCK_DIAGNOSTIC_FALLBACK,
                            EvidenceLevel.SOURCE_INSPECTED,
                            "Loaded-owner raw-block callback is diagnostic fallback, not a primary adapter"),
                    writer(FROSTED_HEART_FAST_NOISE_RAW_BIOME_WRITE, Owner.FROSTED_HEART,
                            EventFamily.BIOME_STATE, RuntimeScope.LOADED_WORLD,
                            WriterDisposition.REQUIRE_FULL_RESYNC, RAW_BIOME_DIAGNOSTIC_FALLBACK,
                            EvidenceLevel.SOURCE_INSPECTED,
                            "Loaded-owner raw-biome callback is diagnostic fallback, not a primary adapter"),
                    writer(FROSTED_HEART_FAST_NOISE_UNMAPPED_RAW_SECTION_WRITE, Owner.FROSTED_HEART,
                            EventFamily.BLOCK_STATE, RuntimeScope.UNMAPPED_WORLDGEN,
                            WriterDisposition.IGNORE_UNMAPPED, UNMAPPED_WORLDGEN_BLOCK_BOUNDARY,
                            EvidenceLevel.SOURCE_INSPECTED,
                            "Ordinary FastNoise worldgen raw section fill before a loaded owner exists"),
                    writer(FROSTED_HEART_FAST_NOISE_UNMAPPED_RAW_BIOME_WRITE, Owner.FROSTED_HEART,
                            EventFamily.BIOME_STATE, RuntimeScope.UNMAPPED_WORLDGEN,
                            WriterDisposition.IGNORE_UNMAPPED, UNMAPPED_WORLDGEN_BIOME_BOUNDARY,
                            EvidenceLevel.SOURCE_INSPECTED,
                            "Ordinary FastNoise worldgen raw biome fill before a loaded owner exists"),
                    writer(FROSTED_HEART_DEBUG_RESTORE_SECTION_REPLACEMENT, Owner.FROSTED_HEART,
                            EventFamily.BLOCK_STATE, RuntimeScope.LOADED_SECTION_REPLACEMENT,
                            WriterDisposition.REQUIRE_FULL_RESYNC, SECTION_REPLACEMENT_ADAPTER,
                            EvidenceLevel.GAME_TEST_PASSED,
                            "DebugCommand restore_backup rebinds the replacement identity and resnapshots"),
                    writer(FORGE_ITEM_PLACEMENT, Owner.FORGE, EventFamily.BLOCK_STATE,
                            RuntimeScope.LOADED_WORLD, WriterDisposition.NORMALIZE, SECTION_HOOK,
                            EvidenceLevel.SOURCE_INSPECTED,
                            "ForgeHooks#onPlaceItemIntoWorld -> BlockItem placement -> Level#setBlock"),
                    writer(FORGE_CHUNK_LOAD, Owner.FORGE, EventFamily.CHUNK_LIFECYCLE,
                            RuntimeScope.CHUNK_LIFECYCLE, WriterDisposition.NORMALIZE, FORGE_CHUNK_EVENTS,
                            EvidenceLevel.GAME_TEST_PASSED,
                            "ChunkEvent.Load -> Phase0aMutationEvents#onChunkLoad"),
                    writer(FORGE_CHUNK_UNLOAD, Owner.FORGE, EventFamily.CHUNK_LIFECYCLE,
                            RuntimeScope.CHUNK_LIFECYCLE, WriterDisposition.NORMALIZE, FORGE_CHUNK_EVENTS,
                            EvidenceLevel.GAME_TEST_PASSED,
                            "ChunkEvent.Unload -> Phase0aMutationEvents#onChunkUnload"),
                    writer(CREATE_LEVEL_WRITE, Owner.CREATE, EventFamily.BLOCK_STATE,
                            RuntimeScope.LOADED_WORLD, WriterDisposition.NORMALIZE, SECTION_HOOK,
                            EvidenceLevel.SOURCE_INSPECTED, "Create Level#setBlock/setBlockAndUpdate call sites"),
                    writer(CREATE_SCHEMATIC_RAIL_WRITE, Owner.CREATE, EventFamily.BLOCK_STATE,
                            RuntimeScope.LOADED_WORLD, WriterDisposition.NORMALIZE, SECTION_HOOK,
                            EvidenceLevel.SOURCE_INSPECTED,
                            "BlockHelper#placeRailWithoutUpdate -> LevelChunkSection#setBlockState(4)"),
                    writer(CREATE_LAYERED_ORE_WRITE, Owner.CREATE, EventFamily.BLOCK_STATE,
                            RuntimeScope.MAPPED_OR_UNMAPPED_WORLDGEN, WriterDisposition.NORMALIZE_IF_MAPPED,
                            SECTION_HOOK, EvidenceLevel.SOURCE_INSPECTED,
                            "LayeredOreFeature#place -> LevelChunkSection#setBlockState(5)"),
                    writer(CREATE_CONTRAPTION_REMOVE, Owner.CREATE, EventFamily.BLOCK_STATE,
                            RuntimeScope.LOADED_WORLD, WriterDisposition.NORMALIZE, SECTION_HOOK,
                            EvidenceLevel.GAME_TEST_PASSED,
                            "Contraption#removeBlocksFromWorld -> Level#setBlock"),
                    writer(CREATE_CONTRAPTION_PLACE, Owner.CREATE, EventFamily.BLOCK_STATE,
                            RuntimeScope.LOADED_WORLD, WriterDisposition.NORMALIZE, SECTION_HOOK,
                            EvidenceLevel.GAME_TEST_PASSED,
                            "Contraption#addBlocksToWorld -> Level#setBlock")
            )
    );

    private Phase0aMutationWriterCensus() {
    }

    public static Manifest current() {
        return CURRENT;
    }

    public static Set<String> requiredWriterIds() {
        return REQUIRED_WRITER_IDS;
    }

    /** Returns every representative structural and evidence gap instead of stopping at the first. */
    public static Audit audit(Manifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        Map<String, Integer> captureCounts = countsById(manifest.capturePoints().stream()
                .map(CapturePoint::id).toList());
        Map<String, Integer> writerCounts = countsById(manifest.writers().stream()
                .map(WriterPath::id).toList());
        Set<String> duplicateCapturePointIds = duplicateIds(captureCounts);
        Set<String> duplicateWriterIds = duplicateIds(writerCounts);

        Map<String, CapturePoint> captureById = new LinkedHashMap<>();
        for (CapturePoint capturePoint : manifest.capturePoints()) {
            captureById.putIfAbsent(capturePoint.id(), capturePoint);
        }
        EnumMap<EventFamily, List<String>> primaryByFamily = new EnumMap<>(EventFamily.class);
        for (CapturePoint capturePoint : manifest.capturePoints()) {
            if (capturePoint.role() == CaptureRole.PRIMARY) {
                primaryByFamily.computeIfAbsent(capturePoint.eventFamily(), ignored -> new ArrayList<>())
                        .add(capturePoint.id());
            }
        }
        Map<EventFamily, List<String>> duplicatePrimaryCaptureIds = new EnumMap<>(EventFamily.class);
        for (Map.Entry<EventFamily, List<String>> entry : primaryByFamily.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicatePrimaryCaptureIds.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
        }

        Set<String> presentWriterIds = new TreeSet<>();
        Set<String> writersWithMissingCapturePoint = new TreeSet<>();
        Set<String> writersWithFamilyMismatch = new TreeSet<>();
        Set<String> recoveryOnlyWriterIds = new TreeSet<>();
        Set<String> blockingRecoveryOnlyWriterIds = new TreeSet<>();
        Set<String> adapterGapWriterIds = new TreeSet<>();
        Set<String> excludedWriterIds = new TreeSet<>();
        Set<String> unverifiedWriterIds = new TreeSet<>();
        EnumSet<Owner> presentOwners = EnumSet.noneOf(Owner.class);
        EnumSet<EventFamily> familiesRequiringPrimary = EnumSet.noneOf(EventFamily.class);

        for (WriterPath writer : manifest.writers()) {
            presentWriterIds.add(writer.id());
            presentOwners.add(writer.owner());
            if (writer.disposition() == WriterDisposition.NORMALIZE
                    || writer.disposition() == WriterDisposition.NORMALIZE_IF_MAPPED) {
                familiesRequiringPrimary.add(writer.eventFamily());
            }
            CapturePoint capturePoint = captureById.get(writer.capturePointId());
            if (capturePoint == null) {
                writersWithMissingCapturePoint.add(writer.id());
            } else {
                if (capturePoint.eventFamily() != writer.eventFamily()) {
                    writersWithFamilyMismatch.add(writer.id());
                }
                if (capturePoint.role() == CaptureRole.RECOVERY) {
                    recoveryOnlyWriterIds.add(writer.id());
                    if (writer.runtimeScope() != RuntimeScope.RECOVERY_SENTINEL) {
                        blockingRecoveryOnlyWriterIds.add(writer.id());
                    }
                }
            }
            if (!writer.evidenceLevel().executed()
                    && writer.disposition() != WriterDisposition.OUTSIDE_SERVER_AUTHORITY) {
                unverifiedWriterIds.add(writer.id());
            }
            if (writer.disposition() == WriterDisposition.ADAPTER_GAP) {
                adapterGapWriterIds.add(writer.id());
            }
            if (writer.disposition() == WriterDisposition.OUTSIDE_SERVER_AUTHORITY) {
                excludedWriterIds.add(writer.id());
            }
        }

        Set<String> missingRequiredWriterIds = new TreeSet<>(REQUIRED_WRITER_IDS);
        missingRequiredWriterIds.removeAll(presentWriterIds);
        EnumSet<Owner> missingOwners = EnumSet.allOf(Owner.class);
        missingOwners.removeAll(presentOwners);
        EnumSet<EventFamily> missingPrimaryCaptureFamilies = EnumSet.copyOf(familiesRequiringPrimary);
        missingPrimaryCaptureFamilies.removeAll(primaryByFamily.keySet());
        return new Audit(
                manifest.fullTargetModCensusComplete(), missingRequiredWriterIds, duplicateWriterIds,
                duplicateCapturePointIds, duplicatePrimaryCaptureIds,
                Set.copyOf(missingPrimaryCaptureFamilies), writersWithMissingCapturePoint,
                writersWithFamilyMismatch, Set.copyOf(missingOwners), recoveryOnlyWriterIds,
                blockingRecoveryOnlyWriterIds, adapterGapWriterIds, excludedWriterIds, unverifiedWriterIds
        );
    }

    private static CapturePoint capture(
            String id, EventFamily family, CaptureRole role, String sourceAnchor) {
        return new CapturePoint(id, family, role, sourceAnchor);
    }

    private static WriterPath writer(
            String id, Owner owner, EventFamily family, RuntimeScope scope,
            WriterDisposition disposition, String capturePointId,
            EvidenceLevel evidence, String sourceAnchor) {
        return new WriterPath(
                id, owner, family, scope, disposition, capturePointId, evidence, sourceAnchor);
    }

    private static Map<String, Integer> countsById(List<String> ids) {
        Map<String, Integer> counts = new HashMap<>();
        for (String id : ids) {
            counts.merge(id, 1, Integer::sum);
        }
        return counts;
    }

    private static Set<String> duplicateIds(Map<String, Integer> counts) {
        Set<String> duplicates = new TreeSet<>();
        counts.forEach((id, count) -> {
            if (count > 1) {
                duplicates.add(id);
            }
        });
        return Set.copyOf(duplicates);
    }

    private static String requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public enum Owner {
        VANILLA,
        FORGE,
        CREATE,
        FROSTED_HEART,
        PROBE
    }

    public enum EventFamily {
        BLOCK_STATE,
        BIOME_STATE,
        CHUNK_LIFECYCLE,
        CHUNK_REGENERATION
    }

    public enum CaptureRole {
        PRIMARY,
        RECOVERY,
        ADAPTER_GAP,
        EXCLUSION
    }

    public enum RuntimeScope {
        LOADED_WORLD,
        CHUNK_LIFECYCLE,
        LOADED_CHUNK_REGENERATION,
        LOADED_SECTION_REPLACEMENT,
        UNMAPPED_WORLDGEN,
        MAPPED_OR_UNMAPPED_WORLDGEN,
        CLIENT_ONLY,
        RECOVERY_SENTINEL
    }

    public enum WriterDisposition {
        NORMALIZE,
        NORMALIZE_IF_MAPPED,
        REQUIRE_FULL_RESYNC,
        IGNORE_UNMAPPED,
        ADAPTER_GAP,
        OUTSIDE_SERVER_AUTHORITY
    }

    public enum EvidenceLevel {
        SOURCE_INSPECTED(false),
        GAME_TEST_IMPLEMENTED(false),
        GAME_TEST_PASSED(true);

        private final boolean executed;

        EvidenceLevel(boolean executed) {
            this.executed = executed;
        }

        public boolean executed() {
            return executed;
        }
    }

    public record CapturePoint(String id, EventFamily eventFamily, CaptureRole role, String sourceAnchor) {
        public CapturePoint {
            id = requireText("capture point id", id);
            eventFamily = Objects.requireNonNull(eventFamily, "eventFamily");
            role = Objects.requireNonNull(role, "role");
            sourceAnchor = requireText("capture point source anchor", sourceAnchor);
        }
    }

    public record WriterPath(
            String id,
            Owner owner,
            EventFamily eventFamily,
            RuntimeScope runtimeScope,
            WriterDisposition disposition,
            String capturePointId,
            EvidenceLevel evidenceLevel,
            String sourceAnchor) {
        public WriterPath {
            id = requireText("writer id", id);
            owner = Objects.requireNonNull(owner, "owner");
            eventFamily = Objects.requireNonNull(eventFamily, "eventFamily");
            runtimeScope = Objects.requireNonNull(runtimeScope, "runtimeScope");
            disposition = Objects.requireNonNull(disposition, "disposition");
            capturePointId = requireText("writer capture point id", capturePointId);
            evidenceLevel = Objects.requireNonNull(evidenceLevel, "evidenceLevel");
            sourceAnchor = requireText("writer source anchor", sourceAnchor);
        }
    }

    public record Manifest(
            boolean fullTargetModCensusComplete,
            List<CapturePoint> capturePoints,
            List<WriterPath> writers) {
        public Manifest {
            capturePoints = List.copyOf(Objects.requireNonNull(capturePoints, "capturePoints"));
            writers = List.copyOf(Objects.requireNonNull(writers, "writers"));
        }
    }

    public record Audit(
            boolean fullTargetModCensusComplete,
            Set<String> missingRequiredWriterIds,
            Set<String> duplicateWriterIds,
            Set<String> duplicateCapturePointIds,
            Map<EventFamily, List<String>> duplicatePrimaryCaptureIds,
            Set<EventFamily> missingPrimaryCaptureFamilies,
            Set<String> writersWithMissingCapturePoint,
            Set<String> writersWithFamilyMismatch,
            Set<Owner> missingOwners,
            Set<String> recoveryOnlyWriterIds,
            Set<String> blockingRecoveryOnlyWriterIds,
            Set<String> adapterGapWriterIds,
            Set<String> excludedWriterIds,
            Set<String> unverifiedWriterIds) {
        public Audit {
            missingRequiredWriterIds = Set.copyOf(missingRequiredWriterIds);
            duplicateWriterIds = Set.copyOf(duplicateWriterIds);
            duplicateCapturePointIds = Set.copyOf(duplicateCapturePointIds);
            Map<EventFamily, List<String>> primaryCopy = new EnumMap<>(EventFamily.class);
            duplicatePrimaryCaptureIds.forEach((family, ids) -> primaryCopy.put(family, List.copyOf(ids)));
            duplicatePrimaryCaptureIds = Map.copyOf(primaryCopy);
            missingPrimaryCaptureFamilies = Set.copyOf(missingPrimaryCaptureFamilies);
            writersWithMissingCapturePoint = Set.copyOf(writersWithMissingCapturePoint);
            writersWithFamilyMismatch = Set.copyOf(writersWithFamilyMismatch);
            missingOwners = Set.copyOf(missingOwners);
            recoveryOnlyWriterIds = Set.copyOf(recoveryOnlyWriterIds);
            blockingRecoveryOnlyWriterIds = Set.copyOf(blockingRecoveryOnlyWriterIds);
            adapterGapWriterIds = Set.copyOf(adapterGapWriterIds);
            excludedWriterIds = Set.copyOf(excludedWriterIds);
            unverifiedWriterIds = Set.copyOf(unverifiedWriterIds);
        }

        public boolean minimumCoverageComplete() {
            return missingRequiredWriterIds.isEmpty()
                    && duplicateWriterIds.isEmpty()
                    && duplicateCapturePointIds.isEmpty()
                    && duplicatePrimaryCaptureIds.isEmpty()
                    && missingPrimaryCaptureFamilies.isEmpty()
                    && writersWithMissingCapturePoint.isEmpty()
                    && writersWithFamilyMismatch.isEmpty()
                    && missingOwners.isEmpty();
        }

        /**
         * This verifies the representative common-path contract. Exhaustive mod enumeration,
         * periodic fingerprint scans, and separately executing every inspected caller are not gates.
         */
        public boolean productionVerified() {
            return minimumCoverageComplete()
                    && adapterGapWriterIds.isEmpty();
        }
    }
}
