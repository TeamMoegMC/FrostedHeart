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

package com.teammoeg.frostedheart.infrastructure.command;

import java.util.Collection;
import java.util.Map.Entry;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.clusterserver.AuthConfig;
import com.teammoeg.frostedheart.clusterserver.AuthConfig.ServerEntry;
import com.teammoeg.frostedheart.clusterserver.ServerConnectionHelper;
import com.teammoeg.frostedheart.clusterserver.network.S2CRedirectPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.GameProfileArgument.Result;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
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
        RequiredArgumentBuilder<CommandSourceStack, Result> permCmd = Commands.argument("target", GameProfileArgument.gameProfile()).then(
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
                            	Commands.literal("go").then(Commands.argument("ip", StringArgumentType.string())
                            		.executes(ct -> {
                                    	Collection<GameProfile> gp=GameProfileArgument.getGameProfiles(ct, "target");
                                    	String ip=StringArgumentType.getString(ct, "ip");
                                    	if(gp.size()==0)
                                    		return 0;
                                    	for(GameProfile gps:gp) {
                                    		ServerPlayer sp=ct.getSource().getServer().getPlayerList().getPlayer(gps.getId());
                                    		sp.connection.send(new ClientboundSetTitleTextPacket(Components.str(ServerConnectionHelper.constructRedirectMessage(ip,false))));
                                    	}
                                        return Command.SINGLE_SUCCESS;
                                    })
                            		.then(Commands.argument("port", IntegerArgumentType.integer(0, 65536))
                            		.executes(ct -> {
                                    	Collection<GameProfile> gp=GameProfileArgument.getGameProfiles(ct, "target");
                                    	String ip=StringArgumentType.getString(ct, "ip")+":"+IntegerArgumentType.getInteger(ct, "port");
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

                ;
        LiteralArgumentBuilder<CommandSourceStack> cmd=Commands.literal("cs").then(Commands.literal("list").executes(ct->{
        	CommandSourceStack sp=ct.getSource();
        	sp.sendSystemMessage(Components.translatable("message.frostedheart.servers").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
        	for(Entry<String, ServerEntry> se:AuthConfig.servers.entrySet()) {
        		if(!se.getValue().hidden)
        			sp.sendSystemMessage(Components.str(se.getValue().name).withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
												Components.str(se.getValue().desc)))));
        	}
        	 return Command.SINGLE_SUCCESS;
        }).then(Commands.literal("join").then(Commands.argument("server", StringArgumentType.string()).suggests((ct,builder)->{
        	AuthConfig.servers.values().stream().filter(n->!n.hidden).filter(n->n.name.startsWith(ct.getInput())).forEach(n->builder.suggest(n.name, Components.str(n.desc)));
        	return builder.buildFuture();
        }).executes(ct->{
        	String server=StringArgumentType.getString(ct, "server");
        	ServerEntry serv=AuthConfig.servers.get(server);
        	if(serv!=null) {
        		FHNetwork.INSTANCE.sendPlayer(ct.getSource().getPlayerOrException(), new S2CRedirectPacket(serv.address,false));
        	}else {
        		ct.getSource().sendSystemMessage(Components.translatable("message.frostedheart.no_such_server"));
        	}
        	
        	return Command.SINGLE_SUCCESS;
        }))));
        for (String string : new String[]{FHMain.MODID, FHMain.ALIAS, FHMain.TWRID}) {
            dispatcher.register(Commands.literal(string).requires(s -> s.hasPermission(2)).then(Commands.literal("server_cluster").then(permCmd)));
        }
        dispatcher.register(cmd);

    }
}
