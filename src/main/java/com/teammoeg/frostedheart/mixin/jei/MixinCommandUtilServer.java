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
     * @reason I must do this hack because of stupid Essentials and Bukkit.
     */
    @Overwrite(remap = false)
    public static boolean hasPermission(PlayerEntity sender) {
        if (sender.isCreative()) {
            return true;
        } else {
            if (FHConfig.SERVER.fixEssJeiIssue.get()) {
                return FHConfig.SERVER.developers.get().contains(sender.getDisplayName().getString());
            } else {
                CommandNode<CommandSource> giveCommand = getGiveCommand(sender);
                CommandSource commandSource = sender.getCommandSource();
                if (giveCommand != null) {
                    return giveCommand.canUse(commandSource);
                } else {
                    MinecraftServer minecraftServer = sender.getServer();
                    if (minecraftServer == null) {
                        return false;
                    } else {
                        int opPermissionLevel = minecraftServer.getOpPermissionLevel();
                        return commandSource.hasPermissionLevel(opPermissionLevel);
                    }
                }
            }
        }
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
