/*
 * Copyright (c) 2024 TeamMoeg
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

import java.util.HashMap;
import java.util.Map;

/**
 * 一个ActionExecutorHandler的模板，可以不用。
 */
public abstract class AbstractActionExecutorHandler implements IActionExecutorHandler{
    /**
     * 存储ActionExecutor。
     * <br>
     * 键为Action的Class。
     * {@link TownResourceActions}
     * <br>
     * 值为Action对应的ActionExecutor。
     */
    protected final Map<Class<? extends ITownResourceAction>, ITownResourceActionExecutor<? extends ITownResourceAction>> executors = new HashMap<>();

    protected <T extends ITownResourceAction> void registerExecutor(Class<T> actionClass, ITownResourceActionExecutor<T> executor){
        executors.put(actionClass, executor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ITownResourceAction> ITownResourceActionExecutor<T> getExecutor(Class<T> actionClass) {
        if(executors.containsKey(actionClass)){
            //这里没有检查类型转换，修改executors的那个Map的时候需要注意。
            return (ITownResourceActionExecutor<T>) executors.get(actionClass);
        }
        throw new IllegalArgumentException("Executor AbstractActionExecutorHandler can't execute action: "
                + actionClass.getName()
                + "!\n Check AbstractActionExecutorHandler, add the sub_executor of this action, " +
                "or don't execute this action in this executor.");
    }
}
