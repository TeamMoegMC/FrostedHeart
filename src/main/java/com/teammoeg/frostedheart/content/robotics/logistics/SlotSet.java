/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import net.minecraft.world.item.ItemStack;

public interface SlotSet extends Set<LogisticSlot> {
	static class EmptySlotSet extends AbstractCollection<LogisticSlot> implements SlotSet{
		private EmptySlotSet() {
			super();
		}
		@Override
		public boolean testStack(ItemStack out, boolean strictNBT) {
			return true;
		}
		@Override
		public int size() {
			return 0;
		}
		@Override
		public Iterator<LogisticSlot> iterator() {
			return new Iterator<LogisticSlot>(){
				@Override
				public boolean hasNext() {
					return false;
				}
				@Override
				public LogisticSlot next() {
					throw new NoSuchElementException();
				}
			};
		}
		@Override
		public boolean add(LogisticSlot e) {
			return false;
		}
		@Override
		public boolean addAll(Collection<? extends LogisticSlot> c) {
			return false;
		}
		
	}
	public static final SlotSet EMPTY=new EmptySlotSet();
	public boolean testStack(ItemStack out,boolean strictNBT);
}
