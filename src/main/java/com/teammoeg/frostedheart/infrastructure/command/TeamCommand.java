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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataType;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.ClimateEvent;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TeamCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> getdata = Commands.literal("get")
        	.then(Commands.argument("player", EntityArgument.player())
        		.then(Commands.argument("type", StringArgumentType.string()).suggests((ct,sb)->{
        			for(SpecialDataType i:CTeamDataManager.get(EntityArgument.getPlayer(ct, "player")).getTypes())
        				sb=sb.suggest(i.getId());
        			return sb.buildFuture();
        		}).executes(ct->{
        			SpecialDataType<SpecialData> type=(SpecialDataType<SpecialData>) SpecialDataType.getType(StringArgumentType.getString(ct, "type"));
        			if(type==null)
        				ct.getSource().sendFailure(Components.literal("Invalid data type"));
        			else {
        				SpecialData data=CTeamDataManager.get(EntityArgument.getPlayer(ct, "player")).getData(type);
        				if(data==null) {
        					ct.getSource().sendFailure(Components.literal("Team does not contains specific data"));
        				}else {
        					ct.getSource().sendSuccess(()->{
								try {
									return NbtUtils.toPrettyComponent(type.saveData(NbtOps.INSTANCE, data));
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								return Components.literal("Can not get data of type");
							}, true);
        				}
        			}
        			
        			return Command.SINGLE_SUCCESS;
        		})));
        
        
        LiteralArgumentBuilder<CommandSourceStack> data = Commands.literal("data").then(getdata);

        for (String string : new String[]{FHMain.MODID, FHMain.ALIAS, FHMain.TWRID}) {
            dispatcher.register(Commands.literal(string).requires(s -> s.hasPermission(2)).then(Commands.literal("team").then(data)));
        }

      }
}
