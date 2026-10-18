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
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** One independently valid reason why a knowledge or technology target is available. */
public sealed interface AccessSource permits AccessSource.ResultSource, AccessSource.LegacySource {
    Codec<AccessSource> CODEC = Codec.STRING.dispatch("kind", AccessSource::kind, AccessSource::codecFor);

    String kind();

    private static Codec<? extends AccessSource> codecFor(String kind) {
        return switch (kind) {
            case "result" -> ResultSource.CODEC;
            case "legacy" -> LegacySource.CODEC;
            default -> throw new IllegalArgumentException("Unknown access source kind: " + kind);
        };
    }

    record ResultSource(ResourceLocation topicId, ResearchResult.ResultType resultType,
            ResourceLocation resultId) implements AccessSource {
        static final Codec<ResultSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("topic").forGetter(ResultSource::topicId),
                ResearchResult.RESULT_TYPE_CODEC.fieldOf("result_type").forGetter(ResultSource::resultType),
                ResourceLocation.CODEC.fieldOf("result").forGetter(ResultSource::resultId)
        ).apply(instance, ResultSource::new));

        @Override
        public String kind() {
            return "result";
        }
    }

    record LegacySource(String researchId, String effectNonce) implements AccessSource {
        static final Codec<LegacySource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("research").forGetter(LegacySource::researchId),
                Codec.STRING.fieldOf("effect").forGetter(LegacySource::effectNonce)
        ).apply(instance, LegacySource::new));

        @Override
        public String kind() {
            return "legacy";
        }
    }
}
