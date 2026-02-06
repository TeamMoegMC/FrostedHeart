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

package com.teammoeg.frostedheart.content.town.resource.action;

import com.teammoeg.frostedheart.content.town.resource.ITownResourceAttribute;

/**
 * 作为一个接口，承载了{@link TownResourceActionResults.ItemResourceAttributeCostActionResult}和{@link TownResourceActions.VirtualResourceAttributeActionResult}共有的一些特性，在{@link TownResourceActions.TownResourceTypeCostAction}中使用
 */
public interface ITownResourceAttributeActionResult extends ITownResourceActionResult {
    boolean allModified();

    double getAmount();

    int getLevel();

    ITownResourceAttribute getTownResourceAttribute();

    double residualAmount();

    double totalModifiedAmount();
}
