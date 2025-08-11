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
    public <T extends ITownResourceAction> ITownResourceActionExecutor<T>  getExecutor(T action){
        Class<? extends ITownResourceAction> actionClass = action.getClass();
        if(executors.containsKey(actionClass)){
            //这里没有检查类型转换，修改Map的时候注意点。
            return (ITownResourceActionExecutor<T>) executors.get(actionClass);
        }
        throw new IllegalArgumentException("Executor AbstractActionExecutorHandler can't execute action: "
                + actionClass.getName()
                + "!\n Check AbstractActionExecutorHandler, add the sub_executor of this action, " +
                "or don't execute this action in this executor.");
    }
}
