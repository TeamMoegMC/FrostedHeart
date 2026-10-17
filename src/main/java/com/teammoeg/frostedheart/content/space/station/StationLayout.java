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

package com.teammoeg.frostedheart.content.space.station;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.core.Vec3i;

/**
 * Immutable coordinates of every section slot of a station ring.
 * <p>
 * The layout is derived from a {@link StationShape}: each layer is a circle, the number of slots comes from that
 * circle's circumference, and slot centres are evenly spaced by angle. Slot {@code 0} starts after the configured
 * phase, so the slot boundaries — not the slot centres — sit on the phase angle. Building a layout is deterministic
 * and cheap; keep the instance instead of recomputing coordinates per query.
 */
public final class StationLayout {
    private final StationShape shape;
    private final List<List<StationSlot>> layers;
    private final List<StationSlot> slots;
    private final Map<Vec3i, StationSlot> slotsByAnchor;

    private StationLayout(StationShape shape, List<List<StationSlot>> layers, List<StationSlot> slots,
                          Map<Vec3i, StationSlot> slotsByAnchor) {
        this.shape = shape;
        this.layers = layers;
        this.slots = slots;
        this.slotsByAnchor = slotsByAnchor;
    }

    /**
     * Builds the complete slot table for {@code shape}.
     *
     * @throws IllegalArgumentException when the shape would make two slot centres round to the same block, which
     *                                  would make section positions ambiguous
     */
    public static StationLayout of(StationShape shape) {
        Objects.requireNonNull(shape, "shape");
        List<List<StationSlot>> layers = new ArrayList<>(shape.layerCount());
        List<StationSlot> slots = new ArrayList<>(shape.totalSectionCount());
        Map<Vec3i, StationSlot> slotsByAnchor = new HashMap<>(shape.totalSectionCount() * 2);

        for (int layer = 0; layer < shape.layerCount(); layer++) {
            int radius = shape.layerRadius(layer);
            int count = shape.sectionCount(layer);
            List<StationSlot> layerSlots = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                double angle = normalizeAngle(shape.phaseRadians() + StationShape.TAU * (index + 0.5D) / count);
                double centerX = radius * Math.cos(angle);
                double centerZ = radius * Math.sin(angle);
                Vec3i anchor = new Vec3i((int) Math.round(centerX), 0, (int) Math.round(centerZ));
                StationSlot slot = new StationSlot(layer, index, count, angle, centerX, centerZ, anchor);

                StationSlot existing = slotsByAnchor.putIfAbsent(anchor, slot);
                if (existing != null) {
                    throw new IllegalArgumentException("Station shape places slots "
                            + describe(existing) + " and " + describe(slot)
                            + " on the same anchor " + anchor + "; increase layerDepth or sectionArcLength");
                }
                layerSlots.add(slot);
                slots.add(slot);
            }
            layers.add(Collections.unmodifiableList(layerSlots));
        }
        return new StationLayout(shape, Collections.unmodifiableList(layers), Collections.unmodifiableList(slots),
                Map.copyOf(slotsByAnchor));
    }

    private static double normalizeAngle(double angle) {
        double normalized = angle % StationShape.TAU;
        return normalized < 0.0D ? normalized + StationShape.TAU : normalized;
    }

    private static String describe(StationSlot slot) {
        return "layer " + slot.layer() + " slot " + slot.index();
    }

    /**
     * Shape this layout was built from.
     */
    public StationShape shape() {
        return shape;
    }

    /**
     * Every slot, ordered by layer and then by index.
     */
    public List<StationSlot> slots() {
        return slots;
    }

    /**
     * Slots of one layer, ordered by index around the ring.
     */
    public List<StationSlot> slots(int layer) {
        shape.layerRadius(layer);
        return layers.get(layer);
    }

    /**
     * Slot at a layer and index.
     */
    public StationSlot slot(int layer, int index) {
        List<StationSlot> layerSlots = slots(layer);
        if (index < 0 || index >= layerSlots.size()) {
            throw new IllegalArgumentException("index out of range [0," + layerSlots.size() + "): " + index);
        }
        return layerSlots.get(index);
    }

    /**
     * Slot whose anchor is exactly {@code anchor}.
     */
    public Optional<StationSlot> slotAt(Vec3i anchor) {
        return Optional.ofNullable(slotsByAnchor.get(anchor));
    }

    /**
     * Total number of section slots across every layer.
     */
    public int sectionCount() {
        return slots.size();
    }

    /**
     * Number of section slots on one layer.
     */
    public int sectionCount(int layer) {
        return slots(layer).size();
    }

    /**
     * Number of layers.
     */
    public int layerCount() {
        return layers.size();
    }
}
