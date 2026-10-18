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
