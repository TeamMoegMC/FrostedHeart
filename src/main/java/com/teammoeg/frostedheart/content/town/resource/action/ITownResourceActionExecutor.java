package com.teammoeg.frostedheart.content.town.resource.action;

/**
 * 接受单一的某种修改资源的操作，即ITownResourceAction的某个子类，并根据其对城镇资源进行修改，随后返回对应的结果。
 * 相关类：{@link ITownResourceActionExecutorHandler}
 */
public interface ITownResourceActionExecutor<T extends ITownResourceAction> {
    ITownResourceActionResult<T> execute(T action);
}
