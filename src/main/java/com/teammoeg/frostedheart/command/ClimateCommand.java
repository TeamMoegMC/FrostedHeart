package com.teammoeg.frostedheart.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.ClimateData;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.climate.chunkdata.ITemperatureAdjust;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.Collection;

public class ClimateCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> get = Commands.literal("get")
                .executes((ct) -> {
                           ct.getSource().sendFeedback(new StringTextComponent(ClimateData.get(ct.getSource().getWorld()).toString()),true);
                            return Command.SINGLE_SUCCESS;
                        });
        LiteralArgumentBuilder<CommandSource> rebuild = Commands.literal("rebuild")
                .executes((ct) -> {
                	
                    ClimateData.get(ct.getSource().getWorld()).resetTempEvent(ct.getSource().getWorld());
                    ct.getSource().sendFeedback(new StringTextComponent("Succeed!").mergeStyle(TextFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
        });
        LiteralArgumentBuilder<CommandSource> init = Commands.literal("init")
        .executes((ct) -> {
            ClimateData.get(ct.getSource().getWorld()).addInitTempEvent(ct.getSource().getWorld());
            ct.getSource().sendFeedback(new StringTextComponent("Succeed!").mergeStyle(TextFormatting.GREEN), false);
            return Command.SINGLE_SUCCESS;
        });

        dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermissionLevel(2)).then(Commands.literal("climate").then(get).then(init).then(rebuild)));
    }
}
