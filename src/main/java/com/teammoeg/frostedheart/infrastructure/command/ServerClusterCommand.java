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

package com.teammoeg.frostedheart.infrastructure.command;

import java.util.Collection;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.clusterserver.ServerConnectionHelper;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerClusterCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("server_cluster")
                .then(Commands.argument("target", GameProfileArgument.gameProfile()).then(
                	Commands.literal("auth")
                        .executes(ct -> {
                        	Collection<GameProfile> gp=GameProfileArgument.getGameProfiles(ct, "target");
                        	if(gp.size()==0)
                        		return 0;
                        	for(GameProfile gps:gp) {
                        		ServerPlayer sp=ct.getSource().getServer().getPlayerList().getPlayer(gps.getId());
                        		sp.connection.send(new ClientboundSetTitleTextPacket(Components.str(ServerConnectionHelper.constructAuthMessage(gps.getName()))));
                        	}
                            return Command.SINGLE_SUCCESS;
                        })).then(
                        	Commands.literal("back")
                            .executes(ct -> {
                            	Collection<GameProfile> gp=GameProfileArgument.getGameProfiles(ct, "target");
                            	if(gp.size()==0)
                            		return 0;
                            	for(GameProfile gps:gp) {
                            		ServerPlayer sp=ct.getSource().getServer().getPlayerList().getPlayer(gps.getId());
                            		sp.connection.send(new ClientboundSetTitleTextPacket(Components.str(ServerConnectionHelper.constructBackMessage(gps.getName()))));
                            	}
                                return Command.SINGLE_SUCCESS;
                            })).then(
                            	Commands.literal("go").then(Commands.argument("ip", StringArgumentType.string()).then(Commands.argument("port", IntegerArgumentType.integer(0, 65536))
                            		.executes(ct -> {
                                    	Collection<GameProfile> gp=GameProfileArgument.getGameProfiles(ct, "target");
                                    	String ip=StringArgumentType.getString(ct, "ip")+IntegerArgumentType.getInteger(ct, "port");
                                    	if(gp.size()==0)
                                    		return 0;
                                    	for(GameProfile gps:gp) {
                                    		ServerPlayer sp=ct.getSource().getServer().getPlayerList().getPlayer(gps.getId());
                                    		sp.connection.send(new ClientboundSetTitleTextPacket(Components.str(ServerConnectionHelper.constructRedirectMessage(ip,false))));
                                    	}
                                        return Command.SINGLE_SUCCESS;
                                    }).then(Commands.literal("temporary")).executes(ct -> {
                                    	Collection<GameProfile> gp=GameProfileArgument.getGameProfiles(ct, "target");
                                    	String ip=StringArgumentType.getString(ct, "ip")+IntegerArgumentType.getInteger(ct, "port");
                                    	if(gp.size()==0)
                                    		return 0;
                                    	for(GameProfile gps:gp) {
                                    		ServerPlayer sp=ct.getSource().getServer().getPlayerList().getPlayer(gps.getId());
                                    		sp.connection.send(new ClientboundSetTitleTextPacket(Components.str(ServerConnectionHelper.constructRedirectMessage(ip,true))));
                                    	}
                                        return Command.SINGLE_SUCCESS;
                                    })
                            		))
                                )

                );
        for (String string : new String[]{FHMain.MODID, FHMain.ALIAS, FHMain.TWRID}) {
            dispatcher.register(Commands.literal(string).requires(s -> s.hasPermission(2)).then(cmd));
        }

    }
}
