/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.observation;

import com.teammoeg.frostedheart.content.town.TownHistoryEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure settlement-day sequencing and idempotent client history merge rules. */
public final class TownHistoryModel {
    private TownHistoryModel() {
    }

    /**
     * Allocates the label for the next completed town settlement. The stable
     * world day remains the baseline, but every settlement advances at least
     * one town day even when several manual /town tick commands run without
     * advancing world time.
     */
    public static long nextSettlementDay(List<TownHistoryEntry> existing, long currentWorldDay) {
        long latestSettlementDay = existing.stream()
                .mapToLong(TownHistoryEntry::day)
                .max()
                .orElse(Long.MIN_VALUE);
        if (latestSettlementDay == Long.MIN_VALUE) return currentWorldDay;
        long nextAfterLatest = latestSettlementDay == Long.MAX_VALUE
                ? Long.MAX_VALUE : latestSettlementDay + 1L;
        return Math.max(currentWorldDay, nextAfterLatest);
    }

    /** Idempotently merges a server entry; duplicate packets replace by town day. */
    public static List<TownHistoryEntry> upsert(
            List<TownHistoryEntry> existing,
            TownHistoryEntry entry,
            int maximumEntries
    ) {
        List<TownHistoryEntry> result = new ArrayList<>(existing);
        result.removeIf(value -> value.day() == entry.day());
        result.add(entry);
        result.sort(Comparator.comparingLong(TownHistoryEntry::day));
        int cap = Math.max(1, maximumEntries);
        if (result.size() > cap) {
            result = new ArrayList<>(result.subList(result.size() - cap, result.size()));
        }
        return List.copyOf(result);
    }
}
