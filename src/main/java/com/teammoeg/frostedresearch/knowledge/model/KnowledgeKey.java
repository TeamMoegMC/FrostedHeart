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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.model;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

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

    /**
     * {@code optionalFieldOf} with the same encoding, except that a present value which fails to decode is an error.
     * DFU 6 treats it as absent, so a saved record would load with that part silently dropped and be saved without it.
     */
    public static <A> MapCodec<Optional<A>> strictOptionalField(Codec<A> codec, String name) {
        MapCodec<Optional<A>> lenient = codec.optionalFieldOf(name);
        return MapCodec.of(lenient, new MapDecoder.Implementation<>() {
            @Override
            public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input) {
                T value = input.get(name);
                return value == null ? DataResult.success(Optional.empty()) : codec.parse(ops, value).map(Optional::of);
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return lenient.keys(ops);
            }
        }, () -> "StrictOptional[" + name + "]");
    }

    public ResourceLocation definitionId() {
        return new ResourceLocation(id);
    }

    public enum Kind {OBSERVATION, IDEA, RESULT}
}
