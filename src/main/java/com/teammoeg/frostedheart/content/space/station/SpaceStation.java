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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import net.minecraft.core.Vec3i;

/**
 * A station ring: geometry from {@link StationLayout} plus the sections placed on it.
 * <p>
 * The layout owns every valid position; the station only records which slot holds which section. Placing a section
 * whose slot was not derived from this layout, or whose anchor is already taken, is rejected instead of silently
 * overwriting another section.
 */
public final class SpaceStation {
    private final StationLayout layout;
    private final Map<Vec3i, Section> sections = new LinkedHashMap<>();
    private final Reference2LongOpenHashMap<SpaceResource> resources =
            new Reference2LongOpenHashMap<>(SpaceResource.values().length);
    private final List<ConstructionSite> constructions = new ArrayList<>();

    public SpaceStation(StationLayout layout) {
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    public SpaceStation(StationShape shape) {
        this(StationLayout.of(shape));
    }

    public StationLayout getLayout() {
        return layout;
    }

    public StationShape getShape() {
        return layout.shape();
    }

    /**
     * Mutable station resource stockpile. Uses {@link SpaceResource} identities, not town item keys.
     */
    public Reference2LongOpenHashMap<SpaceResource> getResources() {
        return resources;
    }

    /**
     * Construction sites currently in progress.
     */
    public List<ConstructionSite> getConstructions() {
        return Collections.unmodifiableList(constructions);
    }

    /**
     * Places {@code section} on its slot.
     *
     * @return {@code true} when the slot was empty and belonged to this station, {@code false} when the anchor was
     * already occupied or the slot does not exist in this layout
     */
    public boolean place(Section section) {
        Objects.requireNonNull(section, "section");
        StationSlot slot = Objects.requireNonNull(section.getSlot(), "section slot");
        Optional<StationSlot> expected = layout.slotAt(slot.anchor());
        if (expected.isEmpty() || !expected.get().equals(slot)) {
            return false;
        }
        if (sections.containsKey(slot.anchor())) {
            return false;
        }
        sections.put(slot.anchor(), section);
        return true;
    }

    /**
     * Places a plain section of {@code type} at a layer and index.
     */
    public Optional<Section> place(SectionType type, int layer, int index) {
        StationSection section = new StationSection(type, layout.slot(layer, index));
        return place(section) ? Optional.of(section) : Optional.empty();
    }

    /**
     * Section placed on the slot whose anchor is {@code anchor}.
     */
    public Optional<Section> sectionAt(Vec3i anchor) {
        return Optional.ofNullable(sections.get(anchor));
    }

    /**
     * Sections in placement order.
     */
    public List<Section> sections() {
        return List.copyOf(sections.values());
    }

    /**
     * Number of placed sections.
     */
    public int sectionCount() {
        return sections.size();
    }

    /**
     * Number of slots still empty.
     */
    public int remainingSlots() {
        return layout.sectionCount() - sections.size();
    }

    /**
     * Number of placed sections of {@code type}.
     */
    public int count(SectionType type) {
        int count = 0;
        for (Section section : sections.values()) {
            if (section.getType() == type) {
                count++;
            }
        }
        return count;
    }

    /**
     * Roles that the station does not contain yet, in enum order.
     */
    public Set<SectionType> missingTypes() {
        EnumSet<SectionType> missing = EnumSet.allOf(SectionType.class);
        for (Section section : sections.values()) {
            missing.remove(section.getType());
        }
        return missing;
    }

    /**
     * Runs one step for every placed section, in placement order.
     */
    public void work(int time) {
        for (Section section : List.copyOf(sections.values())) {
            section.work(this, time);
        }
    }
}
