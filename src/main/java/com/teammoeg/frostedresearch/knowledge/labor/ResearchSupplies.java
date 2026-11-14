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
package com.teammoeg.frostedresearch.knowledge.labor;

import com.teammoeg.frostedresearch.knowledge.definition.ProjectRequirement;

import java.util.List;

/**
 * 研究人力每天消耗的物品来源，例如城镇仓库。数量可以是小数。
 * <p>
 * Where research labor draws its daily items from, such as a town warehouse. Amounts may be fractional.
 */
public interface ResearchSupplies {
    /** A source that holds nothing. */
    ResearchSupplies NONE = new ResearchSupplies() {
        @Override
        public double share(List<ProjectRequirement.Material> items, List<Double> amounts) {
            return items.isEmpty() ? 1 : 0;
        }

        @Override
        public boolean take(List<ProjectRequirement.Material> items, List<Double> amounts) {
            return items.isEmpty();
        }
    };

    /**
     * The largest share, from 0 to 1, of these amounts that is in stock now. {@code amounts} lines up with
     * {@code items}.
     */
    double share(List<ProjectRequirement.Material> items, List<Double> amounts);

    /**
     * Takes every amount together, or nothing when any of them is short.
     */
    boolean take(List<ProjectRequirement.Material> items, List<Double> amounts);
}
