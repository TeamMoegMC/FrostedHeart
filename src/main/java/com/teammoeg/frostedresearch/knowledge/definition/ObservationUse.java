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
