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
