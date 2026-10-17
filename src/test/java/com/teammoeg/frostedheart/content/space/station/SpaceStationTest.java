/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.space.station;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class SpaceStationTest {
    private static SpaceStation newStation() {
        return new SpaceStation(StationShape.of(64, 2, 16, 16.0D));
    }

    @Test
    void placesSectionsOnDistinctSlots() {
        SpaceStation station = newStation();
        StationSlot slot = station.getLayout().slot(0, 3);

        assertTrue(station.place(SectionType.RESIDENTIAL, 0, 3).isPresent());
        assertEquals(1, station.sectionCount());
        assertEquals(station.getLayout().sectionCount() - 1, station.remainingSlots());
        assertEquals(SectionType.RESIDENTIAL, station.sectionAt(slot.anchor()).orElseThrow().getType());

        assertFalse(station.place(SectionType.POWER, 0, 3).isPresent(),
                "an occupied anchor must not be overwritten");
        assertEquals(1, station.sectionCount());
    }

    @Test
    void rejectsSectionsFromAnotherLayout() {
        SpaceStation station = newStation();
        StationLayout other = StationLayout.of(StationShape.of(70, 2, 16, 16.0D));

        assertFalse(station.place(new StationSection(SectionType.PORT, other.slot(0, 0))));
        StationSlot shifted = station.getLayout().slot(0, 0);
        StationSlot wrongDeck = new StationSlot(shifted.layer(), shifted.index(), shifted.layerSectionCount(),
                shifted.angleRadians(), shifted.centerX(), shifted.centerZ(), shifted.anchorAt(30));
        assertFalse(station.place(new StationSection(SectionType.PORT, wrongDeck)));
        assertEquals(0, station.sectionCount());
    }

    @Test
    void tracksMissingSectionTypes() {
        SpaceStation station = newStation();
        assertEquals(EnumSet.allOf(SectionType.class), station.missingTypes());

        station.place(SectionType.LIFE_SUPPORT, 0, 0);
        station.place(SectionType.AGRICULTURE, 0, 1);

        assertEquals(EnumSet.complementOf(EnumSet.of(SectionType.LIFE_SUPPORT, SectionType.AGRICULTURE)),
                station.missingTypes());
        assertEquals(1, station.count(SectionType.LIFE_SUPPORT));
        assertEquals(0, station.count(SectionType.POWER));
    }

    @Test
    void worksEveryPlacedSectionInPlacementOrder() {
        SpaceStation station = newStation();
        StringBuilder order = new StringBuilder();
        station.place(section(SectionType.RESIDENTIAL, station.getLayout().slot(1, 0), order));
        station.place(section(SectionType.POWER, station.getLayout().slot(0, 0), order));

        station.work(1);

        assertEquals("residential,power,", order.toString());
    }

    private static StationSection section(SectionType type, StationSlot slot, StringBuilder order) {
        return new StationSection(type, slot) {
            @Override
            public void work(SpaceStation station, int time) {
                order.append(getType().getKey()).append(',');
            }
        };
    }

    @Test
    void exposesShapeAndOuterRadius() {
        SpaceStation station = newStation();

        assertEquals(2, station.getLayout().layerCount());
        assertEquals(80, station.getShape().outerRadius());
        assertEquals(0, station.getLayout().slot(1, 0).anchor().getY());
    }
}
