/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Persistent, independently generated background knowledge attached to a person. */
public record PersonKnowledgeOverlay(boolean initialized, int backgroundRoll,
        Set<ResourceLocation> knowledgeIds) {
    public static final PersonKnowledgeOverlay UNINITIALIZED = new PersonKnowledgeOverlay(false, -1, Set.of());
    public static final Codec<PersonKnowledgeOverlay> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(PersonKnowledgeOverlay::initialized),
            Codec.INT.optionalFieldOf("background_roll", -1).forGetter(PersonKnowledgeOverlay::backgroundRoll),
            ResourceLocation.CODEC.listOf().optionalFieldOf("knowledge", List.of())
                    .forGetter(value -> List.copyOf(value.knowledgeIds))
    ).apply(instance, (initialized, roll, knowledge) ->
            new PersonKnowledgeOverlay(initialized, roll, new LinkedHashSet<>(knowledge))));

    public PersonKnowledgeOverlay {
        knowledgeIds = Set.copyOf(knowledgeIds);
    }

    public static PersonKnowledgeOverlay initialize(boolean eligible, int roll) {
        int normalized = Math.floorMod(roll, 100);
        return new PersonKnowledgeOverlay(true, normalized,
                PersonKnowledgePackageCatalog.packagesForRoll(eligible, normalized));
    }

    /** Keeps an already generated background stable across reloads and transfers. */
    public PersonKnowledgeOverlay initializeIfNeeded(boolean eligible, int roll) {
        return initialized ? this : initialize(eligible, roll);
    }

    public boolean has(ResourceLocation id) {
        return knowledgeIds.contains(id);
    }
}
