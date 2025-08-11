package com.teammoeg.frostedheart.content.town.resource.action;

/**
 * 集中管理某种城镇的TownResourceActionExecutor，根据传入的action类型返回对应的executor或直接利用其执行action
 */
public interface IActionExecutorHandler extends ITownResourceActionExecutor<ITownResourceAction>{
    <T extends ITownResourceAction> ITownResourceActionExecutor<T>  getExecutor(T action);

    /**
     * ActionExecutorHandler不一定实现所有种类的ActionExecutor，若传入了不支持的action会报错
     */
    default ITownResourceActionResult execute(ITownResourceAction action) {
    	return getExecutor(action).execute(action);
    }
}
