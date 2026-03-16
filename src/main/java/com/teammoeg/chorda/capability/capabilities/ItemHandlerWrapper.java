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

package com.teammoeg.chorda.capability.capabilities;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * 物品处理器的延迟委托包装器。
 * 通过{@link Supplier}延迟获取实际的{@link IItemHandlerModifiable}实例，
 * 并将所有操作委托给它。适用于物品处理器引用可能在运行时发生变化的场景。
 * <p>
 * Lazy-delegation wrapper for item handlers.
 * Lazily obtains the actual {@link IItemHandlerModifiable} instance via a {@link Supplier}
 * and delegates all operations to it. Useful when the item handler reference may change at runtime.
 */
public class ItemHandlerWrapper implements IItemHandlerModifiable {
	/** 提供实际物品处理器实例的供应器 / Supplier providing the actual item handler instance */
	private final Supplier<IItemHandlerModifiable> intern;
	/** {@inheritDoc} */
	public void setStackInSlot(int slot, @NotNull ItemStack stack) {
		intern.get().setStackInSlot(slot, stack);
	}
	/** {@inheritDoc} */
	public int getSlots() {
		return intern.get().getSlots();
	}
	/** {@inheritDoc} */
	public @NotNull ItemStack getStackInSlot(int slot) {
		return intern.get().getStackInSlot(slot);
	}
	/** {@inheritDoc} */
	public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		return intern.get().insertItem(slot, stack, simulate);
	}
	/** {@inheritDoc} */
	public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
		return intern.get().extractItem(slot, amount, simulate);
	}
	/** {@inheritDoc} */
	public int getSlotLimit(int slot) {
		return intern.get().getSlotLimit(slot);
	}
	/** {@inheritDoc} */
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return intern.get().isItemValid(slot, stack);
	}
	/**
	 * 使用指定的供应器构造包装器。
	 * <p>
	 * Constructs a wrapper with the specified supplier.
	 *
	 * @param intern 提供实际物品处理器实例的供应器 / The supplier providing the actual item handler instance
	 */
	public ItemHandlerWrapper(Supplier<IItemHandlerModifiable> intern) {
		super();
		this.intern = intern;
	}


}
