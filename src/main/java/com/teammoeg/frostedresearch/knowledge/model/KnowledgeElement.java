/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * 统一内容载体；想法/成果的摘要仅用于定义暂不可用时展示。
 */
public record KnowledgeElement(KnowledgeKey key, Optional<Observation> observation, String title, String body) {
    public static final Codec<KnowledgeElement> CODEC = RecordCodecBuilder.<KnowledgeElement>create(i -> i.group(
            KnowledgeKey.CODEC.fieldOf("key").forGetter(KnowledgeElement::key),
            Observation.CODEC.optionalFieldOf("observation").forGetter(KnowledgeElement::observation),
            Codec.STRING.optionalFieldOf("title", "").forGetter(KnowledgeElement::title),
            Codec.STRING.optionalFieldOf("body", "").forGetter(KnowledgeElement::body)
    ).apply(i, KnowledgeElement::new)).comapFlatMap(e -> {
        if (e.key.kind() == KnowledgeKey.Kind.OBSERVATION) {
            if (e.observation.isEmpty() || !e.observation.get().key().equals(e.key))
                return DataResult.error(() -> "Observation identity must agree with its original record");
        } else if (e.observation.isPresent())
            return DataResult.error(() -> "Definitions cannot carry observation records");
        return DataResult.success(e);
    }, java.util.function.Function.identity());

    public static KnowledgeElement observation(Observation value) {
        return new KnowledgeElement(value.key(), Optional.of(value), "", "");
    }

    public static KnowledgeElement idea(ResourceLocation id) {
        return new KnowledgeElement(KnowledgeKey.idea(id), Optional.empty(), "", "");
    }

    public static KnowledgeElement result(ResourceLocation id) {
        return new KnowledgeElement(KnowledgeKey.result(id), Optional.empty(), "", "");
    }

    public boolean canExportNote() {
        return observation.map(Observation::canExportNote).orElse(true);
    }
}
