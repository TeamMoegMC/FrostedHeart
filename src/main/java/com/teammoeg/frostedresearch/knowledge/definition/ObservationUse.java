/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.definition;

import com.teammoeg.frostedresearch.knowledge.model.Observation;

import java.util.Optional;

/**
 * 研究等外部系统显式登记需求；未知等价性保留记录，不推断其可互相替代。
 */
public interface ObservationUse {
    boolean matches(Observation observation);

    default Optional<Boolean> equivalent(Observation first, Observation second) {
        return Optional.empty();
    }

    default int independentRecords() {
        return 1;
    }
}
