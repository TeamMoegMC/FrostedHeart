/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import java.util.Optional;

/** Pure per-town, per-settlement-day deduplication policy for morning shortage Tips. */
public final class TownTransportShortageNotificationModel {
    public static final State INITIAL = new State(-1L);

    private TownTransportShortageNotificationModel() {
    }

    public static Result onMorningSettlement(
            State state,
            long townDay,
            double totalCapacity,
            double reservedCapacity
    ) {
        State current = state == null ? INITIAL : state;
        if (townDay < 0L || current.lastEvaluatedTownDay() == townDay) {
            return new Result(current, Optional.empty());
        }
        State evaluated = new State(townDay);
        return new Result(evaluated,
                TownTransportShortageNotice.from(totalCapacity, reservedCapacity));
    }

    public record State(long lastEvaluatedTownDay) {
    }

    public record Result(State state, Optional<TownTransportShortageNotice> notice) {
        public Result {
            state = state == null ? INITIAL : state;
            notice = notice == null ? Optional.empty() : notice;
        }
    }
}
