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

import org.jetbrains.annotations.NotNull;

import com.teammoeg.chorda.block.entity.SyncableBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * 物品处理器的变更检测包装器。
 * 包装一个现有的{@link IItemHandler}，在物品插入、提取或设置时自动触发变更回调。
 * 通常用于方块实体中，当库存发生变化时自动调用{@code setChanged()}或同步数据。
 * <p>
 * Change-detection wrapper for item handlers.
 * Wraps an existing {@link IItemHandler} and automatically triggers a change callback
 * when items are inserted, extracted, or set.
 * Typically used in block entities to automatically call {@code setChanged()} or sync data
 * when inventory contents change.
 */
public class ChangeDetectedItemHandler implements IItemHandler,IItemHandlerModifiable,INBTSerializable<CompoundTag>{
	/** 被包装的物品处理器 / The wrapped item handler */
	IItemHandler handler;
	/** 内容变更时的回调 / Callback invoked when contents change */
	Runnable onchange;
	/**
	 * 使用指定的物品处理器和变更回调构造包装器。
	 * <p>
	 * Constructs a wrapper with the specified item handler and change callback.
	 *
	 * @param handler 被包装的物品处理器 / The item handler to wrap
	 * @param onchange 内容变更时的回调 / The callback to invoke when contents change
	 */
	public ChangeDetectedItemHandler(IItemHandler handler,Runnable onchange) {
		super();
		this.handler = handler;
		this.onchange = onchange;
	}
	/**
	 * 从方块实体创建一个包装器，变更时调用{@code setChanged()}。
	 * <p>
	 * Creates a wrapper from a block entity that calls {@code setChanged()} on changes.
	 *
	 * @param blockEntity 拥有此库存的方块实体 / The block entity owning this inventory
	 * @param nested 被包装的物品处理器 / The item handler to wrap
	 * @param <T> 方块实体类型 / The block entity type
	 * @return 变更检测物品处理器 / A change-detected item handler
	 */
	public static <T extends BlockEntity> ChangeDetectedItemHandler fromBESetChanged(T blockEntity,IItemHandler nested) {
		return new ChangeDetectedItemHandler(nested,()->{blockEntity.setChanged();});
	}
	
	
	
	/**
	 * 从可同步的方块实体创建一个包装器，变更时同时调用{@code setChanged()}和{@code syncData()}。
	 * <p>
	 * Creates a wrapper from a syncable block entity that calls both {@code setChanged()} and
	 * {@code syncData()} on changes.
	 *
	 * @param blockEntity 拥有此库存的可同步方块实体 / The syncable block entity owning this inventory
	 * @param nested 被包装的物品处理器 / The item handler to wrap
	 * @param <T> 方块实体类型，必须同时实现SyncableBlockEntity / The block entity type, must also implement SyncableBlockEntity
	 * @return 变更检测物品处理器 / A change-detected item handler
	 */
	public static <T extends BlockEntity& SyncableBlockEntity> IItemHandler fromBESynced(T blockEntity, IItemHandler nested) {
		return new ChangeDetectedItemHandler(nested,()->{blockEntity.setChanged();blockEntity.syncData();});
	}
	/** {@inheritDoc} */
	public int getSlots() {
		return handler.getSlots();
	}
	/** {@inheritDoc} */
	public @NotNull ItemStack getStackInSlot(int slot) {
		return handler.getStackInSlot(slot);
	}
	/**
	 * 向指定槽位插入物品。如果非模拟且实际插入了物品，则触发变更回调。
	 * <p>
	 * Inserts an item into the specified slot. Triggers the change callback if not simulating
	 * and items were actually inserted.
	 *
	 * {@inheritDoc}
	 */
	public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		ItemStack ret= handler.insertItem(slot, stack, simulate);
		if(!simulate&&ret.getCount()!=stack.getCount())
			onContentsChanged(slot);
		return ret;
	}
	/**
	 * 从指定槽位提取物品。如果非模拟且实际提取了物品，则触发变更回调。
	 * <p>
	 * Extracts items from the specified slot. Triggers the change callback if not simulating
	 * and items were actually extracted.
	 *
	 * {@inheritDoc}
	 */
	public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
		ItemStack extracted= handler.extractItem(slot, amount, simulate);
		if(!simulate&&!extracted.isEmpty())
			onContentsChanged(slot);
		return extracted;
	}
	/** {@inheritDoc} */
	public int getSlotLimit(int slot) {
		return handler.getSlotLimit(slot);
	}
	/** {@inheritDoc} */
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return handler.isItemValid(slot, stack);
	}
	/**
	 * 当槽位内容发生变化时调用。执行构造时提供的变更回调。
	 * <p>
	 * Called when slot contents change. Executes the change callback provided at construction.
	 *
	 * @param slot 发生变化的槽位索引 / The index of the slot that changed
	 */
	protected void onContentsChanged(int slot) {
		onchange.run();
	}
	/**
	 * 设置指定槽位的物品。要求被包装的处理器实现{@link IItemHandlerModifiable}。
	 * <p>
	 * Sets the item stack in the specified slot. Requires the wrapped handler to implement
	 * {@link IItemHandlerModifiable}.
	 *
	 * {@inheritDoc}
	 * @throws UnsupportedOperationException 如果被包装的处理器未实现IItemHandlerModifiable / if the wrapped handler does not implement IItemHandlerModifiable
	 */
	@Override
	public void setStackInSlot(int slot, @NotNull ItemStack stack) {
		if(handler instanceof IItemHandlerModifiable im) {
			im.setStackInSlot(slot, stack);
		}else
			throw new UnsupportedOperationException(handler+" does not implemented IItemHandlerModifiable!");
			
	}
	/**
	 * 将物品处理器序列化为NBT。要求被包装的处理器实现{@link INBTSerializable}。
	 * <p>
	 * Serializes the item handler to NBT. Requires the wrapped handler to implement
	 * {@link INBTSerializable}.
	 *
	 * {@inheritDoc}
	 * @throws UnsupportedOperationException 如果被包装的处理器未实现INBTSerializable / if the wrapped handler does not implement INBTSerializable
	 */
	@Override
	public CompoundTag serializeNBT() {
		if(handler instanceof INBTSerializable im) {
			return (CompoundTag) im.serializeNBT();
		}else
			throw new UnsupportedOperationException(handler+" does not implemented INBTSerializable!");
	}
	/**
	 * 从NBT反序列化物品处理器。要求被包装的处理器实现{@link INBTSerializable}。
	 * <p>
	 * Deserializes the item handler from NBT. Requires the wrapped handler to implement
	 * {@link INBTSerializable}.
	 *
	 * {@inheritDoc}
	 * @throws UnsupportedOperationException 如果被包装的处理器未实现INBTSerializable / if the wrapped handler does not implement INBTSerializable
	 */
	@Override
	public void deserializeNBT(CompoundTag nbt) {
		if(handler instanceof INBTSerializable im) {
			im.deserializeNBT(nbt);
		}else
			throw new UnsupportedOperationException(handler+" does not implemented INBTSerializable!");
	}

}
