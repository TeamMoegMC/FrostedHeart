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
package com.teammoeg.frostedresearch.knowledge.link;

import java.util.List;

/**
 * 槽位与跨槽条件的组合，联络规则与研究证据共用。 / Slots and cross-slot conditions shared by links and evidence.
 */
public record SlotPattern(List<LinkRule.Slot> inputs, List<LinkRule.CrossCondition> conditions) {
    public SlotPattern {
        inputs = List.copyOf(inputs);
        conditions = List.copyOf(conditions);
    }
}
