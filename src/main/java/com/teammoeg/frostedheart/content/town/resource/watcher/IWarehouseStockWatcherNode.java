package com.teammoeg.frostedheart.content.town.resource.watcher;

import com.teammoeg.frostedheart.content.town.buildings.warehouse.SimpleItemKey;

public interface IWarehouseStockWatcherNode {
    /**
     * 当资源持有者分配/更新 Watcher 时调用。
     * 宿主应保存该 Watcher 引用，并立即通过它配置监听的物品。
     */
    void updateWatcher(IWarehouseStockWatcher newWatcher);

    /**
     * 当监听的物品库存发生变化时回调。
     * @param itemKey   变化的物品（精确键）
     * @param newAmount 该物品在仓库中的最新总量
     */
    void onStockChange(SimpleItemKey itemKey, long newAmount);
}
