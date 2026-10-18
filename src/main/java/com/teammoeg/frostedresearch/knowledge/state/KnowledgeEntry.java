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
package com.teammoeg.frostedresearch.knowledge.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;

/**
 * 首次获得记录和显示摘要随知识一起保留。 / Retained acquisition and presentation fallback.
 */
public record KnowledgeEntry(KnowledgeElement element, AcquisitionSource source, long learnedAt, String resultType,
                             AcquisitionSource originalSource) {
    public static final Codec<KnowledgeEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            KnowledgeElement.CODEC.fieldOf("element").forGetter(KnowledgeEntry::element),
            AcquisitionSource.CODEC.fieldOf("source").forGetter(KnowledgeEntry::source),
            Codec.LONG.optionalFieldOf("learned_at", -1L).forGetter(KnowledgeEntry::learnedAt),
            Codec.STRING.optionalFieldOf("result_type", "").forGetter(KnowledgeEntry::resultType),
            AcquisitionSource.CODEC.fieldOf("original_source").forGetter(KnowledgeEntry::originalSource)
    ).apply(i, KnowledgeEntry::new));

    public KnowledgeEntry(KnowledgeElement element, AcquisitionSource source, long learnedAt, String resultType) {
        this(element, source, learnedAt, resultType, source);
    }

    public KnowledgeEntry learned(long time) {
        return new KnowledgeEntry(element, source, time, resultType, originalSource);
    }
}
