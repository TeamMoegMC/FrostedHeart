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
 * 用于记录对城镇资源进行操作的结果。
 * @param <A> 这里的泛型类：Action和ActionResult相互绑定，在{@link IActionExecutorHandler}中的execute方法返回确定的Result类型。
 */
public interface ITownResourceActionResult <A extends ITownResourceAction<?>>{
    A getAction();
}
