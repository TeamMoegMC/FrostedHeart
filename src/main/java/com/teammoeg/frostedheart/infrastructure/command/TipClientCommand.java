package com.teammoeg.frostedheart.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipHelper;
import com.teammoeg.frostedheart.content.tips.TipManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.teammoeg.frostedheart.infrastructure.command.CommandHelper.*;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TipClientCommand {
    public static boolean editMode = false;

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        var client = literal("tipClient")
                        .then(literal("reload")
                                .executes(c -> {TipManager.INSTANCE.loadFromFile(); c.getSource().sendSuccess(() -> Component.literal("Loaded " + TipManager.INSTANCE.getAllTips().size() + " tip(s)"), true); return Command.SINGLE_SUCCESS;}))
                        .then(literal("unlockAll")
                                .executes(c -> {TipManager.state().unlockAll(); return Command.SINGLE_SUCCESS;}))
                        .then(literal("edit").then(string("id").suggests(TipHelper::suggest)
                                .executes(c -> {TipHelper.edit(StringArgumentType.getString(c, "id"), null); return Command.SINGLE_SUCCESS;})))
                        .then(literal("editMode")
                                .executes(c -> {editMode = !editMode; return Command.SINGLE_SUCCESS;}))
                        .then(literal("display").then(string("id").suggests(TipHelper::suggest)
                                .executes(TipClientCommand::clientDisplay)))
                        .then(literal("displayCustom")
                                .then(literal("json").then(string("json")
                                        .executes(TipClientCommand::clientDisplayJson)))
                                .then(string("title").then(string("content").then(integer("displayTime")
                                        .executes(TipClientCommand::clientDisplayCustom)))));

        for (String string : new String[]{FHMain.MODID, FHMain.ALIAS, FHMain.TWRID}) {
            dispatcher.register(Commands.literal(string).then(client));
        }
        dispatcher.register(client);
    }

    
    private static int clientDisplay(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        TipManager.display().general(id);
        return Command.SINGLE_SUCCESS;
    }

    private static int clientDisplayJson(CommandContext<CommandSourceStack> ctx) {
        String json = StringArgumentType.getString(ctx, "json");
        Tip tip = TipHelper.parse(json).copy()
                .temporary()
                .build();
        TipManager.display().general(tip);
        return Command.SINGLE_SUCCESS;
    }

    private static int clientDisplayCustom(CommandContext<CommandSourceStack> ctx) {
        String title = StringArgumentType.getString(ctx, "title");
        String content = StringArgumentType.getString(ctx, "content");
        int displayTime = IntegerArgumentType.getInteger(ctx, "displayTime");
        Tip tip = TipCommand.toTip(title, content, displayTime);
        TipManager.display().general(tip);
        return Command.SINGLE_SUCCESS;
    }
}
