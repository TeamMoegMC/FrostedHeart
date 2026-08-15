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

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.Locale;

/**
 * 仓库 GUI 的物品排序模式（仅客户端使用）。
 * 点击排序按钮时按声明顺序循环切换。
 */
public enum WarehouseSortMode {
    AMOUNT_DESC,
    AMOUNT_ASC,
    NAME_ASC,
    NAME_DESC;

    // 最终 tie-break：物品注册名，保证同名同数量（不同 NBT）时顺序确定，避免排序抖动
    private static final Comparator<VirtualItemStack> BY_ITEM_ID =
            Comparator.comparing(v -> BuiltInRegistries.ITEM.getKey(v.getKey().item()).toString());

    private static final Comparator<VirtualItemStack> AMOUNT_DESC_COMPARATOR =
            Comparator.comparingLong(VirtualItemStack::getAmount).reversed()
                    .thenComparing(VirtualItemStack::getLowercaseName)
                    .thenComparing(BY_ITEM_ID);

    private static final Comparator<VirtualItemStack> AMOUNT_ASC_COMPARATOR =
            Comparator.comparingLong(VirtualItemStack::getAmount)
                    .thenComparing(VirtualItemStack::getLowercaseName)
                    .thenComparing(BY_ITEM_ID);

    private static final Comparator<VirtualItemStack> NAME_ASC_COMPARATOR =
            Comparator.comparing(VirtualItemStack::getLowercaseName)
                    .thenComparing(Comparator.comparingLong(VirtualItemStack::getAmount).reversed())
                    .thenComparing(BY_ITEM_ID);

    private static final Comparator<VirtualItemStack> NAME_DESC_COMPARATOR =
            Comparator.comparing(VirtualItemStack::getLowercaseName, Comparator.reverseOrder())
                    .thenComparing(Comparator.comparingLong(VirtualItemStack::getAmount).reversed())
                    .thenComparing(BY_ITEM_ID);

    /**
     * 循环切换到下一个排序模式。
     */
    public WarehouseSortMode next() {
        WarehouseSortMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    /**
     * 该模式对应的比较器（预先构建的常量，避免每次排序重复分配 lambda）。
     */
    public Comparator<VirtualItemStack> comparator() {
        return switch (this) {
            case AMOUNT_DESC -> AMOUNT_DESC_COMPARATOR;
            case AMOUNT_ASC -> AMOUNT_ASC_COMPARATOR;
            case NAME_ASC -> NAME_ASC_COMPARATOR;
            case NAME_DESC -> NAME_DESC_COMPARATOR;
        };
    }

    /**
     * 排序按钮上显示的本地化标签。
     */
    public Component label() {
        return Component.translatable("gui.frostedheart.warehouse.sort." + name().toLowerCase(Locale.ROOT));
    }
}
