/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.observation;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Pure ten-second coalescing state machine for tower service notifications. */
public final class TownTowerTipThrottle {
    public static final long WINDOW_GAME_TICKS = 200L;
    public static final State INITIAL = new State(null, 0L, null);

    private TownTowerTipThrottle() {
    }

    public static Result onCrossing(State state, long gameTime, boolean active) {
        if (state.lastNotifiedActive() == null) {
            TownSignalNotice notice = notice(active);
            return new Result(new State(active, gameTime + WINDOW_GAME_TICKS, null), List.of(notice));
        }
        if (gameTime <= state.cooldownUntilGameTick()) {
            return new Result(new State(
                    state.lastNotifiedActive(), state.cooldownUntilGameTick(), active), List.of());
        }
        if (state.lastNotifiedActive() == active) {
            return new Result(new State(state.lastNotifiedActive(), 0L, null), List.of());
        }
        TownSignalNotice notice = notice(active);
        return new Result(new State(active, gameTime + WINDOW_GAME_TICKS, null), List.of(notice));
    }

    public static Result onTick(State state, long gameTime) {
        if (state.deferredActive() == null || gameTime <= state.cooldownUntilGameTick()) {
            return new Result(state, List.of());
        }
        boolean finalActive = state.deferredActive();
        if (state.lastNotifiedActive() != null && state.lastNotifiedActive() == finalActive) {
            return new Result(new State(state.lastNotifiedActive(), 0L, null), List.of());
        }
        TownSignalNotice notice = notice(finalActive);
        return new Result(new State(finalActive, gameTime + WINDOW_GAME_TICKS, null), List.of(notice));
    }

    private static TownSignalNotice notice(boolean active) {
        return new TownSignalNotice(
                active ? TownSignalEvent.Type.TOWER_SERVICE_RESTORED
                        : TownSignalEvent.Type.TOWER_SERVICE_LOST,
                active ? TownSignalEvent.Severity.INFORMATION
                        : TownSignalEvent.Severity.CRITICAL,
                1);
    }

    public record State(
            @Nullable Boolean lastNotifiedActive,
            long cooldownUntilGameTick,
            @Nullable Boolean deferredActive
    ) {
    }

    public record Result(State state, List<TownSignalNotice> emitted) {
        public Result {
            emitted = List.copyOf(emitted);
        }
    }
}
