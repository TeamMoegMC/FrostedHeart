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

import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.model.*;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

/**
 * 定义与有效性查询；学习前提不作为持续有效条件。 / Current definitions and validity.
 */
public final class KnowledgeAvailability {
    private KnowledgeAvailability() {
    }

    public static ResearchResult result(ResourceLocation id) {
        var definition = KnowledgeDefinitions.current().results().get(id);
        if (definition != null) return definition.enabled() ? definition.result() : null;
        var entry = ResearchResultCatalog.current().result(id);
        return entry == null ? null : entry.result();
    }

    public static boolean available(KnowledgeElement element) {
        return switch (element.key().kind()) {
            case OBSERVATION ->
                    element.observation().map(KnowledgeDefinitions.current()::hasObservationUse).orElse(false);
            case IDEA -> {
                var def = KnowledgeDefinitions.current().ideas().get(new ResourceLocation(element.key().id()));
                yield def != null && def.enabled();
            }
            case RESULT -> result(new ResourceLocation(element.key().id())) != null;
        };
    }

    public static List<KnowledgeKey> understanding(KnowledgeKey key) {
        if (key.kind() == KnowledgeKey.Kind.IDEA) {
            var d = KnowledgeDefinitions.current().ideas().get(new ResourceLocation(key.id()));
            return d == null ? List.of() : d.understanding();
        }
        if (key.kind() == KnowledgeKey.Kind.RESULT) {
            var d = KnowledgeDefinitions.current().results().get(new ResourceLocation(key.id()));
            return d == null ? List.of() : d.understanding();
        }
        return List.of();
    }

    public static KnowledgeElement describe(KnowledgeElement original) {
        var key = original.key();
        if (key.kind() == KnowledgeKey.Kind.IDEA) {
            var d = KnowledgeDefinitions.current().ideas().get(new ResourceLocation(key.id()));
            if (d != null) return new KnowledgeElement(key, original.observation(), d.title(), d.body());
        } else if (key.kind() == KnowledgeKey.Kind.RESULT) {
            var d = KnowledgeDefinitions.current().results().get(new ResourceLocation(key.id()));
            if (d != null) return new KnowledgeElement(key, original.observation(), d.title(), d.body());
        }
        return original;
    }

    public static String resultType(KnowledgeKey key) {
        if (key.kind() != KnowledgeKey.Kind.RESULT) return "";
        ResearchResult result = result(new ResourceLocation(key.id()));
        return result == null ? "" : result.type().token();
    }
}
