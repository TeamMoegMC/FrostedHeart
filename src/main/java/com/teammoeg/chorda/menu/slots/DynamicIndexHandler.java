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

package com.teammoeg.chorda.menu.slots;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 动态索引槽位，允许在运行时改变其指向的容器索引。
 * <p>
 * Dynamic index slot that allows changing the container index it points to at runtime.
 * Unlike standard slots that always reference a fixed container index, this slot uses a mutable
 * {@code index} field to determine which container slot to read from and write to.
 */
public class DynamicIndexHandler extends Slot
{
    /**
     * 构造一个动态索引槽位。
     * <p>
     * Constructs a dynamic index handler slot.
     *
     * @param pContainer 容器 / The container
     * @param pSlot 初始槽位索引 / The initial slot index
     * @param pX 槽位X坐标 / The slot X position
     * @param pY 槽位Y坐标 / The slot Y position
     */
    public DynamicIndexHandler(Container pContainer, int pSlot, int pX, int pY) {
		super(pContainer, pSlot, pX, pY);
	}

	/** 当前动态索引，指向容器中的实际槽位 / Current dynamic index pointing to the actual slot in the container */
	private int index;


    /**
     * 检查物品是否可以放入此槽位，不允许放入空物品堆。
     * <p>
     * Checks if the item stack can be placed in this slot. Empty stacks are not allowed.
     *
     * @param stack 要放置的物品堆 / The item stack to place
     * @return 物品堆非空时返回true / True if the stack is not empty
     */
    @Override
    public boolean mayPlace(ItemStack stack)
    {
        if (stack.isEmpty())
            return false;
        return true;
    }

    /**
     * 使用动态索引从容器中获取物品。
     * <p>
     * Gets the item from the container using the dynamic index.
     *
     * @return 当前动态索引处的物品堆 / The item stack at the current dynamic index
     */
    @Override
    public ItemStack getItem()
    {
        return container.getItem(index);
    }

    /**
     * 使用动态索引将物品设置到容器中。
     * <p>
     * Sets the item in the container using the dynamic index.
     *
     * @param stack 要设置的物品堆 / The item stack to set
     */
    @Override
    public void set(ItemStack stack)
    {
    	container.setItem(index, stack);
        this.setChanged();
    }


    /**
     * 使用动态索引从容器中移除指定数量的物品。
     * <p>
     * Removes a specified amount of items from the container using the dynamic index.
     *
     * @param amount 要移除的数量 / The amount to remove
     * @return 被移除的物品堆 / The removed item stack
     */
    @Override
    public ItemStack remove(int amount)
    {
    	return this.container.removeItem(index, amount);
    }

}
