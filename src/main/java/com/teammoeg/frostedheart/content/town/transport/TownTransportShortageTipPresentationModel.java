/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import java.util.List;
import java.util.Locale;

/** Pure localization keys, formatting, and layout limits for the shortage Tip. */
public final class TownTransportShortageTipPresentationModel {
    public static final String TITLE_KEY = "tips.frostedheart.town.transport_shortage.title";
    public static final String DETAIL_KEY = "tips.frostedheart.town.transport_shortage.detail";
    public static final String OVERFLOW_KEY = "tips.frostedheart.town.transport_shortage.overflow";
    public static final int MAX_VISIBLE_NOTICES = 3;

    private TownTransportShortageTipPresentationModel() {
    }

    public static Presentation create(List<TownTransportShortageNotice> notices) {
        List<TownTransportShortageNotice> safe = List.copyOf(notices == null ? List.of() : notices);
        int visibleCount = Math.min(MAX_VISIBLE_NOTICES, safe.size());
        return new Presentation(
                safe.subList(0, visibleCount),
                safe.size() - visibleCount,
                TITLE_KEY,
                DETAIL_KEY,
                OVERFLOW_KEY);
    }

    public static String formatCapacity(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static String formatScale(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value * 100.0);
    }

    public record Presentation(
            List<TownTransportShortageNotice> visibleNotices,
            int overflowCount,
            String titleKey,
            String detailKey,
            String overflowKey
    ) {
        public Presentation {
            visibleNotices = List.copyOf(visibleNotices);
            overflowCount = Math.max(0, overflowCount);
        }
    }
}
