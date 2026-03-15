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

import com.teammoeg.frostedheart.FHMain;
import org.jetbrains.annotations.Nullable;

/**
 * 集中管理某种城镇的TownResourceActionExecutor，根据传入的action类型返回对应的executor或直接利用其执行action
 */
public interface IActionExecutorHandler{

    default @Nullable <A extends ITownResourceAction<R>, R extends ITownResourceActionResult<A>> ITownResourceActionExecutor<A, R>  getExecutor(A action){
        @SuppressWarnings("unchecked")
        Class<A> actionClass = (Class<A>) action.getClass();
        return this.getExecutor(actionClass);
    }

    @Nullable <A extends ITownResourceAction<R>, R extends ITownResourceActionResult<A>> ITownResourceActionExecutor<A, R>  getExecutor(Class<A> clazz);


    /**
     * 判断此类能否处理此action
     * @return true if this handler can handle this action
     */
    boolean hasExecutor(ITownResourceAction<?> action);

    /**
     * ActionExecutorHandler不一定实现所有种类的ActionExecutor，若传入了不支持的action会报错
     */
    default <A extends ITownResourceAction<R>, R extends ITownResourceActionResult<A>> R execute(A action) {
        ITownResourceActionExecutor<A, R> executor = this.getExecutor(action);
        if(executor == null){
            //我说这种严重问题就直接throw了
            throw new IllegalArgumentException("Executor AbstractActionExecutorHandler can't execute action: "
                    + action.getClass().getName()
                    + "!\n Check AbstractActionExecutorHandler, add the sub_executor of this action, " +
                    "or don't execute this action in this executor.");
        }
    	return executor.execute(action);
    }

    /**
     * 尝试执行action，允许action的类型是模糊的，但相应的，返回的Result类型也是模糊的
     */
    default <A extends ITownResourceAction<? super R>, R extends ITownResourceActionResult<?>> R executeFuzzy(A action) {
        ITownResourceActionExecutor<ITownResourceAction<?>, ? extends ITownResourceActionResult<?>> executor = this.getExecutor(action.getClass());
        if(executor == null){
            throw new IllegalArgumentException("Executor AbstractActionExecutorHandler can't execute action: "
                    + action.getClass().getName()
                    + "!\n Check AbstractActionExecutorHandler, add the sub_executor of this action, " +
                    "or don't execute this action in this executor.");
        } else{
            //我觉得这个转换应该没问题吧
            return (R) executor.execute(action);
        }
    }

}
