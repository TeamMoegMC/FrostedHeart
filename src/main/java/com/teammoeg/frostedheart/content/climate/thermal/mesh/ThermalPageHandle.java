/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
    // A position in the full capture history, not a separate revision sequence.
    private volatile long lastPhysicalGeometryRevision;
    private volatile long firstStaleGeometryTick = -1L;
    private final AtomicReference<GeometryResyncToken> resyncRequirement =
            new AtomicReference<>();
    private volatile PagePublication publication = PagePublication.EMPTY;
    private volatile PagePublication previousPublication = PagePublication.EMPTY;
    private record StagedPublication(com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token,
            PagePublication previous, PagePublication next) { }
    private volatile StagedPublication stagedPublication;

    private PagePublication visiblePublication() {
        var staged = stagedPublication;
        return staged == null ? publication : staged.token.committed() ? staged.next : staged.previous;
    }
    public void stagePublication(com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token, PagePublication next) {
        if (stagedPublication != null) throw new IllegalStateException("Page already owns a topology exchange");
        stagedPublication = new StagedPublication(token, publication, next);
    }
    public void finishPublication(com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token) {
        var staged = stagedPublication;
        if (staged == null || staged.token != token) return;
        if (token.committed()) {
            previousPublication = staged.next == PagePublication.EMPTY ? PagePublication.EMPTY : staged.previous;
            publication = staged.next;
        }
        stagedPublication = null;
    }
    private final AtomicReference<com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation> inputCredit = new AtomicReference<>();

    public void inputCredit(com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation credit) {
        if (credit != null && !inputCredit.compareAndSet(null, credit)) credit.close();
    }
    public com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation takeInputCredit() {
        return inputCredit.getAndSet(null);
    }
    public void releaseInputCredit() {
        var credit = takeInputCredit();
        if (credit != null) credit.close();
    }

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

    public long lastPhysicalGeometryRevision() { return lastPhysicalGeometryRevision; }

    public long firstStaleGeometryTick() {
        return firstStaleGeometryTick;
    }

    /** Makes the current publication stale before deferred final-state capture. */
    public long beginGeometryMutation() {
        return beginGeometryMutation(-1L, true);
    }

    /** Every state capture advances; only actual physical changes invalidate existing Air. */
    public synchronized long beginGeometryMutation(long gameTick, boolean physicalChange) {
        long revision = advanceGeometryRevision(physicalChange);
        var visible = visiblePublication();
        if (gameTick >= 0L && visible != PagePublication.EMPTY
                && visible.geometryRevision() == revision - 1L)
            firstStaleGeometryTick = gameTick;
        GeometryResyncToken existing = resyncRequirement.get();
        if (existing != null) {
            advanceResyncRequirement(existing.reason(), revision);
        }
        return revision;
    }

    public boolean capturedFallbackWithin(long gameTick, int maximumAgeTicks) {
        long firstStale = firstStaleGeometryTick;
        return firstStale >= 0L && gameTick >= firstStale
                && gameTick - firstStale <= maximumAgeTicks;
    }

    /** A new captured geometry may start a new deadline; coefficient-only cuts may not. */
    public synchronized void notePublishedGeometry(long geometryRevision, long gameTick) {
        PagePublication current = visiblePublication();
        PagePublication previous = previousPublication;
        if (current == PagePublication.EMPTY || current.geometryRevision() != geometryRevision) return;
        if (previous != PagePublication.EMPTY && previous.geometryRevision() >= geometryRevision) return;
        firstStaleGeometryTick = liveGeometryRevision.get() == geometryRevision
                ? -1L : Math.max(firstStaleGeometryTick, gameTick);
    }

    public synchronized long requireFullGeometryResync(GeometryResyncReason reason) {
        Objects.requireNonNull(reason, "reason");
        long revision = advanceGeometryRevision(true);
        advanceResyncRequirement(reason, revision);
        return revision;
    }

    /** Writer holds this monitor; readers see physical invalidation before the new capture revision. */
    private long advanceGeometryRevision(boolean physicalChange) {
        long revision = liveGeometryRevision.get() + 1;
        if (physicalChange) lastPhysicalGeometryRevision = revision;
        liveGeometryRevision.set(revision);
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
        Objects.requireNonNull(next, "Page publication is required");
        if (stagedPublication != null) throw new IllegalStateException("Page publication is staged");
        previousPublication = next == PagePublication.EMPTY ? PagePublication.EMPTY : publication;
        publication = next;
    }

    /** Returns one stable immutable cut, or {@code null} while capture is stale. */
    public PagePublication currentPublication() {
        long revision = liveGeometryRevision.get();
        PagePublication current = visiblePublication();
        return current != PagePublication.EMPTY
                && resyncRequirement.get() == null
                && current.geometryRevision() == revision
                && liveGeometryRevision.get() == revision
                ? current : null;
    }

    /** Complete Air geometry can remain current while only world/save state IDs are being captured. */
    public PagePublication currentAirPublication() {
        long physical = lastPhysicalGeometryRevision;
        PagePublication current = visiblePublication();
        return current != PagePublication.EMPTY && current.geometryRevision() >= physical
                && current.geometryRevision() <= liveGeometryRevision.get()
                && resyncRequirement.get() == null && lastPhysicalGeometryRevision == physical
                ? current : null;
    }

    public boolean hasCurrentAirGeometry(long capturedRevision) {
        PagePublication current = currentAirPublication();
        return current != null && current.geometryRevision() >= capturedRevision
                && capturedRevision >= lastPhysicalGeometryRevision;
    }

    /** Last non-empty worker cut for bounded temperature-only fallback/capture. */
    public PagePublication lastPublication() {
        PagePublication current = visiblePublication();
        return current == PagePublication.EMPTY ? null : current;
    }

    /** Previous committed geometry while the query buffer catches up with a new Page cut. */
    public PagePublication previousPublication() {
        var staged = stagedPublication;
        if (staged != null && staged.token.committed())
            return staged.previous == PagePublication.EMPTY ? null : staged.previous;
        PagePublication previous = previousPublication;
        return previous == PagePublication.EMPTY ? null : previous;
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
