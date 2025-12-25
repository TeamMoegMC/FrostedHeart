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
