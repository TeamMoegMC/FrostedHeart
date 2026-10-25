package com.teammoeg.chorda.dataholders.persistence;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import com.teammoeg.chorda.Chorda;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

/**
 * One server session's checkpoints. Record state belongs to the server thread;
 * the worker sees only frozen snapshots, a bounded queue and result messages.
 * The single drain runs to completion without a tick or server.execute callback.
 */
public final class PersistenceCoordinator {
    public enum Kind { TEAM, AI_TOWN, TOWN_METADATA }
    public record RecordKey(Kind kind, UUID id) {}
    public record CommitFacts(int storageVersion, boolean deleted, Set<UUID> supersededHolderIds) {
        public CommitFacts { supersededHolderIds = Set.copyOf(supersededHolderIds); }
        static CommitFacts from(CompoundTag root) {
            Set<UUID> superseded = new HashSet<>();
            for (Tag tag : root.getList("supersededHolderIds", Tag.TAG_INT_ARRAY)) {
                superseded.add(NbtUtils.loadUUID(tag));
            }
            return new CommitFacts(root.getInt("storageVersion"), root.getBoolean("deleted"), superseded);
        }
    }
    public record RecordSnapshot(RecordKey key, Path target, long revision, long captureTick,
                                 CompoundTag root, long retainedBytes, CommitFacts facts) {}
    public record WriteResult(RecordKey key, long revision, boolean success, Throwable failure,
                              long writtenBytes, CommitFacts facts) {}
    public record SaveBatch(Map<RecordKey, Long> targets, Map<RecordKey, Throwable> captureFailures,
                            Map<RecordKey, Long> deferredRetirements) {}
    public record SaveReport(Map<RecordKey, String> failures) {
        public boolean successful() { return failures.isEmpty(); }
    }
    public interface RecordSource {
        /** Refresh untracked component capture versions; do not run gameplay. */
        void prepareCheckpoint();
        /** Return detached NBT. The source must never mutate it after returning. */
        CompoundTag capture() throws Exception;
    }
    @FunctionalInterface
    public interface Writer { long write(RecordSnapshot snapshot) throws Exception; }

    public static final class RecordState {
        private final RecordKey key;
        private final Path target;
        private RecordSource source;
        private long currentRevision = 1;
        private long capturedRevision;
        private long persistedRevision;
        private Throwable lastFailure;
        private boolean retired;
        private boolean needsReplacementConfirmation;

        private RecordState(RecordKey key, Path target, RecordSource source) {
            this.key = key;
            this.target = target;
            this.source = source;
        }
        public void markChanged() { currentRevision++; }
        public long currentRevision() { return currentRevision; }
        public long capturedRevision() { return capturedRevision; }
        public long persistedRevision() { return persistedRevision; }
        public boolean isDirty() { return currentRevision > persistedRevision; }
        public boolean isRetired() { return retired; }
    }

    private final Map<RecordKey, RecordState> records = new LinkedHashMap<>();
    private final Set<UUID> confirmedSuperseded = new HashSet<>();
    private final ArrayList<WriteResult> commitEffects = new ArrayList<>();
    private BiConsumer<RecordKey, CommitFacts> commitListener = (key, facts) -> {};
    private final Executor executor;
    private final Writer writer;
    private final int maxSnapshots;
    private final long maxBytes;
    private final Object queueLock = new Object();
    private final LinkedHashMap<RecordKey, RecordSnapshot> pending = new LinkedHashMap<>();
    private final ConcurrentLinkedQueue<WriteResult> completed = new ConcurrentLinkedQueue<>();
    private RecordSnapshot inFlight;
    private long retainedBytes;
    private boolean drainScheduled;
    private boolean closed;
    private CompletableFuture<Void> drainDone = CompletableFuture.completedFuture(null);

    public PersistenceCoordinator(Executor executor, Writer writer) {
        this(executor, writer, 64, 32L * 1024 * 1024);
    }

    public PersistenceCoordinator(Executor executor, Writer writer, int maxSnapshots, long maxBytes) {
        this.executor = executor;
        this.writer = writer;
        this.maxSnapshots = Math.max(1, maxSnapshots);
        this.maxBytes = Math.max(1, maxBytes);
    }

    public RecordState registerRecord(RecordKey key, Path target, RecordSource source) {
        RecordState state = records.get(key);
        if (state == null) {
            state = new RecordState(key, target, source);
            records.put(key, state);
        } else {
            state.source = source;
            state.markChanged();
        }
        return state;
    }

    public void setCommitListener(BiConsumer<RecordKey, CommitFacts> listener) {
        commitListener = listener;
    }

    /** Loading failed before any checkpoint: retain the disk record unchanged. */
    public void discardUnloaded(RecordKey key) {
        RecordState state = records.get(key);
        if (state != null && state.capturedRevision == 0) records.remove(key);
    }

    /** Facts read from a complete V1 header are already durable. */
    public void confirmLoaded(CommitFacts facts) {
        confirmedSuperseded.addAll(facts.supersededHolderIds());
    }

    public void retire(RecordState state, RecordSource deletion, boolean needsConfirmation) {
        state.source = deletion;
        state.retired = true;
        state.needsReplacementConfirmation = needsConfirmation;
        state.markChanged();
    }

    public SaveBatch checkpoint(long tick) {
        applyCompleted();
        applyCommitEffects();
        return capture(tick, state -> true, true);
    }

    private SaveBatch capture(long tick, Predicate<RecordState> include, boolean prepare) {
        Map<RecordKey, Long> targets = new LinkedHashMap<>();
        Map<RecordKey, Throwable> failures = new LinkedHashMap<>();
        Map<RecordKey, Long> deferred = new LinkedHashMap<>();
        for (RecordState state : new ArrayList<>(records.values())) {
            if (!include.test(state)) continue;
            try {
                if (prepare && !state.retired) state.source.prepareCheckpoint();
                if (!state.isDirty()) continue;
                if (state.needsReplacementConfirmation && !confirmedSuperseded.contains(state.key.id())) {
                    deferred.put(state.key, state.currentRevision);
                    continue;
                }
                long revision = state.currentRevision;
                targets.put(state.key, revision);
                if (queuedRevision(state.key) >= revision) continue;
                CompoundTag root = state.source.capture();
                root.putInt("storageVersion", 1);
                root.putLong("captureTick", tick);
                long bytes = root.sizeInBytes();
                if (bytes < 0) bytes = maxBytes + 1;
                enqueue(new RecordSnapshot(state.key, state.target, revision, tick, root,
                        bytes, CommitFacts.from(root)));
                state.capturedRevision = revision;
            } catch (Exception ex) {
                state.markChanged();
                targets.put(state.key, state.currentRevision);
                failures.put(state.key, ex);
                state.lastFailure = ex;
                Chorda.LOGGER.error("Unable to capture record {}", state.key, ex);
            }
        }
        return new SaveBatch(Map.copyOf(targets), Map.copyOf(failures), Map.copyOf(deferred));
    }

    /** Business records, authorized retirements, then migration metadata. */
    public SaveReport checkpointAndAwait(long tick) {
        SaveBatch first = checkpoint(tick);
        awaitDrain();
        applyCommitEffects();
        SaveBatch retirements = capture(tick, state -> state.retired, false);
        awaitDrain();
        applyCommitEffects();
        SaveBatch metadata = capture(tick, state -> state.key.kind() == Kind.TOWN_METADATA, false);
        awaitDrain();
        applyCommitEffects();
        return report(first, retirements, metadata);
    }

    private SaveReport report(SaveBatch... batches) {
        Map<RecordKey, String> failures = new LinkedHashMap<>();
        for (SaveBatch batch : batches) {
            Map<RecordKey, Long> expected = new LinkedHashMap<>(batch.targets());
            expected.putAll(batch.deferredRetirements());
            for (var target : expected.entrySet()) {
                RecordState state = records.get(target.getKey());
                if (state != null && state.persistedRevision >= target.getValue()) continue;
                Throwable cause = state == null ? null : state.lastFailure;
                failures.put(target.getKey(), "required=" + target.getValue() + ", saved="
                        + (state == null ? 0 : state.persistedRevision)
                        + (cause == null ? "; replacement not confirmed" : "; " + cause));
            }
        }
        return new SaveReport(Collections.unmodifiableMap(failures));
    }

    /** Only version/error updates here; safe during admission and capture loops. */
    public void applyCompleted() {
        WriteResult result;
        while ((result = completed.poll()) != null) {
            RecordState state = records.get(result.key());
            if (state == null) continue;
            if (result.success()) {
                state.persistedRevision = Math.max(state.persistedRevision, result.revision());
                state.lastFailure = null;
                commitEffects.add(result);
            } else {
                state.lastFailure = result.failure();
                Chorda.LOGGER.error("Unable to write record {} revision {}", result.key(), result.revision(), result.failure());
            }
        }
    }

    /** Called only between batches, never inside source.capture or queue locks. */
    public void applyCommitEffects() {
        var effects = new ArrayList<>(commitEffects);
        commitEffects.clear();
        for (WriteResult result : effects) {
            confirmedSuperseded.addAll(result.facts().supersededHolderIds());
            try {
                commitListener.accept(result.key(), result.facts());
            } catch (RuntimeException failure) {
                commitEffects.add(result);
                Chorda.LOGGER.error("Deferred record confirmation {}", result.key(), failure);
            }
        }
    }

    private long queuedRevision(RecordKey key) {
        synchronized (queueLock) {
            long revision = inFlight != null && inFlight.key().equals(key) ? inFlight.revision() : 0;
            RecordSnapshot next = pending.get(key);
            return next == null ? revision : Math.max(revision, next.revision());
        }
    }

    private void enqueue(RecordSnapshot snapshot) {
        CompletableFuture<Void> start = null;
        boolean interrupted = false;
        try {
            while (true) {
                applyCompleted();
                synchronized (queueLock) {
                    if (closed) throw new IllegalStateException("Persistence session has closed");
                    RecordSnapshot previous = pending.get(snapshot.key());
                    int count = pending.size() + (inFlight == null ? 0 : 1);
                    long newBytes = retainedBytes - (previous == null ? 0 : previous.retainedBytes()) + snapshot.retainedBytes();
                    if (count == 0 || (count + (previous == null ? 1 : 0) <= maxSnapshots && newBytes <= maxBytes)) {
                        pending.put(snapshot.key(), snapshot);
                        retainedBytes = newBytes;
                        if (!drainScheduled) {
                            drainScheduled = true;
                            start = drainDone = new CompletableFuture<>();
                        }
                        break;
                    }
                    try { queueLock.wait(); }
                    catch (InterruptedException ex) { interrupted = true; }
                }
            }
            if (start != null) {
                CompletableFuture<Void> token = start;
                try { executor.execute(() -> drain(token)); }
                catch (RuntimeException rejected) { abortDrain(rejected, token); }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private void drain(CompletableFuture<Void> token) {
        try {
            while (true) {
                RecordSnapshot snapshot;
                synchronized (queueLock) {
                    if (pending.isEmpty()) {
                        drainScheduled = false;
                        queueLock.notifyAll();
                        break;
                    }
                    var iterator = pending.values().iterator();
                    snapshot = iterator.next();
                    iterator.remove();
                    inFlight = snapshot;
                }
                WriteResult result;
                try {
                    long bytes = writer.write(snapshot);
                    result = new WriteResult(snapshot.key(), snapshot.revision(), true, null, bytes, snapshot.facts());
                } catch (Exception failure) {
                    result = failed(snapshot, failure);
                }
                synchronized (queueLock) {
                    completed.add(result);
                    inFlight = null;
                    retainedBytes -= snapshot.retainedBytes();
                    queueLock.notifyAll();
                }
            }
            token.complete(null);
        } catch (Throwable failure) {
            abortDrain(failure, token);
        }
    }

    private static WriteResult failed(RecordSnapshot snapshot, Throwable failure) {
        return new WriteResult(snapshot.key(), snapshot.revision(), false, failure, 0, snapshot.facts());
    }

    private void abortDrain(Throwable failure, CompletableFuture<Void> token) {
        synchronized (queueLock) {
            if (drainDone == token) {
                if (inFlight != null) completed.add(failed(inFlight, failure));
                for (RecordSnapshot snapshot : pending.values()) completed.add(failed(snapshot, failure));
                inFlight = null;
                pending.clear();
                retainedBytes = 0;
                drainScheduled = false;
                queueLock.notifyAll();
            }
        }
        token.complete(null);
    }

    public void awaitDrain() {
        CompletableFuture<Void> token;
        synchronized (queueLock) { token = drainDone; }
        token.join();
        applyCompleted();
    }

    /** Frozen work only: safe after worlds have closed. Does not own the executor. */
    public SaveReport close() {
        awaitDrain();
        applyCommitEffects();
        synchronized (queueLock) { closed = true; }
        Map<RecordKey, Long> expected = new LinkedHashMap<>();
        for (RecordState state : records.values()) {
            expected.put(state.key, state.currentRevision);
        }
        return report(new SaveBatch(expected, Map.of(), Map.of()));
    }
}
