package com.teammoeg.frostedheart.content.town.resource;

public interface IResourceActionResult {
    ITownResourceAction getAction();

    /**
     * 已弃用，目前打算让TownResourceHolder里面的方法自己做数据同步。
     * <br>
     * 此方法仅用于数据同步。
     * 不管容量等因素，将这个结果应用到城镇资源。
     * 添加/减少的量为实际添加/减少的数量。
     * @param resourceHolder 将要应用到的城镇资源
     */
    @Deprecated
    void applyForce(TownResourceHolder resourceHolder);
}
