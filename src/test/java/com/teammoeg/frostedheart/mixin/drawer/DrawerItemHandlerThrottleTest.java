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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.LongSupplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;

class DrawerItemHandlerThrottleTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void insertionMovesOneItemAndWaitsTenTicks() {
		MutableGameTime time = new MutableGameTime();
		ItemStackHandler backing = new ItemStackHandler(1);
		DrawerItemHandlerThrottle handler = handler(backing, time);
		ItemStack input = new ItemStack(Items.STONE, 4);

		assertEquals(3, handler.insertItem(0, input, true).getCount());
		assertEquals(0, backing.getStackInSlot(0).getCount());
		assertEquals(3, handler.insertItem(0, input, false).getCount());
		assertEquals(1, backing.getStackInSlot(0).getCount());

		time.tick = 9;
		assertEquals(4, handler.insertItem(0, input, false).getCount());
		assertEquals(1, backing.getStackInSlot(0).getCount());

		time.tick = 10;
		assertEquals(3, handler.insertItem(0, input, false).getCount());
		assertEquals(2, backing.getStackInSlot(0).getCount());
	}

	@Test
	void extractionMovesOneItemAndWaitsFortyTicks() {
		MutableGameTime time = new MutableGameTime();
		ItemStackHandler backing = new ItemStackHandler(1);
		backing.setStackInSlot(0, new ItemStack(Items.STONE, 4));
		DrawerItemHandlerThrottle handler = handler(backing, time);

		assertEquals(1, handler.extractItem(0, 64, true).getCount());
		assertEquals(4, backing.getStackInSlot(0).getCount());
		assertEquals(1, handler.extractItem(0, 64, false).getCount());
		assertEquals(3, backing.getStackInSlot(0).getCount());

		time.tick = 39;
		assertTrue(handler.extractItem(0, 64, false).isEmpty());
		assertEquals(3, backing.getStackInSlot(0).getCount());

		time.tick = 40;
		assertEquals(1, handler.extractItem(0, 64, false).getCount());
		assertEquals(2, backing.getStackInSlot(0).getCount());
	}

	@Test
	void wrappersShareCooldownStateButInputAndOutputAreIndependent() {
		MutableGameTime time = new MutableGameTime();
		ItemStackHandler backing = new ItemStackHandler(1);
		backing.setStackInSlot(0, new ItemStack(Items.STONE, 2));
		DrawerItemHandlerThrottle.State state = state(10, 40);
		DrawerItemHandlerThrottle first = new DrawerItemHandlerThrottle(backing, time, state);
		DrawerItemHandlerThrottle second = new DrawerItemHandlerThrottle(backing, time, state);

		assertEquals(1, first.insertItem(0, new ItemStack(Items.STONE, 2), false).getCount());
		assertEquals(2, second.insertItem(0, new ItemStack(Items.STONE, 2), false).getCount());
		assertEquals(1, second.extractItem(0, 64, false).getCount());
		assertTrue(first.extractItem(0, 64, false).isEmpty());
	}

	@Test
	void cooldownsUseConfiguredTickValues() {
		MutableGameTime time = new MutableGameTime();
		ItemStackHandler backing = new ItemStackHandler(1);
		backing.setStackInSlot(0, new ItemStack(Items.STONE, 2));
		DrawerItemHandlerThrottle handler = new DrawerItemHandlerThrottle(backing, time, state(3, 7));

		assertTrue(handler.insertItem(0, new ItemStack(Items.STONE), false).isEmpty());
		time.tick = 2;
		assertEquals(1, handler.insertItem(0, new ItemStack(Items.STONE), false).getCount());
		time.tick = 3;
		assertTrue(handler.insertItem(0, new ItemStack(Items.STONE), false).isEmpty());

		assertEquals(1, handler.extractItem(0, 64, false).getCount());
		time.tick = 9;
		assertTrue(handler.extractItem(0, 64, false).isEmpty());
		time.tick = 10;
		assertEquals(1, handler.extractItem(0, 64, false).getCount());
	}

	private static DrawerItemHandlerThrottle handler(ItemStackHandler backing, LongSupplier time) {
		return new DrawerItemHandlerThrottle(backing, time, state(10, 40));
	}

	private static DrawerItemHandlerThrottle.State state(int inputCooldownTicks, int outputCooldownTicks) {
		return new DrawerItemHandlerThrottle.State(() -> inputCooldownTicks, () -> outputCooldownTicks);
	}

	private static final class MutableGameTime implements LongSupplier {
		private long tick;

		@Override
		public long getAsLong() {
			return tick;
		}
	}
}
