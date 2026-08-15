/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.observation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Pure, localization-independent layout policy for one town signal tip. */
public final class TownSignalTipPresentationModel {
    public static final int MAX_VISIBLE_EVENTS = 5;

    private TownSignalTipPresentationModel() {
    }

    public static Presentation create(Collection<TownSignalNotice> notices) {
        List<TownSignalNotice> sorted = new ArrayList<>(notices);
        sorted.sort((left, right) -> Integer.compare(
                TownSignalEventModel.severityRank(right.severity()),
                TownSignalEventModel.severityRank(left.severity())));
        TownSignalEvent.Severity highest = sorted.isEmpty()
                ? TownSignalEvent.Severity.INFORMATION
                : sorted.get(0).severity();
        int visibleCount = Math.min(MAX_VISIBLE_EVENTS, sorted.size());
        return new Presentation(
                highest,
                List.copyOf(sorted.subList(0, visibleCount)),
                sorted.size() - visibleCount,
                titleKey(highest),
                displayTime(highest),
                fontColor(highest),
                highest == TownSignalEvent.Severity.CRITICAL
                        || highest == TownSignalEvent.Severity.IRREVERSIBLE);
    }

    public static boolean usesAffectedCount(TownSignalEvent.Type type) {
        return switch (type) {
            case HOUSE_TEMPERATURE_UNSAFE, HOUSE_TEMPERATURE_RECOVERED,
                    HUNTING_TEMPERATURE_STOP, HUNTING_TEMPERATURE_RECOVERED,
                    WORK_CAPACITY_LOST, WORK_CAPACITY_RECOVERED,
                    STAFFING_TARGET_UNMET, STAFFING_TARGET_RECOVERED,
                    EXIT_RISK_ENTERED, EXIT_RISK_RECOVERED,
                    RESIDENT_EXIT_HEALTH, RESIDENT_EXIT_MENTAL, RESIDENT_EXIT_BOTH -> true;
            default -> false;
        };
    }

    public static String eventKey(TownSignalEvent.Type type) {
        return "tips.frostedheart.town.event." + type.name().toLowerCase(Locale.ROOT);
    }

    private static String titleKey(TownSignalEvent.Severity severity) {
        return switch (severity) {
            case INFORMATION -> "tips.frostedheart.town.title.information";
            case WARNING -> "tips.frostedheart.town.title.warning";
            case CRITICAL -> "tips.frostedheart.town.title.critical";
            case IRREVERSIBLE -> "tips.frostedheart.town.title.irreversible";
        };
    }

    private static int displayTime(TownSignalEvent.Severity severity) {
        return switch (severity) {
            case INFORMATION -> 4_000;
            case WARNING -> 6_000;
            case CRITICAL -> 8_000;
            case IRREVERSIBLE -> 10_000;
        };
    }

    private static int fontColor(TownSignalEvent.Severity severity) {
        return switch (severity) {
            case INFORMATION -> 0xFF55FFFF;
            case WARNING -> 0xFFFFAA00;
            case CRITICAL -> 0xFFFF5555;
            case IRREVERSIBLE -> 0xFFAA55FF;
        };
    }

    public record Presentation(
            TownSignalEvent.Severity highestSeverity,
            List<TownSignalNotice> visibleEvents,
            int overflowCount,
            String titleKey,
            int displayTimeMillis,
            int fontColor,
            boolean preemptsTutorial
    ) {
        public Presentation {
            visibleEvents = List.copyOf(visibleEvents);
            overflowCount = Math.max(0, overflowCount);
        }
    }
}
