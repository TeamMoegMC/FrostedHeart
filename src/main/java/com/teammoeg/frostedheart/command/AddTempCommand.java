package com.teammoeg.frostedheart.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.climate.chunkdata.ITemperatureAdjust;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.util.text.StringTextComponent;

import java.util.Collection;

public class AddTempCommand {
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		LiteralArgumentBuilder<CommandSource> add = Commands.literal("settemp")
				.then(Commands.argument("position", BlockPosArgument.blockPos()).executes((ct) -> {
					ChunkData.removeTempAdjust(ct.getSource().getWorld(), BlockPosArgument.getBlockPos(ct, "position"));
					return Command.SINGLE_SUCCESS;
				}).then(Commands.argument("range", IntegerArgumentType.integer())
						.then(Commands.argument("temperature", IntegerArgumentType.integer()).executes((ct) -> {
							ChunkData.addCubicTempAdjust(ct.getSource().getWorld(),
									BlockPosArgument.getBlockPos(ct, "position"),
									IntegerArgumentType.getInteger(ct, "range"),
									IntegerArgumentType.getInteger(ct, "temperature"));
							return Command.SINGLE_SUCCESS;
						}))));
		LiteralArgumentBuilder<CommandSource> get = Commands.literal("gettemp")
				.executes((ct) -> {
					Collection<ITemperatureAdjust> adjs=ChunkData.getAdjust(ct.getSource().getWorld(),ct.getSource().asPlayer().getPosition());
					if(adjs.size()==0) {
						ct.getSource().sendFeedback(new StringTextComponent("No Active Adjust!"),true);
					}else {
						ct.getSource().sendFeedback(new StringTextComponent("Active Adjusts:"),true);
						for(ITemperatureAdjust adj:adjs) {
							ct.getSource().sendFeedback(new StringTextComponent("center:"+adj.getCenterX()+" "+adj.getCenterY()+" "+adj.getCenterZ()+",radius:"+adj.getRadius()+",temperature:"+adj.getValueAt(ct.getSource().asPlayer().getPosition())),true);
						}
					}
					return Command.SINGLE_SUCCESS;
				})
				.then(Commands.argument("position", BlockPosArgument.blockPos())
						.executes((ct) -> {
							Collection<ITemperatureAdjust> adjs=ChunkData.getAdjust(ct.getSource().getWorld(), BlockPosArgument.getBlockPos(ct, "position"));
							if(adjs.size()==0) {
								ct.getSource().sendFeedback(new StringTextComponent("No Active Adjust!"),true);
							}else {
								ct.getSource().sendFeedback(new StringTextComponent("Active Adjusts:"),true);
								for(ITemperatureAdjust adj:adjs) {
									ct.getSource().sendFeedback(new StringTextComponent("center:"+adj.getCenterX()+" "+adj.getCenterY()+" "+adj.getCenterZ()+",radius:"+adj.getRadius()+",temperature:"+adj.getValueAt(BlockPosArgument.getBlockPos(ct, "position"))),true);
								}
							}
							return Command.SINGLE_SUCCESS;
						}));
		dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermissionLevel(2)).then(add).then(get));
	}
}
