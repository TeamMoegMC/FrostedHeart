/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Small content catalogue for persistent knowledge carried by people.
 *
 * <p>The conversation and entity layers only enumerate packages from here;
 * they do not need to know which research subject a package may lead to.
 * Packages without a research offer are still useful conversation content and
 * follow the same persistence and selection path.</p>
 */
public final class PersonKnowledgePackageCatalog {
    private static final Map<ResourceLocation, Definition> DEFINITIONS = new LinkedHashMap<>();

    private PersonKnowledgePackageCatalog() {
    }

    /** Registers content without teaching the research core any domain IDs. */
    public static synchronized void register(Definition definition) {
        DEFINITIONS.put(definition.id(), definition);
    }

    /** Returns the packages assigned by one stable 0..99 background roll. */
    public static Set<ResourceLocation> packagesForRoll(boolean eligible, int normalizedRoll) {
        if (!eligible) return Set.of();
        return DEFINITIONS.values().stream()
                .filter(definition -> definition.matches(normalizedRoll))
                .map(Definition::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * Enumerates shareable packages in catalogue order. Unknown persisted IDs
     * remain in the overlay but are ignored until a definition is restored.
     */
    public static List<Share> shares(PersonKnowledgeOverlay overlay, String source) {
        return DEFINITIONS.values().stream()
                .filter(definition -> overlay.has(definition.id()))
                .map(definition -> new Share(definition.id(), definition.replyTranslationKey(),
                        definition.offer().map(template -> template.create(definition.id(), source))))
                .toList();
    }

    public static List<Definition> definitions() {
        return List.copyOf(DEFINITIONS.values());
    }

    public record Definition(ResourceLocation id, int rollStartInclusive, int rollEndExclusive,
            String replyTranslationKey, Optional<OfferTemplate> offer) {
        public Definition {
            if (rollStartInclusive < 0 || rollEndExclusive > 100 || rollStartInclusive >= rollEndExclusive) {
                throw new IllegalArgumentException("knowledge-package roll range must be within 0..100");
            }
            offer = offer == null ? Optional.empty() : offer;
        }

        boolean matches(int normalizedRoll) {
            return normalizedRoll >= rollStartInclusive && normalizedRoll < rollEndExclusive;
        }
    }

    public record OfferTemplate(ResourceLocation topicId, ResourceLocation ideaId) {
        KnowledgeOffer create(ResourceLocation packageId, String source) {
            return new KnowledgeOffer(packageId, topicId, ideaId, source);
        }
    }

    /** One package-specific reply plus an optional team-knowledge mutation. */
    public record Share(ResourceLocation packageId, String replyTranslationKey,
            Optional<KnowledgeOffer> offer) {
    }
}
