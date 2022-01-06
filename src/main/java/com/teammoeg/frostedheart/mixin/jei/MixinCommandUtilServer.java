/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.mixin.jei;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.FHConfig;

import mezz.jei.util.CommandUtilServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;

@Mixin(CommandUtilServer.class)
public class MixinCommandUtilServer {
    /**
     * @author yuesha-yc
     * @reason I must do this hack because EssentialsX and Bukkit does not compat with JEI
     */
	@Inject(at=@At("HEAD"),method="hasPermission",cancellable=true,remap=false)
    private static void hasPermission(PlayerEntity sender,CallbackInfoReturnable<Boolean> cbib)  {
		if (FHConfig.SERVER.fixEssJeiIssue.get()) {
			MinecraftServer s=sender.getServer();
			if(s!=null)
				cbib.setReturnValue(sender.getCommandSource().hasPermissionLevel(s.getOpPermissionLevel()));
		}
    }
}
