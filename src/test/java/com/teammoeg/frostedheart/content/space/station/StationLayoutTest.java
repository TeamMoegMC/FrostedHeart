/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.space.station;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

class StationLayoutTest {
    @Test
    void sectionCountFollowsCircumference() {
        StationShape shape = StationShape.of(64, 3, 16, 16.0D);

        assertEquals(25, shape.sectionCount(0));
        assertEquals(31, shape.sectionCount(1));
        assertEquals(37, shape.sectionCount(2));
        assertEquals(93, shape.totalSectionCount());

        StationLayout layout = StationLayout.of(shape);
        assertEquals(93, layout.sectionCount());
        assertEquals(25, layout.sectionCount(0));
        assertEquals(31, layout.sectionCount(1));
        assertEquals(37, layout.sectionCount(2));
    }

    @Test
    void layersAreConcentricAndOrdered() {
        StationShape shape = StationShape.of(32, 4, 8, 12.0D);
        StationLayout layout = StationLayout.of(shape);

        assertEquals(4, layout.layerCount());
        assertEquals(List.of(32, 40, 48, 56),
                List.of(shape.layerRadius(0), shape.layerRadius(1), shape.layerRadius(2), shape.layerRadius(3)));

        double previousRadius = 0.0D;
        for (int layer = 0; layer < layout.layerCount(); layer++) {
            double radius = layout.slots(layer).get(0).centerRadius();
            assertTrue(radius > previousRadius);
            previousRadius = radius;
        }
    }

    @Test
    void slotCentresAreEvenlySpacedAroundTheRing() {
        StationLayout layout = StationLayout.of(StationShape.of(64, 1, 16, 16.0D));
        List<StationSlot> slots = layout.slots(0);
        double step = StationShape.TAU / slots.size();

        for (int index = 0; index < slots.size(); index++) {
            StationSlot slot = slots.get(index);
            assertEquals(index, slot.index());
            assertEquals(slots.size(), slot.layerSectionCount());
            assertEquals((index + 0.5D) * step, slot.angleRadians(), 1.0E-9D);
        }
    }

    @Test
    void anchorsSitOnTheLayerCircleAndAreUnique() {
        StationLayout layout = StationLayout.of(StationShape.of(20, 3, 10, 14.0D));

        for (StationSlot slot : layout.slots()) {
            double radius = layout.shape().layerRadius(slot.layer());
            assertEquals(radius, slot.centerRadius(), 1.0E-9D);
            assertEquals(slot.anchor(), layout.slotAt(slot.anchor()).orElseThrow().anchor());
            assertTrue(Math.abs(slot.centerRadius() - Math.hypot(slot.anchor().getX(), slot.anchor().getZ())) <= 0.75D,
                    "rounded anchor must stay next to the circle: " + slot);
            assertEquals(0, slot.anchor().getY());
            assertEquals(new Vec3i(slot.anchor().getX(), 7, slot.anchor().getZ()), slot.anchorAt(7));
        }
    }

    @Test
    void phaseRotatesSlotCentres() {
        StationLayout none = StationLayout.of(StationShape.of(50, 1, 10, 20.0D));
        StationLayout rotated = StationLayout.of(new StationShape(50, 1, 10, 20.0D, Math.PI / 2.0D));

        double offset = rotated.slots(0).get(0).angleRadians() - none.slots(0).get(0).angleRadians();
        offset = (offset + StationShape.TAU) % StationShape.TAU;
        assertEquals(Math.PI / 2.0D, offset, 1.0E-9D);
        assertEquals(none.slots(0).size(), rotated.slots(0).size());
    }

    @Test
    void rejectsShapesWithoutUsableGeometry() {
        assertThrows(IllegalArgumentException.class, () -> StationShape.of(0, 1, 8, 16.0D));
        assertThrows(IllegalArgumentException.class, () -> StationShape.of(16, 0, 8, 16.0D));
        assertThrows(IllegalArgumentException.class, () -> StationShape.of(16, 1, 0, 16.0D));
        assertThrows(IllegalArgumentException.class, () -> StationShape.of(16, 1, 8, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> StationShape.of(16, 1, 8, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new StationShape(16, 1, 8, 16.0D, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> StationShape.of(16, 1, 8, 16.0D).layerRadius(1));
    }

    @Test
    void tinyRingStillHasOneSectionPerLayer() {
        StationShape shape = StationShape.of(1, 2, 1, 16.0D);

        assertEquals(1, shape.sectionCount(0));
        assertEquals(1, shape.sectionCount(1));
        assertEquals(2, StationLayout.of(shape).sectionCount());
    }
}
