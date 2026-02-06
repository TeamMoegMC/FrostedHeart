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
 * 集中管理某种城镇的TownResourceActionExecutor，根据传入的action类型返回对应的executor或直接利用其执行action
 */
public interface IActionExecutorHandler extends ITownResourceActionExecutor<ITownResourceAction>{

    default <T extends ITownResourceAction> ITownResourceActionExecutor<T>  getExecutor(T action){
        @SuppressWarnings("unchecked")
        Class<T> actionClass = (Class<T>) action.getClass();
        return this.getExecutor(actionClass);
    }

    <T extends ITownResourceAction> ITownResourceActionExecutor<T>  getExecutor(Class<T> clazz);

    /**
     * ActionExecutorHandler不一定实现所有种类的ActionExecutor，若传入了不支持的action会报错
     */
    default ITownResourceActionResult execute(ITownResourceAction action) {
    	return getExecutor(action).execute(action);
    }
}
