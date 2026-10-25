package com.teammoeg.frostedheart.content.town.model;

import java.util.UUID;

/** Pure automatic opportunity numbering; negative windows are valid. */
public final class TownScheduleModel {
    public static final long TICKS_PER_DAY = 24000;
    public static final int MORNING_TICK = 1000;
    public static final int PHASE_COUNT = 20;

    private TownScheduleModel() {}
    public static int phase(UUID holderId) { return MORNING_TICK + Math.floorMod(holderId.hashCode(), PHASE_COUNT); }
    public static long window(long logicalTick, UUID holderId) { return Math.floorDiv(logicalTick - phase(holderId), TICKS_PER_DAY); }
    public static long calendarDay(long logicalTick) { return Math.floorDiv(logicalTick, TICKS_PER_DAY); }
}
