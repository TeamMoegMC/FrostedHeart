package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import net.minecraft.item.ItemStack;

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
