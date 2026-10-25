package com.teammoeg.frostedheart.content.town;

import java.util.Optional;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persisted automatic progress, separate from manual town age and runtime skips. */
public record AutoSettlementState(Optional<Long> processedThrough, Optional<Long> lastCompleted,
                                  Optional<Origin> pendingOrigin) {
    public enum Origin { NEW, LEGACY }
    public static final Codec<AutoSettlementState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("processedThrough").forGetter(AutoSettlementState::processedThrough),
            Codec.LONG.optionalFieldOf("lastCompleted").forGetter(AutoSettlementState::lastCompleted),
            Codec.STRING.xmap(Origin::valueOf, Origin::name).optionalFieldOf("pendingOrigin")
                    .forGetter(AutoSettlementState::pendingOrigin)
    ).apply(instance, AutoSettlementState::new));

    public static AutoSettlementState pendingNew() { return pending(Origin.NEW); }
    public static AutoSettlementState pendingLegacy() { return pending(Origin.LEGACY); }
    private static AutoSettlementState pending(Origin origin) {
        return new AutoSettlementState(Optional.empty(), Optional.empty(), Optional.of(origin));
    }

    public AutoSettlementState normalize() {
        Optional<Long> lowerBound = maximum(processedThrough, lastCompleted);
        Optional<Origin> origin = pendingOrigin;
        if (lowerBound.isEmpty() && origin.isEmpty()) origin = Optional.of(Origin.LEGACY);
        return new AutoSettlementState(lowerBound, lastCompleted, origin);
    }

    public AutoSettlementState resolveInitialWindow(long window, long calendarDay) {
        AutoSettlementState state = normalize();
        if (state.pendingOrigin.isEmpty()) return state;
        long baseline = state.pendingOrigin.get() == Origin.LEGACY ? calendarDay : window;
        return new AutoSettlementState(maximum(state.processedThrough, Optional.of(baseline)),
                state.lastCompleted, Optional.empty());
    }

    /** Candidate economic facts win; the old instance contributes deduplication obligations. */
    public AutoSettlementState mergeForReplacement(AutoSettlementState previous) {
        AutoSettlementState candidate = normalize();
        AutoSettlementState old = previous.normalize();
        Optional<Origin> origin = candidate.pendingOrigin;
        if (old.pendingOrigin.orElse(null) == Origin.LEGACY || origin.orElse(null) == Origin.LEGACY) {
            origin = Optional.of(Origin.LEGACY);
        } else if (origin.isEmpty()) origin = old.pendingOrigin;
        return new AutoSettlementState(maximum(candidate.processedThrough, old.processedThrough),
                candidate.lastCompleted, origin);
    }

    public AutoSettlementState afterAttempt(long window) {
        return new AutoSettlementState(maximum(processedThrough, Optional.of(window)), lastCompleted, pendingOrigin);
    }

    public AutoSettlementState afterCompletion(long window) {
        return new AutoSettlementState(maximum(processedThrough, Optional.of(window)),
                maximum(lastCompleted, Optional.of(window)), pendingOrigin);
    }

    private static Optional<Long> maximum(Optional<Long> first, Optional<Long> second) {
        if (first.isEmpty()) return second;
        if (second.isEmpty()) return first;
        return Optional.of(Math.max(first.get(), second.get()));
    }
}
