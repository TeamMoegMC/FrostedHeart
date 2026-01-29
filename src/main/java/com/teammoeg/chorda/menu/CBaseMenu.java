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

package com.teammoeg.chorda.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import com.teammoeg.chorda.ChordaNetwork;
import com.teammoeg.chorda.client.cui.menu.DeactivatableSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.SyncableDataSlot;
import com.teammoeg.chorda.network.ContainerDataSyncMessageS2C;
import com.teammoeg.chorda.network.ContainerOperationMessageC2S;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.Lazy;

public abstract class CBaseMenu extends AbstractContainerMenu {
	public static class Validator implements Predicate<Player> {
		private static record BoundCheck(Supplier<Vec3> initial,AABB bounds) implements Predicate<Player> {
			@Override
			public boolean test(Player t) {
				Vec3 pos=t.position().subtract(initial.get());
				return bounds.contains(pos.x, pos.y, pos.z);
			}
		};
		private static record RadiusCheck(Supplier<Vec3> initial,float distsqr) implements Predicate<Player> {
			@Override
			public boolean test(Player t) {
				return t.position().distanceToSqr(initial.get()) <= distsqr;
			}
		};
		private static record LevelCheck(Supplier<Level> initial) implements Predicate<Player> {
			@Override
			public boolean test(Player t) {
				return initial.get()==t.level();
			}
		};
		private static record BlockEntityCheck(BlockEntity be) implements Predicate<Player> {
			@Override
			public boolean test(Player t) {
				return !be.isRemoved();
			}
		};
		private static record EntityCheck(Entity e) implements Predicate<Player> {
			@Override
			public boolean test(Player t) {
				return !e.isRemoved();
			}
		};
		List<Predicate<Player>> citeria=new ArrayList<>();
		public Validator() {
			super();
		}
		public Validator range(BlockPos center,float radius) {
			return range(()->Vec3.atCenterOf(center),radius);
		}
		public Validator bound(Supplier<Vec3> center,AABB bounds) {
			citeria.add(new BoundCheck(center,bounds));
			return this;
		}
		public Validator bound(BlockPos center,AABB bounds) {
			return bound(()->Vec3.atCenterOf(center),bounds);
		}
		public Validator range(Supplier<Vec3> center,float radius) {
			citeria.add(new RadiusCheck(center,radius*radius));
			return this;
		}

		public Validator level(Supplier<Level> level) {
			citeria.add(new LevelCheck(level));
			return this;
		}
		public Validator level(Level level) {
			citeria.add(new LevelCheck(()->level));
			return this;
		}
		public Validator blockEntity(BlockEntity block,float radius) {
			blockEntityWithoutRange(block);
			level(block::getLevel);
			return range(block.getBlockPos(),radius);
		}
		public Validator blockEntity(BlockEntity block,AABB bounds) {
			blockEntityWithoutRange(block);
			level(block::getLevel);
			return bound(block.getBlockPos(),bounds);
		}
		public Validator blockEntity(BlockEntity block) {
			return blockEntity(block,8);
		}
		public Validator blockEntityWithoutRange(BlockEntity block) {
			citeria.add(new BlockEntityCheck(block));
			return this;
		}
		public Validator entity(Entity entity,float radius) {
			entityWithoutRange(entity);
			level(entity::level);
			range(entity::position,radius);
			return this;
		}
		public Validator entity(Entity block) {
			return entity(block,8);
		}
		public Validator entityWithoutRange(Entity entity) {
			citeria.add(new EntityCheck(entity));
			return this;
		}
		public Validator custom(Predicate<Player> predicate) {
			citeria.add(predicate);
			return this;
		}
		public Validator custom(BooleanSupplier predicate) {
			citeria.add(p->predicate.getAsBoolean());
			return this;
		}
		@Override
		public boolean test(Player t) {
			for(Predicate<Player> p:citeria) {
				if(!p.test(t))return false;
			}
			return true;
		}
	}
	/**
	 * A simple builder to define the slot fill order when player shift-click its invertory slot
	 * It would start to fill from the earlier defined range, then the later
	 * */
	public static class QuickMoveStackBuilder {
		private static record Range(int start, int end, boolean reverse) {
			private Range(int slot) {
				this(slot, slot + 1, false);
			}

		}

		List<Range> ranges = new ArrayList<>();

		private QuickMoveStackBuilder() {
		}
		/**
		 * Define an empty behaviour
		 * */
		public static QuickMoveStackBuilder begin() {
			return new QuickMoveStackBuilder();
		}
		/**
		 * Define a single slot to fill
		 * */
		public static QuickMoveStackBuilder first(int slot) {
			return begin().then(slot);
		}
		/**
		 * Define a range of slot to fill
		 * 
		 * */
		public static QuickMoveStackBuilder first(int beginInclusive, int endExclusive) {
			return begin().then(beginInclusive, endExclusive);
		}
		/**
		 * Define a single slot to fill
		 * */
		public QuickMoveStackBuilder then(int slot) {
			ranges.add(new Range(slot));
			return this;
		}
		/**
		 * Define a range of slot to fill
		 * 
		 * */
		public QuickMoveStackBuilder then(int beginInclusive, int endExclusive) {
			ranges.add(new Range(beginInclusive, endExclusive, false));
			return this;
		}
		/**
		 * Define a range of slot to fill
		 * 
		 * @param reversed to define whether later slot to be filled first
		 * */
		public QuickMoveStackBuilder then(int beginInclusive, int endExclusive, boolean reversed) {
			ranges.add(new Range(beginInclusive, endExclusive, reversed));
			return this;
		}

		public Function<ItemStack, Boolean> build(CBaseMenu t) {
			return i -> {
				for (Range r : ranges) {
					if (t.moveItemStackTo(i, r.start(), r.end(), r.reverse()))
						return true;
				}
				return false;
			};

		}
	}

	protected final int INV_START;
	protected static final int INV_SIZE = 36;
	protected static final int INV_QUICK = 27;
	private final Lazy<Validator> validator=Lazy.of(()->buildValidator(new Validator()));
	protected Lazy<Function<ItemStack, Boolean>> moveFunction = Lazy.of(() -> defineQuickMoveStack().build(this));
	protected List<SyncableDataSlot<?>> specialDataSlots = new ArrayList<>();
	@Getter
	private Player player;
	/**
	 * Constructor of c base menu
	 * Implementation notes:
	 * You can call {@link #addPlayerInventory addPlayerInventory} to add defaulted player inventory slots
	 * You may override {@link #buildValidator(Validator) buildValidator} to define rules whether this menu should close
	 * You may override {@link #defineQuickMoveStack() defineQuickMoveStack} to define quickMoveStack(shift-click) behaviour of the menu slots,default ones is to fill from the first slot to the last one.
	 * You may use {@link CCustomMenuSlot} to broadcast ui changes to the client menu.
	 * @param inv_start start slot index of player inventory, for example, if the machine has 5 slots, then this should be 5
	 * 
	 * */
	public CBaseMenu(MenuType<?> pMenuType, int pContainerId, Player player, int inv_start) {
		super(pMenuType, pContainerId);
		this.INV_START = inv_start;
		this.player = player;
	}

	protected void addPlayerInventory(Inventory inv, int invX, int invY, int quickBarY) {
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 9; j++)
				addSlot(new Slot(inv, j + i * 9 + 9, invX + j * 18, invY + i * 18));
		for (int i = 0; i < 9; i++)
			addSlot(new Slot(inv, i, invX + i * 18, quickBarY));
	}
	/**
	 * Define quickmovestack logic, when a player shift-clicked a slot item in their inventory, then it would traverse this builder definition and fill slots in defined order 
	 * */
	public QuickMoveStackBuilder defineQuickMoveStack() {
		return QuickMoveStackBuilder.first(0, INV_START);
	}
	/**
	 * Build gui validator logic, when some condition in validator does not meet, then the ui would be closed.
	 * */
	@Nonnull
	protected Validator buildValidator(@Nonnull Validator builder) {
		return builder;
	}
	public boolean quickMoveIn(ItemStack slotStack) {
		return moveFunction.get().apply(slotStack);
	}
	public void setSlotVisible(boolean en) {
		for (Slot s : this.slots) {
			if (s instanceof DeactivatableSlot)
				((DeactivatableSlot) s).setActived(en);
		}
	}
	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		ItemStack itemStack = ItemStack.EMPTY;
		Slot slot = slots.get(index);

		if (slot != null && slot.hasItem()) {
			ItemStack slotStack = slot.getItem();
			itemStack = slotStack.copy();
			if (index < INV_START) {//move item from block to inventory
				if (!this.moveItemStackTo(slotStack, INV_QUICK +INV_START, INV_SIZE + INV_START, false))//first move item to quickbar
					if (!this.moveItemStackTo(slotStack, INV_START, INV_QUICK + INV_START, false)) {//then move item to inventory
						return ItemStack.EMPTY;
					}
				slot.onQuickCraft(slotStack, itemStack);
			} else if (index >= INV_START) {//move item from inventory
				if (!quickMoveIn(slotStack)) {//first try to move into block
					if (index < INV_QUICK + INV_START) {//if moving from quickbar, move to inventory
						if (!this.moveItemStackTo(slotStack, INV_QUICK + INV_START, INV_SIZE + INV_START, false))
							return ItemStack.EMPTY;
					} else if (index < INV_SIZE + INV_START && !this.moveItemStackTo(slotStack, INV_START, INV_QUICK + INV_START, false))//if moving from inventory, move to quickbar
						return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(slotStack, INV_START, INV_SIZE + INV_START, false)) {
				return ItemStack.EMPTY;
			}
			if (slotStack.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
			if (slotStack.getCount() == itemStack.getCount()) {
				return ItemStack.EMPTY;
			}
			slot.onTake(playerIn, slotStack);
		}
		return itemStack;
	}

	@Override
	public boolean moveItemStackTo(ItemStack pStack, int pStartIndex, int pEndIndex, boolean pReverseDirection) {
		boolean flag = false;
		int i = pStartIndex;
		if (pReverseDirection) {
			i = pEndIndex - 1;
		}

		if (pStack.isStackable()) {
			while (!pStack.isEmpty()) {
				if (pReverseDirection) {
					if (i < pStartIndex) {
						break;
					}
				} else if (i >= pEndIndex) {
					break;
				}

				Slot slot = this.slots.get(i);
				ItemStack itemstack = slot.getItem();
				if(slot.mayPlace(pStack))
					if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(pStack, itemstack)) {
						int j = itemstack.getCount() + pStack.getCount();
						int maxSize = Math.min(slot.getMaxStackSize(), pStack.getMaxStackSize());
						if (j <= maxSize) {
							pStack.setCount(0);
							itemstack.setCount(j);
							slot.setChanged();
							flag = true;
						} else if (itemstack.getCount() < maxSize) {
							pStack.shrink(maxSize - itemstack.getCount());
							itemstack.setCount(maxSize);
							slot.setChanged();
							flag = true;
						}
					}

				if (pReverseDirection) {
					--i;
				} else {
					++i;
				}
			}
		}

		if (!pStack.isEmpty()) {
			if (pReverseDirection) {
				i = pEndIndex - 1;
			} else {
				i = pStartIndex;
			}

			while (true) {
				if (pReverseDirection) {
					if (i < pStartIndex) {
						break;
					}
				} else if (i >= pEndIndex) {
					break;
				}

				Slot slot1 = this.slots.get(i);
				ItemStack itemstack1 = slot1.getItem();
				if (itemstack1.isEmpty() && slot1.mayPlace(pStack)) {
					if (pStack.getCount() > slot1.getMaxStackSize()) {
						slot1.setByPlayer(pStack.split(slot1.getMaxStackSize()));
					} else {
						slot1.setByPlayer(pStack.split(pStack.getCount()));
					}

					slot1.setChanged();
					flag = true;
					break;
				}

				if (pReverseDirection) {
					--i;
				} else {
					++i;
				}
			}
		}

		return flag;

	}

	public void receiveMessage(short btnId, int state) {

	}

	public void sendMessage(int btnId, int state) {
		ChordaNetwork.INSTANCE.sendToServer(new ContainerOperationMessageC2S(this.containerId, (short) btnId, state));
	}

	public void sendMessage(int btnId, boolean state) {
		ChordaNetwork.INSTANCE.sendToServer(new ContainerOperationMessageC2S(this.containerId, (short) btnId, state ? 1 : 0));
	}

	public void sendMessage(int btnId, float state) {
		ChordaNetwork.INSTANCE.sendToServer(new ContainerOperationMessageC2S(this.containerId, (short) btnId, Float.floatToRawIntBits(state)));
	}

	@Override
	public DataSlot addDataSlot(DataSlot pIntValue) {
		return super.addDataSlot(pIntValue);
	}

	@Override
	public void addDataSlots(ContainerData pArray) {
		super.addDataSlots(pArray);
	}

	public void addDataSlot(SyncableDataSlot<?> slot) {
		specialDataSlots.add(slot);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void processPacket(ContainerDataSyncMessageS2C packet) {
		packet.forEach((i, o) -> {
			((SyncableDataSlot) specialDataSlots.get(i)).setValue(o);
		});
	}

	@Override
	public void broadcastChanges() {

		super.broadcastChanges();
		if(!player.level().isClientSide) {
			ContainerDataSyncMessageS2C packet = new ContainerDataSyncMessageS2C();
			for (int i = 0; i < specialDataSlots.size(); i++) {
				SyncableDataSlot<?> slot = specialDataSlots.get(i);
				if (slot.checkForUpdate()) {
					packet.add(i, slot.getConverter(), slot.getValue());
				}
			}
			if (packet.hasData() && player != null)
				ChordaNetwork.INSTANCE.sendPlayer((ServerPlayer) player, packet);

		}
	}

	@Override
	public void broadcastFullState() {
		super.broadcastFullState();
		if(!player.level().isClientSide) {
			ContainerDataSyncMessageS2C packet = new ContainerDataSyncMessageS2C();
			for (int i = 0; i < specialDataSlots.size(); i++) {
				SyncableDataSlot<?> slot = specialDataSlots.get(i);
				slot.checkForUpdate();
				packet.add(i, slot.getConverter(), slot.getValue());
				
			}
			if (packet.hasData() && player != null)
				ChordaNetwork.INSTANCE.sendPlayer((ServerPlayer) player, packet);
		}
	}

	@Override
	public boolean stillValid(Player pPlayer) {
		return validator.get().test(pPlayer);
	}

}