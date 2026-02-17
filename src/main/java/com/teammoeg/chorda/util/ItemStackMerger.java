package com.teammoeg.chorda.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具类：合并物品列表，将相同的物品堆叠到其最大堆叠上限。
 */
public class ItemStackMerger {

    /**
     * 内部键类，仅根据物品类型和 NBT 数据（包括耐久、附魔等）标识物品，
     * 忽略物品数量，用于分组统计。
     */
    private static class ItemStackKey {
        private final ItemStack prototype; // 数量为 1 的原型，用于比较

        public ItemStackKey(ItemStack stack) {
            // 复制一份并设置数量为 1，确保 equals/hashCode 不依赖数量
            ItemStack copy = stack.copy();
            copy.setCount(1);
            this.prototype = copy;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemStackKey that = (ItemStackKey) o;
            // 使用 Minecraft 原版方法判断两个物品是否完全相同（类型、NBT、耐久等）
            return ItemStack.isSameItemSameTags(this.prototype, that.prototype);
        }

        @Override
        public int hashCode() {
            // 基于物品注册名和 NBT 计算哈希码
            int result = prototype.getItem().hashCode();
            CompoundTag tag = prototype.getTag();
            if (tag != null) {
                result = 31 * result + tag.hashCode();
            }
            return result;
        }
    }

    /**
     * 合并给定的物品列表，将相同的物品尽可能堆叠到其最大堆叠上限。
     *
     * @param items 原始物品列表（可包含空物品）
     * @return 合并后的新物品列表
     */
    public static List<ItemStack> mergeItemStacks(List<ItemStack> items) {
        // 过滤掉空物品
        List<ItemStack> nonEmpty = items.stream()
                .filter(stack -> !stack.isEmpty())
                .toList();

        // 统计每种物品的总数量
        Map<ItemStackKey, Integer> totalCounts = new HashMap<>();
        for (ItemStack stack : nonEmpty) {
            ItemStackKey key = new ItemStackKey(stack);
            totalCounts.put(key, totalCounts.getOrDefault(key, 0) + stack.getCount());
        }

        // 存放合并后的结果
        List<ItemStack> result = new ArrayList<>();

        // 遍历每个物品种类，拆分堆叠
        for (Map.Entry<ItemStackKey, Integer> entry : totalCounts.entrySet()) {
            ItemStack prototype = entry.getKey().prototype; // 原型（数量为 1）
            int total = entry.getValue();                   // 该种类物品的总数量
            int maxStackSize = prototype.getMaxStackSize(); // 该物品的最大堆叠上限

            // 按最大堆叠上限拆分
            while (total > 0) {
                int count = Math.min(total, maxStackSize);
                ItemStack newStack = prototype.copy();
                newStack.setCount(count);
                result.add(newStack);
                total -= count;
            }
        }

        return result;
    }
}