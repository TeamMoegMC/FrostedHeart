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
 * 用于记录对城镇资源进行操作的具体内容。可以使用一个record类来实现这个接口。
 * <br>
 * Action本身不对资源进行操作，将其输入到对应的{@link ITownResourceActionExecutor}中，由executor来进行操作。
 * <br>
 * 通常的，一个城镇的资源可以接受多种Action，因此可通过{@link IActionExecutorHandler}自动分配对应的executor，并执行。
 * <br>
 * Action实例一般放在{@link TownResourceActions}
 * <br>
 * @param <R> 这里的泛型类：Action和ActionResult相互绑定，在{@link IActionExecutorHandler}中的execute方法返回确定的Result类型。
 */
public interface ITownResourceAction<R extends ITownResourceActionResult<?>> {//为啥编译器想让我删掉R？删了这个ActionHandler怎么知道要返回什么结果

    //int getID();目前不需要ID，AbstractActionExecutorHandler依靠Class判断。

}
