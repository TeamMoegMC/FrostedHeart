/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Cross-thread Page identity, live revision, and publication endpoint. */
public final class ThermalPageHandle {
    public static final int BASE_BRICK_COUNT = 64;

    private final long sectionKey;
    private final long lifecycleGeneration;
    private final AtomicLong liveGeometryRevision = new AtomicLong();
    private final AtomicReference<GeometryResyncToken> resyncRequirement =
            new AtomicReference<>();
    private volatile PagePublication publication = PagePublication.EMPTY;

    public ThermalPageHandle(long sectionKey, long lifecycleGeneration) {
        if (lifecycleGeneration < 0L) {
            throw new IllegalArgumentException(
                    "lifecycleGeneration must be non-negative");
        }
        this.sectionKey = sectionKey;
        this.lifecycleGeneration = lifecycleGeneration;
    }

    public long sectionKey() {
        return sectionKey;
    }

    public long lifecycleGeneration() {
        return lifecycleGeneration;
    }

    public long liveGeometryRevision() {
        return liveGeometryRevision.get();
    }

    /** Makes the current publication stale before deferred final-state capture. */
    public long beginGeometryMutation() {
        long revision = liveGeometryRevision.incrementAndGet();
        GeometryResyncToken existing = resyncRequirement.get();
        if (existing != null) {
            advanceResyncRequirement(existing.reason(), revision);
        }
        return revision;
    }

    public long requireFullGeometryResync(GeometryResyncReason reason) {
        Objects.requireNonNull(reason, "reason");
        long revision = liveGeometryRevision.incrementAndGet();
        advanceResyncRequirement(reason, revision);
        return revision;
    }

    public boolean acknowledgeFullGeometryResync(GeometryResyncToken token) {
        if (!owns(token)) {
            return false;
        }
        GeometryResyncToken current = resyncRequirement.get();
        return current != null
                && current.requiredRevision() == token.requiredRevision()
                && current.reason() == token.reason()
                && liveGeometryRevision.get() == token.requiredRevision()
                && resyncRequirement.compareAndSet(current, null);
    }

    public GeometryResyncToken pendingFullGeometryResync() {
        return resyncRequirement.get();
    }

    /** Worker-only final reference exchange after a prepared topology commit. */
    public void publish(PagePublication next) {
        publication = Objects.requireNonNull(
                next, "Page publication is required");
    }

    /** Returns one stable immutable cut, or {@code null} while capture is stale. */
    public PagePublication currentPublication() {
        long revision = liveGeometryRevision.get();
        PagePublication current = publication;
        return current != PagePublication.EMPTY
                && resyncRequirement.get() == null
                && current.geometryRevision() == revision
                && liveGeometryRevision.get() == revision
                ? current : null;
    }

    /** Last non-empty worker cut for bounded temperature-only fallback/capture. */
    public PagePublication lastPublication() {
        PagePublication current = publication;
        return current == PagePublication.EMPTY ? null : current;
    }

    private boolean owns(GeometryResyncToken token) {
        return token != null
                && token.sectionKey() == sectionKey
                && token.lifecycleGeneration() == lifecycleGeneration;
    }

    private void advanceResyncRequirement(
            GeometryResyncReason reason,
            long revision
    ) {
        while (true) {
            GeometryResyncToken current = resyncRequirement.get();
            GeometryResyncToken next = new GeometryResyncToken(
                    sectionKey,
                    lifecycleGeneration,
                    current == null
                            ? revision
                            : Math.max(revision, current.requiredRevision()),
                    current == null ? reason : current.reason());
            if (resyncRequirement.compareAndSet(current, next)) {
                return;
            }
        }
    }

    public enum GeometryResyncReason {
        CAPTURE_INCOMPLETE,
        SECTION_REPLACED,
        EXPLICIT_INVALIDATION
    }

    public record GeometryResyncToken(
            long sectionKey,
            long lifecycleGeneration,
            long requiredRevision,
            GeometryResyncReason reason
    ) {
        public GeometryResyncToken {
            if (lifecycleGeneration < 0L || requiredRevision <= 0L
                    || reason == null) {
                throw new IllegalArgumentException(
                        "resync token fields are invalid");
            }
        }
    }
}
