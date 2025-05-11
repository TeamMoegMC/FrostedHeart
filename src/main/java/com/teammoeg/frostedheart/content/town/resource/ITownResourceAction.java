package com.teammoeg.frostedheart.content.town.resource;

public interface ITownResourceAction {
    /**
     * 在传入的TownResourceManager上执行该实例所表示的操作
     * @param resourceHolder 执行操作所在的TownResourceHolder
     * @return 该操作的结果
     */
    IResourceActionResult apply(TownResourceHolder resourceHolder);
}
