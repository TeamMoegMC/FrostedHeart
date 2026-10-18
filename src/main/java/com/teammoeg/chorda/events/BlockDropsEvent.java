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

package com.teammoeg.chorda.events;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class BlockDropsEvent extends Event {

	private final BlockState state;
	private final ServerLevel level;
	private final BlockPos pos;
	private final @Nullable BlockEntity blockEntity;
	private final @Nullable Entity entity;
	private final ItemStack tool;
	List<ItemStack> drops;
	public BlockDropsEvent(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity, Entity entity, ItemStack tool, List<ItemStack> drops) {
		super();
		this.state = state;
		this.level = level;
		this.pos = pos;
		this.blockEntity = blockEntity;
		this.entity = entity;
		this.tool = tool;
		this.drops = drops;
	}
	public BlockState getState() {
		return state;
	}
	public ServerLevel getLevel() {
		return level;
	}
	public BlockPos getPos() {
		return pos;
	}
	public BlockEntity getBlockEntity() {
		return blockEntity;
	}
	public Entity getEntity() {
		return entity;
	}
	public ItemStack getTool() {
		return tool;
	}
	public List<ItemStack> getDrops() {
		return drops;
	}
}
