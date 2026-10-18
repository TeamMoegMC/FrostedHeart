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

package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.common.util.LazyOptional;
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

	public PlayerListMixin() {
	}
	@Inject(method = "sendLevelInfo", at = @At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;isRaining()Z", ordinal = 0, remap=true), remap=true, cancellable = true)
	public void fh$sendLevelInfo(ServerPlayer pPlayer, ServerLevel pLevel,CallbackInfo cbi) {
		LazyOptional<PlayerTemperatureData> cap=PlayerTemperatureData.getCapability(pPlayer);
		if(cap.isPresent()) {
			cap.resolve().get().sendInitWeather(pPlayer,pLevel);
			cbi.cancel();
		}
	}
}
