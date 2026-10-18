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
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;

import java.util.HashSet;
import java.util.Set;

public class WarehouseStockWatcher implements IWarehouseStockWatcher {
    final IWarehouseStockWatcherNode node;
    final TeamTownResourceHolder holder;
    final Set<SimpleItemKey> myKeys = new HashSet<>();
    boolean watchAll = false;

    public WarehouseStockWatcher(IWarehouseStockWatcherNode node, TeamTownResourceHolder holder) {
        this.node = node;
        this.holder = holder;
    }

    @Override
    public void addWatch(SimpleItemKey item) {
        if (myKeys.add(item)) {
            holder.exactIndex.computeIfAbsent(item, k -> new HashSet<>()).add(this);
        }
    }

    @Override
    public void removeWatch(SimpleItemKey item) {
        if (myKeys.remove(item)) {
            Set<WarehouseStockWatcher> set = holder.exactIndex.get(item);
            if (set != null) {
                set.remove(this);
                if (set.isEmpty()) holder.exactIndex.remove(item);
            }
        }
    }

    @Override
    public void setWatchAll(boolean watchAll) {
        if (this.watchAll == watchAll) return;
        this.watchAll = watchAll;
        if (watchAll) {
//            holder.watchAllWatchers.add(this);
        } else {
//            holder.watchAllWatchers.remove(this);
        }
    }

    @Override
    public boolean isWatchAll() {
        return watchAll;
    }

    @Override
    public void reset() {
        // 移除所有精确监听
        for (SimpleItemKey key : myKeys) {
            Set<WarehouseStockWatcher> set = holder.exactIndex.get(key);
            if (set != null) {
                set.remove(this);
                if (set.isEmpty()) holder.exactIndex.remove(key);
            }
        }
        myKeys.clear();
        // 移除全监听
        setWatchAll(false);
    }

    // 获取宿主
    public IWarehouseStockWatcherNode getNode() {
        return node;
    }
}