/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation;

import com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation.Phase0aMutationWriterCensus.CapturePoint;
import com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation.Phase0aMutationWriterCensus.CaptureRole;
import com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation.Phase0aMutationWriterCensus.EventFamily;
import com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation.Phase0aMutationWriterCensus.Manifest;
import com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation.Phase0aMutationWriterCensus.WriterDisposition;
import com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation.Phase0aMutationWriterCensus.WriterPath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Test-only normalization model retained for the Phase 0a writer fixtures.
 *
 * <p>The current Minecraft probe does not dispatch through this class. It freezes how an adapter
 * must identify its writer/capture point, how same-tick observations coalesce, and which anomalies
 * require a sticky full resync instead of an unsafe delta.
 */
public final class Phase0aMutationAdapterContract {
    public static final String ABSENT = "<absent>";

    private Phase0aMutationAdapterContract() {
    }

    /** Normalizes all observations while retaining every coverage, routing, and continuity issue. */
    public static NormalizationResult normalize(Manifest manifest, List<Observation> observations) {
        Objects.requireNonNull(manifest, "manifest");
        List<Observation> ordered = new ArrayList<>(Objects.requireNonNull(observations, "observations"));
        ordered.sort(Comparator.comparingLong(Observation::sequence)
                .thenComparing(Observation::writerId)
                .thenComparing(Observation::capturePointId));

        Phase0aMutationWriterCensus.Audit censusAudit = Phase0aMutationWriterCensus.audit(manifest);
        Map<String, WriterPath> writerById = firstWritersById(manifest.writers());
        Map<String, CapturePoint> captureById = firstCapturePointsById(manifest.capturePoints());
        Map<EventKey, EventBuilder> builders = new LinkedHashMap<>();
        Set<ResyncTarget> fullResyncTargets = new LinkedHashSet<>();
        List<DuplicateObservation> duplicates = new ArrayList<>();
        List<Discontinuity> discontinuities = new ArrayList<>();
        List<RouteViolation> routeViolations = new ArrayList<>();
        int ignoredObservations = 0;
        int recoveryObservations = 0;

        for (Observation observation : ordered) {
            ResyncTarget target = observation.target();
            WriterPath writer = writerById.get(observation.writerId());
            if (writer == null) {
                routeViolations.add(new RouteViolation(
                        observation, RouteViolationReason.UNKNOWN_WRITER, null));
                fullResyncTargets.add(target);
                continue;
            }

            CapturePoint capturePoint = captureById.get(observation.capturePointId());
            if (capturePoint == null) {
                routeViolations.add(new RouteViolation(
                        observation, RouteViolationReason.UNKNOWN_CAPTURE_POINT, writer.capturePointId()));
                fullResyncTargets.add(target);
                continue;
            }

            boolean routeValid = true;
            if (writer.eventFamily() != observation.eventFamily()) {
                routeViolations.add(new RouteViolation(
                        observation, RouteViolationReason.WRITER_FAMILY_MISMATCH, writer.capturePointId()));
                routeValid = false;
            }
            if (!writer.capturePointId().equals(observation.capturePointId())) {
                routeViolations.add(new RouteViolation(
                        observation, RouteViolationReason.CAPTURE_POINT_MISMATCH, writer.capturePointId()));
                routeValid = false;
            }
            if (capturePoint.eventFamily() != observation.eventFamily()) {
                routeViolations.add(new RouteViolation(
                        observation, RouteViolationReason.CAPTURE_FAMILY_MISMATCH, writer.capturePointId()));
                routeValid = false;
            }
            if (!routeValid) {
                fullResyncTargets.add(target);
                continue;
            }

            if (writer.disposition() == WriterDisposition.IGNORE_UNMAPPED
                    || writer.disposition() == WriterDisposition.OUTSIDE_SERVER_AUTHORITY
                    || (writer.disposition() == WriterDisposition.NORMALIZE_IF_MAPPED
                    && !observation.hasMappedLifecycleOwner())) {
                ignoredObservations++;
                continue;
            }
            if (writer.disposition() == WriterDisposition.ADAPTER_GAP) {
                routeViolations.add(new RouteViolation(
                        observation, RouteViolationReason.KNOWN_ADAPTER_GAP, writer.capturePointId()));
                fullResyncTargets.add(target);
                continue;
            }
            if (writer.disposition() == WriterDisposition.REQUIRE_FULL_RESYNC
                    || capturePoint.role() == CaptureRole.RECOVERY) {
                recoveryObservations++;
                fullResyncTargets.add(target);
                continue;
            }
            if (capturePoint.role() != CaptureRole.PRIMARY) {
                routeViolations.add(new RouteViolation(
                        observation, RouteViolationReason.NON_PRIMARY_NORMALIZATION, writer.capturePointId()));
                fullResyncTargets.add(target);
                continue;
            }
            if (observation.beforeToken().equals(observation.afterToken())) {
                ignoredObservations++;
                continue;
            }

            EventKey key = observation.eventKey();
            EventBuilder builder = builders.get(key);
            if (builder == null) {
                builders.put(key, new EventBuilder(observation));
                continue;
            }

            Transition transition = new Transition(observation.beforeToken(), observation.afterToken());
            if (builder.afterToken.equals(observation.beforeToken())) {
                builder.apply(observation, transition);
                continue;
            }

            Long originalSequence = builder.firstSequenceByTransition.get(transition);
            if (originalSequence != null) {
                duplicates.add(new DuplicateObservation(
                        key, originalSequence, observation.sequence(), observation.writerId()));
                continue;
            }

            discontinuities.add(new Discontinuity(
                    key,
                    builder.afterToken,
                    observation.beforeToken(),
                    observation.sequence(),
                    observation.writerId()
            ));
            builder.markDiscontinuousAndAdvance(observation, transition);
            fullResyncTargets.add(target);
        }

        List<CanonicalEvent> canonicalEvents = new ArrayList<>();
        int netNoopGroups = 0;
        int suppressedCanonicalGroups = 0;
        for (EventBuilder builder : builders.values()) {
            if (builder.discontinuous || fullResyncTargets.contains(builder.key.target())) {
                suppressedCanonicalGroups++;
                continue;
            }
            if (builder.beforeToken.equals(builder.afterToken)) {
                netNoopGroups++;
                continue;
            }
            canonicalEvents.add(builder.build());
        }

        return new NormalizationResult(
                censusAudit,
                canonicalEvents,
                fullResyncTargets,
                duplicates,
                discontinuities,
                routeViolations,
                ignoredObservations,
                recoveryObservations,
                netNoopGroups,
                suppressedCanonicalGroups
        );
    }

    private static Map<String, WriterPath> firstWritersById(List<WriterPath> writers) {
        Map<String, WriterPath> result = new LinkedHashMap<>();
        for (WriterPath writer : writers) {
            result.putIfAbsent(writer.id(), writer);
        }
        return result;
    }

    private static Map<String, CapturePoint> firstCapturePointsById(List<CapturePoint> capturePoints) {
        Map<String, CapturePoint> result = new LinkedHashMap<>();
        for (CapturePoint capturePoint : capturePoints) {
            result.putIfAbsent(capturePoint.id(), capturePoint);
        }
        return result;
    }

    private static String requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public enum RouteViolationReason {
        UNKNOWN_WRITER,
        UNKNOWN_CAPTURE_POINT,
        WRITER_FAMILY_MISMATCH,
        CAPTURE_POINT_MISMATCH,
        CAPTURE_FAMILY_MISMATCH,
        NON_PRIMARY_NORMALIZATION,
        KNOWN_ADAPTER_GAP
    }

    /**
     * Adapter observation. Tokens are stable adapter-owned snapshots, not Java object identities.
     * Subjects use an event-family-specific stable key such as a block position, chunk, or dynamic ID.
     */
    public record Observation(
            long sequence,
            String writerId,
            String capturePointId,
            EventFamily eventFamily,
            String dimensionKey,
            String subjectKey,
            long lifecycleGeneration,
            long effectiveTick,
            String beforeToken,
            String afterToken) {
        public Observation {
            if (sequence <= 0L) {
                throw new IllegalArgumentException("sequence must be positive");
            }
            writerId = requireText("writerId", writerId);
            capturePointId = requireText("capturePointId", capturePointId);
            eventFamily = Objects.requireNonNull(eventFamily, "eventFamily");
            dimensionKey = requireText("dimensionKey", dimensionKey);
            subjectKey = requireText("subjectKey", subjectKey);
            if (lifecycleGeneration < 0L) {
                throw new IllegalArgumentException("lifecycleGeneration must be non-negative");
            }
            if (effectiveTick < 0L) {
                throw new IllegalArgumentException("effectiveTick must be non-negative");
            }
            beforeToken = requireText("beforeToken", beforeToken);
            afterToken = requireText("afterToken", afterToken);
        }

        private EventKey eventKey() {
            return new EventKey(
                    eventFamily,
                    dimensionKey,
                    subjectKey,
                    lifecycleGeneration,
                    effectiveTick
            );
        }

        private ResyncTarget target() {
            return new ResyncTarget(eventFamily, dimensionKey, subjectKey, lifecycleGeneration);
        }

        /** Generation zero is the Phase 0a sentinel for a section with no loaded owner. */
        private boolean hasMappedLifecycleOwner() {
            return lifecycleGeneration > 0L;
        }
    }

    public record EventKey(
            EventFamily eventFamily,
            String dimensionKey,
            String subjectKey,
            long lifecycleGeneration,
            long effectiveTick) {
        private ResyncTarget target() {
            return new ResyncTarget(eventFamily, dimensionKey, subjectKey, lifecycleGeneration);
        }
    }

    public record ResyncTarget(
            EventFamily eventFamily,
            String dimensionKey,
            String subjectKey,
            long lifecycleGeneration) {
    }

    public record CanonicalEvent(
            EventKey key,
            String beforeToken,
            String afterToken,
            long firstSequence,
            long lastSequence,
            int observationCount,
            Set<String> writerIds) {
        public CanonicalEvent {
            writerIds = Set.copyOf(writerIds);
        }
    }

    public record DuplicateObservation(
            EventKey key,
            long originalSequence,
            long duplicateSequence,
            String duplicateWriterId) {
    }

    public record Discontinuity(
            EventKey key,
            String expectedBeforeToken,
            String observedBeforeToken,
            long sequence,
            String writerId) {
    }

    public record RouteViolation(
            Observation observation,
            RouteViolationReason reason,
            String expectedCapturePointId) {
    }

    public record NormalizationResult(
            Phase0aMutationWriterCensus.Audit censusAudit,
            List<CanonicalEvent> canonicalEvents,
            Set<ResyncTarget> fullResyncTargets,
            List<DuplicateObservation> duplicateObservations,
            List<Discontinuity> discontinuities,
            List<RouteViolation> routeViolations,
            int ignoredObservations,
            int recoveryObservations,
            int netNoopGroups,
            int suppressedCanonicalGroups) {
        public NormalizationResult {
            censusAudit = Objects.requireNonNull(censusAudit, "censusAudit");
            canonicalEvents = List.copyOf(canonicalEvents);
            fullResyncTargets = Set.copyOf(fullResyncTargets);
            duplicateObservations = List.copyOf(duplicateObservations);
            discontinuities = List.copyOf(discontinuities);
            routeViolations = List.copyOf(routeViolations);
        }

        public boolean normalizationClean() {
            return censusAudit.minimumCoverageComplete()
                    && duplicateObservations.isEmpty()
                    && discontinuities.isEmpty()
                    && routeViolations.isEmpty()
                    && fullResyncTargets.isEmpty();
        }
    }

    private record Transition(String beforeToken, String afterToken) {
    }

    private static final class EventBuilder {
        private final EventKey key;
        private final String beforeToken;
        private final long firstSequence;
        private final Set<String> writerIds = new LinkedHashSet<>();
        private final Map<Transition, Long> firstSequenceByTransition = new LinkedHashMap<>();
        private String afterToken;
        private long lastSequence;
        private int observationCount;
        private boolean discontinuous;

        private EventBuilder(Observation observation) {
            key = observation.eventKey();
            beforeToken = observation.beforeToken();
            afterToken = observation.afterToken();
            firstSequence = observation.sequence();
            lastSequence = observation.sequence();
            observationCount = 1;
            writerIds.add(observation.writerId());
            firstSequenceByTransition.put(
                    new Transition(observation.beforeToken(), observation.afterToken()),
                    observation.sequence()
            );
        }

        private void apply(Observation observation, Transition transition) {
            firstSequenceByTransition.putIfAbsent(transition, observation.sequence());
            afterToken = observation.afterToken();
            lastSequence = observation.sequence();
            observationCount++;
            writerIds.add(observation.writerId());
        }

        private void markDiscontinuousAndAdvance(Observation observation, Transition transition) {
            discontinuous = true;
            apply(observation, transition);
        }

        private CanonicalEvent build() {
            return new CanonicalEvent(
                    key,
                    beforeToken,
                    afterToken,
                    firstSequence,
                    lastSequence,
                    observationCount,
                    writerIds
            );
        }
    }
}
