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
