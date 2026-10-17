/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.UUID;

/**
 * 知识身份：观察使用原始记录 UUID，想法及成果使用定义标识。
 */
public record KnowledgeKey(Kind kind, String id) {
    public static final Codec<Kind> KIND_CODEC = enumCodec(Kind.class);
    public static final Codec<KnowledgeKey> CODEC = RecordCodecBuilder.<KnowledgeKey>create(i -> i.group(
            KIND_CODEC.fieldOf("kind").forGetter(KnowledgeKey::kind),
            Codec.STRING.fieldOf("id").forGetter(KnowledgeKey::id)
    ).apply(i, KnowledgeKey::new)).comapFlatMap(key -> {
        try {
            if (key.kind == Kind.OBSERVATION) UUID.fromString(key.id);
            else new ResourceLocation(key.id);
            return DataResult.success(key);
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Invalid knowledge identity: " + key);
        }
    }, java.util.function.Function.identity());

    public static KnowledgeKey observation(UUID id) {
        return new KnowledgeKey(Kind.OBSERVATION, id.toString());
    }

    public static KnowledgeKey idea(ResourceLocation id) {
        return new KnowledgeKey(Kind.IDEA, id.toString());
    }

    public static KnowledgeKey result(ResourceLocation id) {
        return new KnowledgeKey(Kind.RESULT, id.toString());
    }

    public static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
        return Codec.STRING.comapFlatMap(s -> {
            try {
                return DataResult.success(Enum.valueOf(type, s.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "Unknown " + type.getSimpleName() + ": " + s);
            }
        }, e -> e.name().toLowerCase(Locale.ROOT));
    }

    public ResourceLocation definitionId() {
        return new ResourceLocation(id);
    }

    public enum Kind {OBSERVATION, IDEA, RESULT}
}
