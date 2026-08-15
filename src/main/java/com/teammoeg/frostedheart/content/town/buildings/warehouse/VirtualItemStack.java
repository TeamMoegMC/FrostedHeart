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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VirtualItemStack {
    private final SimpleItemKey key;
    private long amount;
    // 客户端惰性缓存的小写显示名，用于搜索过滤与按名称排序。
    // 不参与网络传输；同一实例的物品种类不变，缓存始终有效。
    private String cachedLowercaseName;

    public VirtualItemStack(SimpleItemKey key, long amount) {
        this.key = key;
        this.amount = amount;
    }

    public SimpleItemKey getKey() {
        return key;
    }

    public ItemStack getDisplayStack() {
        return key.toStack(1);
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    /**
     * 获取小写物品显示名（客户端惰性缓存，每种物品只计算一次）。
     * 用于仓库 GUI 的搜索过滤与按名称排序。
     */
    public String getLowercaseName() {
        if (cachedLowercaseName == null) {
            cachedLowercaseName = getDisplayStack().getHoverName().getString().toLowerCase(Locale.ROOT);
        }
        return cachedLowercaseName;
    }
}
