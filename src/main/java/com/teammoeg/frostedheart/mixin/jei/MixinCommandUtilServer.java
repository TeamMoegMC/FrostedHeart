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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.teammoeg.frostedheart.FHConfig;
import mezz.jei.util.CommandUtilServer;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CommandUtilServer.class)
public class MixinCommandUtilServer {
    /**
     * @author yuesha-yc
     * @reason I must do this hack because EssentialsX and Bukkit does not compat with JEI
     */
    @SuppressWarnings("resource")
	@Overwrite(remap = false)
    public static boolean hasPermission(PlayerEntity sender) {
        if (sender.isCreative()) {
            return true;
        }
		if (FHConfig.SERVER.fixEssJeiIssue.get()) {
		    return FHConfig.SERVER.developers.get().contains(sender.getDisplayName().getString());
		}
		CommandNode<CommandSource> giveCommand = getGiveCommand(sender);
		CommandSource commandSource = sender.getCommandSource();
		if (giveCommand != null) {
		    return giveCommand.canUse(commandSource);
		}
		MinecraftServer minecraftServer = sender.getServer();
		if (minecraftServer == null) {
		    return false;
		}
		int opPermissionLevel = minecraftServer.getOpPermissionLevel();
		return commandSource.hasPermissionLevel(opPermissionLevel);
    }

    private static CommandNode<CommandSource> getGiveCommand(PlayerEntity sender) {
        MinecraftServer minecraftServer = sender.getServer();
        if (minecraftServer == null) {
            return null;
        }
        Commands commandManager = minecraftServer.getCommandManager();
        CommandDispatcher<CommandSource> dispatcher = commandManager.getDispatcher();
        RootCommandNode<CommandSource> root = dispatcher.getRoot();
        return root.getChild("give");
    }
}
