package com.teammoeg.frostedheart.content.town.resource.action;

/**
 * 用于记录对城镇资源进行操作的具体内容。可以使用一个record类来实现这个接口。
 * <br>
 * Action本身不对资源进行操作，将其输入到对应的{@link ITownResourceActionExecutor}中，由executor来进行操作。
 * <br>
 * 通常的，一个城镇的资源可以接受多种Action，因此可通过{@link ITownResourceActionExecutorHandler}自动分配对应的executor，并执行。
 * <br>
 * Action实例一般放在{@link TownResourceActions}
 */
public interface ITownResourceAction {
    /**
     * 请在实现这个接口的类中定义一个static final int字段，用于保存Action的ID。
     * ID会在{@link AbstractTownResourceActionExecutorHandler}中使用，作为Map的key，用来找到Action对应的Executor。
     */
    //int getID();目前不需要ID，AbstractActionExecutorHandler依靠Class判断。

}
