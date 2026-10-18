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

import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 队伍知识存储视图；状态修改由 KnowledgeService 提交。 / Team storage view.
 */
public final class KnowledgeArchive {
    final Map<KnowledgeKey, KnowledgeEntry> entries = new LinkedHashMap<>();

    public Map<KnowledgeKey, KnowledgeEntry> entries() {
        return Collections.unmodifiableMap(entries);
    }

    public boolean contains(KnowledgeKey key) {
        return entries.containsKey(key);
    }

    public long observationCount() {
        return entries.keySet().stream().filter(k -> k.kind() == KnowledgeKey.Kind.OBSERVATION).count();
    }
}
