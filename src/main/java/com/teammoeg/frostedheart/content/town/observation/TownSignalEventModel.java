/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.observation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Pure merging and notification compaction rules for town threshold events. */
public final class TownSignalEventModel {
    private TownSignalEventModel() {
    }

    /**
     * Adds an event to a daily history list. Equal state crossings are deduplicated,
     * while same-cause resident exits are combined instead of losing residents.
     */
    public static void addToHistory(List<TownSignalEvent> events, TownSignalEvent candidate) {
        if (isTowerService(candidate.type())) {
            events.add(candidate);
            return;
        }
        for (int index = 0; index < events.size(); index++) {
            TownSignalEvent existing = events.get(index);
            if (sameHistoryKey(existing, candidate) && isResidentExit(candidate.type())) {
                events.set(index, new TownSignalEvent(
                        existing.day(), existing.hour(), existing.type(), existing.severity(),
                        saturatedAdd(existing.affectedCount(), candidate.affectedCount()),
                        existing.episodeId(), joinDetails(existing.detail(), candidate.detail())));
                return;
            }
            if (existing.day() == candidate.day()
                    && existing.hour() == candidate.hour()
                    && existing.type() == candidate.type()
                    && existing.severity() == candidate.severity()
                    && existing.affectedCount() == candidate.affectedCount()) {
                return;
            }
        }
        events.add(candidate);
    }

    /**
     * Compacts a burst into final state transitions. State domains use latest-wins;
     * irreversible exits retain and sum each cause. Output is severity-first and
     * stable by the original occurrence order within a severity.
     */
    public static List<TownSignalNotice> compactNotifications(Collection<TownSignalEvent> events) {
        Map<Domain, IndexedNotice> latestStates = new EnumMap<>(Domain.class);
        Map<TownSignalEvent.Type, IndexedNotice> exits = new EnumMap<>(TownSignalEvent.Type.class);
        int order = 0;
        for (TownSignalEvent event : events) {
            TownSignalNotice notice = TownSignalNotice.from(event);
            if (isResidentExit(event.type())) {
                IndexedNotice prior = exits.get(event.type());
                exits.put(event.type(), prior == null
                        ? new IndexedNotice(notice, order)
                        : new IndexedNotice(new TownSignalNotice(
                                notice.type(), maximumSeverity(prior.notice().severity(), notice.severity()),
                                saturatedAdd(prior.notice().affectedCount(), notice.affectedCount())),
                                prior.order()));
            } else {
                latestStates.put(domain(event.type()), new IndexedNotice(notice, order));
            }
            order++;
        }

        List<IndexedNotice> compacted = new ArrayList<>(latestStates.values());
        compacted.addAll(exits.values());
        compacted.sort((left, right) -> {
            int severity = Integer.compare(severityRank(right.notice().severity()),
                    severityRank(left.notice().severity()));
            return severity != 0 ? severity : Integer.compare(left.order(), right.order());
        });
        return compacted.stream().map(IndexedNotice::notice).toList();
    }

    public static boolean isTowerService(TownSignalEvent.Type type) {
        return type == TownSignalEvent.Type.TOWER_SERVICE_LOST
                || type == TownSignalEvent.Type.TOWER_SERVICE_RESTORED;
    }

    public static boolean isResidentExit(TownSignalEvent.Type type) {
        return type == TownSignalEvent.Type.RESIDENT_EXIT_HEALTH
                || type == TownSignalEvent.Type.RESIDENT_EXIT_MENTAL
                || type == TownSignalEvent.Type.RESIDENT_EXIT_BOTH;
    }

    public static int severityRank(TownSignalEvent.Severity severity) {
        return switch (severity) {
            case INFORMATION -> 0;
            case WARNING -> 1;
            case CRITICAL -> 2;
            case IRREVERSIBLE -> 3;
        };
    }

    /** Canonical player-facing severity for each event type. */
    public static TownSignalEvent.Severity defaultSeverity(TownSignalEvent.Type type) {
        return switch (type) {
            case CLIMATE_COLD_ENDED, TOWER_SERVICE_RESTORED,
                    HOUSE_TEMPERATURE_RECOVERED, HUNTING_TEMPERATURE_RECOVERED,
                    FOOD_RESERVE_RECOVERED, FUEL_RESERVE_RECOVERED,
                    WORK_CAPACITY_RECOVERED, EXIT_RISK_RECOVERED,
                    CRISIS_RECOVERED -> TownSignalEvent.Severity.INFORMATION;
            case CLIMATE_COLD_WARNING, HUNTING_TEMPERATURE_STOP,
                    FOOD_RESERVE_WARNING, FUEL_RESERVE_WARNING,
                    WORK_CAPACITY_LOST -> TownSignalEvent.Severity.WARNING;
            case TOWER_SERVICE_LOST, HOUSE_TEMPERATURE_UNSAFE,
                    FOOD_SHORTAGE, FUEL_SHORTAGE,
                    EXIT_RISK_ENTERED -> TownSignalEvent.Severity.CRITICAL;
            case RESIDENT_EXIT_HEALTH, RESIDENT_EXIT_MENTAL,
                    RESIDENT_EXIT_BOTH -> TownSignalEvent.Severity.IRREVERSIBLE;
        };
    }

    private static boolean sameHistoryKey(TownSignalEvent left, TownSignalEvent right) {
        return left.day() == right.day() && left.hour() == right.hour()
                && left.type() == right.type() && left.severity() == right.severity();
    }

    private static int saturatedAdd(int left, int right) {
        long result = (long) left + right;
        return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, result);
    }

    private static String joinDetails(String left, String right) {
        if (right == null || right.isBlank() || left.equals(right)) return left;
        if (left == null || left.isBlank()) return right;
        return left + "," + right;
    }

    private static TownSignalEvent.Severity maximumSeverity(
            TownSignalEvent.Severity left,
            TownSignalEvent.Severity right
    ) {
        return severityRank(left) >= severityRank(right) ? left : right;
    }

    private static Domain domain(TownSignalEvent.Type type) {
        return switch (type) {
            case CLIMATE_COLD_WARNING, CLIMATE_COLD_ENDED -> Domain.CLIMATE;
            case TOWER_SERVICE_LOST, TOWER_SERVICE_RESTORED -> Domain.TOWER;
            case HOUSE_TEMPERATURE_UNSAFE, HOUSE_TEMPERATURE_RECOVERED -> Domain.HOUSE_TEMPERATURE;
            case HUNTING_TEMPERATURE_STOP, HUNTING_TEMPERATURE_RECOVERED -> Domain.HUNTING_TEMPERATURE;
            case FOOD_RESERVE_WARNING, FOOD_SHORTAGE, FOOD_RESERVE_RECOVERED -> Domain.FOOD_RESERVE;
            case FUEL_RESERVE_WARNING, FUEL_SHORTAGE, FUEL_RESERVE_RECOVERED -> Domain.FUEL_RESERVE;
            case WORK_CAPACITY_LOST, WORK_CAPACITY_RECOVERED -> Domain.WORK_CAPACITY;
            case EXIT_RISK_ENTERED, EXIT_RISK_RECOVERED -> Domain.EXIT_RISK;
            case CRISIS_RECOVERED -> Domain.CRISIS;
            case RESIDENT_EXIT_HEALTH -> Domain.RESIDENT_EXIT_HEALTH;
            case RESIDENT_EXIT_MENTAL -> Domain.RESIDENT_EXIT_MENTAL;
            case RESIDENT_EXIT_BOTH -> Domain.RESIDENT_EXIT_BOTH;
        };
    }

    private enum Domain {
        CLIMATE,
        TOWER,
        HOUSE_TEMPERATURE,
        HUNTING_TEMPERATURE,
        FOOD_RESERVE,
        FUEL_RESERVE,
        WORK_CAPACITY,
        EXIT_RISK,
        CRISIS,
        RESIDENT_EXIT_HEALTH,
        RESIDENT_EXIT_MENTAL,
        RESIDENT_EXIT_BOTH
    }

    private record IndexedNotice(TownSignalNotice notice, int order) {
    }
}
