/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read model for acquired findings. Orphan result IDs are intentionally absent. */
public final class KnowledgeProjection {
    public static final Codec<KnowledgeProjection> CODEC = FindingEntry.CODEC.listOf()
            .xmap(KnowledgeProjection::new, projection -> new ArrayList<>(projection.findings.values()));
    public static final KnowledgeProjection EMPTY = new KnowledgeProjection(List.of());

    private final Map<ResourceLocation, FindingEntry> findings;

    public KnowledgeProjection(List<FindingEntry> findings) {
        Map<ResourceLocation, FindingEntry> byId = new LinkedHashMap<>();
        findings.forEach(entry -> byId.put(entry.findingId(), entry));
        this.findings = Collections.unmodifiableMap(byId);
    }

    public boolean hasFinding(ResourceLocation id) {
        return findings.containsKey(id);
    }

    public FindingEntry finding(ResourceLocation id) {
        return findings.get(id);
    }

    public Map<ResourceLocation, FindingEntry> findings() {
        return findings;
    }

    public record FindingEntry(ResourceLocation findingId, List<ResourceLocation> views,
            AccessSource.ResultSource source) {
        static final Codec<FindingEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("finding").forGetter(FindingEntry::findingId),
                ResourceLocation.CODEC.listOf().fieldOf("views").forGetter(FindingEntry::views),
                AccessSource.ResultSource.CODEC.fieldOf("source").forGetter(FindingEntry::source)
        ).apply(instance, FindingEntry::new));

        public FindingEntry {
            views = List.copyOf(views);
        }
    }
}
