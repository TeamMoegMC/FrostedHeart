package com.teammoeg.frostedheart.content.town.resource.watcher;

import com.teammoeg.frostedheart.content.town.buildings.warehouse.SimpleItemKey;

public interface IWarehouseStockWatcher {
    /** 清空所有监听配置，准备重新设置。 */
    void reset();

    /** 添加一个要监听的物品。 */
    void addWatch(SimpleItemKey item);

    /** 移除一个物品的监听。 */
    void removeWatch(SimpleItemKey item);

    /** 设置是否监听所有物品（模糊模式）。 */
    void setWatchAll(boolean watchAll);

    /** 是否处于全监听模式。 */
    boolean isWatchAll();
}
