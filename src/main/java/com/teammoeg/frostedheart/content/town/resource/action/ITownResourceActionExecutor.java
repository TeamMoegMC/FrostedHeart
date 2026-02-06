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

/**
 * 接受单一的某种修改资源的操作，即ITownResourceAction的某个子类，并根据其对城镇资源进行修改，随后返回对应的结果。
 * 相关类：{@link IActionExecutorHandler}
 */
public interface ITownResourceActionExecutor<T extends ITownResourceAction> {
    ITownResourceActionResult execute(T action);
}
