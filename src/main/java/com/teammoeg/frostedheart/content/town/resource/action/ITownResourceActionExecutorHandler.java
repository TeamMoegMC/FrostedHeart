package com.teammoeg.frostedheart.content.town.resource.action;

/**
 * 集中管理某种城镇的TownResourceActionExecutor，根据传入的action类型返回对应的executor或直接利用其执行action
 */
public interface ITownResourceActionExecutorHandler{

    default <T extends ITownResourceAction> ITownResourceActionExecutor<T>  getExecutor(T action){
        @SuppressWarnings("unchecked")
        Class<T> actionClass = (Class<T>) action.getClass();
        return this.getExecutor(actionClass);
    }

    /**
     * 利用ITownResourceAction的类型获取对应的ActionExecutor。
     */
    <T extends ITownResourceAction> ITownResourceActionExecutor<T>  getExecutor(Class<T> clazz);

    /**
     * ActionExecutorHandler不一定实现所有种类的ActionExecutor，若传入了不支持的action会报错
     */
    //default ITownResourceActionResult<ITownResourceAction> execute(ITownResourceAction action) {
    //	return getExecutor(action).execute(action);
    //}

    /**
     * 获取对应的ActionExecutor并执行。
     * ActionExecutorHandler不一定实现所有种类的ActionExecutor，若传入了不支持的action会报错
     * @param action 一个ITownResourceAction
     * @return action对应的结果，其类型与action的类型对应
     */
    default <T extends ITownResourceAction> ITownResourceActionResult<T> execute(T action){
        return getExecutor(action).execute(action);
    }
}
