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

package com.teammoeg.frostedresearch.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
@Cancelable
public class DrawDeskOpenEvent extends Event {
	final Player openPlayer;
	final Level level;
	final BlockPos pos;
	public DrawDeskOpenEvent(Player openPlayer, Level l, BlockPos pos) {
		super();
		this.openPlayer = openPlayer;
		this.level = l;
		this.pos = pos;
	}
	public Level getLevel() {
		return level;
	}
	public Player getOpenPlayer() {
		return openPlayer;
	}
	public BlockPos getPos() {
		return pos;
	}

}
