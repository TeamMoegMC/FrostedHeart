/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Historical test-only dynamic-exclusion prototype. Production treats moving
 * structures as air and does not use this index.
 */
public final class Phase0aDynamicExclusionIndex {
    private final int maxInterestedSections;
    private final Set<SectionKey> interestedSections = new LinkedHashSet<>();
    private final Map<SectionKey, Set<Long>> exclusionsBySection = new HashMap<>();
    private final Map<Long, Set<SectionKey>> sectionsByDynamicObject = new HashMap<>();
    private final Map<Long, DynamicSnapshot> snapshotsByDynamicObject = new HashMap<>();

    public Phase0aDynamicExclusionIndex(int maxInterestedSections) {
        if (maxInterestedSections <= 0) {
            throw new IllegalArgumentException("maxInterestedSections must be positive");
        }
        this.maxInterestedSections = maxInterestedSections;
    }

    public void setInterestedSections(Set<SectionKey> sections) {
        if (sections.size() > maxInterestedSections) {
            throw new IllegalArgumentException("interested section budget exceeded");
        }
        interestedSections.clear();
        interestedSections.addAll(sections);
        exclusionsBySection.clear();
        sectionsByDynamicObject.clear();
        for (Map.Entry<Long, DynamicSnapshot> entry : snapshotsByDynamicObject.entrySet()) {
            Set<SectionKey> indexed = intersectInterested(entry.getValue().dimension(), entry.getValue().bounds());
            index(entry.getKey(), indexed);
        }
    }

    public DynamicUpdate update(
            long dynamicObjectId,
            ResourceKey<Level> dimension,
            @Nullable AABB oldBounds,
            @Nullable AABB newBounds,
            long effectiveTick,
            long watermark) {
        Set<SectionKey> oldSections = intersectInterested(dimension, oldBounds);
        Set<SectionKey> newSections = intersectInterested(dimension, newBounds);

        Set<SectionKey> previouslyIndexed = sectionsByDynamicObject.remove(dynamicObjectId);
        if (previouslyIndexed != null) {
            for (SectionKey section : previouslyIndexed) {
                remove(section, dynamicObjectId);
            }
        }
        if (newBounds == null) {
            snapshotsByDynamicObject.remove(dynamicObjectId);
        } else {
            snapshotsByDynamicObject.put(dynamicObjectId, new DynamicSnapshot(dimension, newBounds));
            index(dynamicObjectId, newSections);
        }

        Set<SectionKey> invalidated = new LinkedHashSet<>(oldSections);
        if (previouslyIndexed != null) {
            invalidated.addAll(previouslyIndexed);
        }
        invalidated.addAll(newSections);
        return new DynamicUpdate(
                dynamicObjectId,
                Set.copyOf(oldSections),
                Set.copyOf(newSections),
                Set.copyOf(invalidated),
                effectiveTick,
                watermark,
                ExclusionState.UNRESOLVED_DYNAMIC);
    }

    public ExclusionState stateAt(SectionKey section) {
        Set<Long> exclusions = exclusionsBySection.get(section);
        return exclusions == null || exclusions.isEmpty()
                ? ExclusionState.CLEAR
                : ExclusionState.UNRESOLVED_DYNAMIC;
    }

    private Set<SectionKey> intersectInterested(ResourceKey<Level> dimension, @Nullable AABB bounds) {
        Set<SectionKey> result = new LinkedHashSet<>();
        if (bounds == null) {
            return result;
        }
        for (SectionKey section : interestedSections) {
            if (section.dimension().equals(dimension) && intersects(bounds, section)) {
                result.add(section);
            }
        }
        return result;
    }

    private static boolean intersects(AABB bounds, SectionKey section) {
        double minX = section.sectionX() * 16.0D;
        double minY = section.sectionY() * 16.0D;
        double minZ = section.sectionZ() * 16.0D;
        return bounds.maxX > minX && bounds.minX < minX + 16.0D
                && bounds.maxY > minY && bounds.minY < minY + 16.0D
                && bounds.maxZ > minZ && bounds.minZ < minZ + 16.0D;
    }

    private void remove(SectionKey section, long dynamicObjectId) {
        Set<Long> exclusions = exclusionsBySection.get(section);
        if (exclusions != null) {
            exclusions.remove(dynamicObjectId);
            if (exclusions.isEmpty()) {
                exclusionsBySection.remove(section);
            }
        }
    }

    private void index(long dynamicObjectId, Set<SectionKey> sections) {
        if (sections.isEmpty()) {
            return;
        }
        sectionsByDynamicObject.put(dynamicObjectId, new HashSet<>(sections));
        for (SectionKey section : sections) {
            exclusionsBySection.computeIfAbsent(section, ignored -> new HashSet<>()).add(dynamicObjectId);
        }
    }

    public enum ExclusionState {
        CLEAR,
        UNRESOLVED_DYNAMIC
    }

    public record SectionKey(ResourceKey<Level> dimension, int sectionX, int sectionY, int sectionZ) {
    }

    public record DynamicUpdate(
            long dynamicObjectId,
            Set<SectionKey> oldSections,
            Set<SectionKey> newSections,
            Set<SectionKey> invalidatedSections,
            long effectiveTick,
            long watermark,
            ExclusionState state) {
    }

    private record DynamicSnapshot(ResourceKey<Level> dimension, AABB bounds) {
    }
}
