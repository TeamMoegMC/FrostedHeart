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

package com.teammoeg.frostedheart.mixin.drawer;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public final class DrawerItemHandlerThrottle implements IItemHandler {
	private final IItemHandler delegate;
	private final LongSupplier gameTime;
	private final State state;

	public DrawerItemHandlerThrottle(IItemHandler delegate, LongSupplier gameTime, State state) {
		this.delegate = delegate;
		this.gameTime = gameTime;
		this.state = state;
	}

	@Override
	public int getSlots() {
		return delegate.getSlots();
	}

	@Override
	public @NotNull ItemStack getStackInSlot(int slot) {
		return delegate.getStackInSlot(slot);
	}

	@Override
	public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		long now = gameTime.getAsLong();
		if (stack.isEmpty() || !state.canInsert(now))
			return stack;

		ItemStack singleItem = stack.copyWithCount(1);
		ItemStack singleRemainder = delegate.insertItem(slot, singleItem, simulate);
		if (!singleRemainder.isEmpty())
			return stack;

		if (!simulate)
			state.recordInsert(now);
		return remainderAfterOne(stack);
	}

	@Override
	public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
		long now = gameTime.getAsLong();
		if (amount <= 0 || !state.canExtract(now))
			return ItemStack.EMPTY;

		ItemStack extracted = delegate.extractItem(slot, 1, simulate);
		if (!simulate && !extracted.isEmpty())
			state.recordExtract(now);
		return extracted;
	}

	@Override
	public int getSlotLimit(int slot) {
		return delegate.getSlotLimit(slot);
	}

	@Override
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return delegate.isItemValid(slot, stack);
	}

	private static ItemStack remainderAfterOne(ItemStack stack) {
		if (stack.getCount() == 1)
			return ItemStack.EMPTY;
		ItemStack remainder = stack.copy();
		remainder.shrink(1);
		return remainder;
	}

	public static final class State {
		private final IntSupplier inputCooldownTicks;
		private final IntSupplier outputCooldownTicks;
		private long nextInsertTick = Long.MIN_VALUE;
		private long nextExtractTick = Long.MIN_VALUE;

		public State(IntSupplier inputCooldownTicks, IntSupplier outputCooldownTicks) {
			this.inputCooldownTicks = inputCooldownTicks;
			this.outputCooldownTicks = outputCooldownTicks;
		}

		boolean canInsert(long gameTime) {
			return gameTime >= nextInsertTick;
		}

		boolean canExtract(long gameTime) {
			return gameTime >= nextExtractTick;
		}

		void recordInsert(long gameTime) {
			nextInsertTick = gameTime + inputCooldownTicks.getAsInt();
		}

		void recordExtract(long gameTime) {
			nextExtractTick = gameTime + outputCooldownTicks.getAsInt();
		}
	}
}
