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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * 行政操作也走统一知识事件入口。 / Commands share the ordinary knowledge transaction boundary.
 */
@Mod.EventBusSubscriber(modid = FRMain.MODID)
public final class KnowledgeCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(tree());
    }

    static LiteralArgumentBuilder<CommandSourceStack> tree() {
        var root = Commands.literal("knowledge");
        for (String operation : List.of("grant", "forget", "status", "dream")) {
            var action = Commands.literal(operation);
            if (operation.equals("grant") || operation.equals("forget")) action.requires(s -> s.hasPermission(2));
            root.then(keyed(action, (c, key) -> operate(c, operation, key)));
        }
        root.then(Commands.literal("discuss")
                .then(Commands.literal("accept").executes(c -> feedback(c, KnowledgeDiscussion.accept(c.getSource().getPlayerOrException()) ? "SUCCESS" : "NO_DISCUSSION")))
                .then(Commands.literal("invite").then(keyed(Commands.argument("player", EntityArgument.player()), (c, key) ->
                        feedback(c, KnowledgeDiscussion.invite(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player"), key) ? "SUCCESS" : "INVALID_DISCUSSION")))));
        root.then(Commands.literal("merge").requires(s -> s.hasPermission(2)).then(Commands.argument("source_team", UuidArgument.uuid()).executes(c -> {
            var source = com.teammoeg.frostedresearch.api.KnowledgeDataAPI.getData(UuidArgument.getUuid(c, "source_team"));
            if (source.isEmpty()) return feedback(c, "NOT_FOUND");
            KnowledgeService.forPlayer(c.getSource().getPlayerOrException()).merge(source.get().get());
            return feedback(c, "SUCCESS");
        })));
        root.then(Commands.literal("research").requires(source -> source.hasPermission(2))
                .then(Commands.literal("complete")
                        .then(Commands.argument("project", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(KnowledgeDefinitions.current().projects().keySet(), builder))
                                .executes(context -> completeProject(context, null))
                                .then(Commands.argument("idea", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(KnowledgeDefinitions.current().ideas().keySet(), builder))
                                        .executes(context -> completeProject(context, ResourceLocationArgument.getId(context, "idea")))))));
        return root;
    }

    private interface KeyCommand {
        int run(CommandContext<CommandSourceStack> context, KnowledgeKey key) throws CommandSyntaxException;
    }

    /**
     * Adds the identity arguments under a node: an observation by record UUID, an idea or a result by definition id.
     * Definition ids are resource locations, so they need no quotes, and loaded definitions are suggested.
     */
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T keyed(T node, KeyCommand command) {
        return node
                .then(Commands.literal("observation").then(Commands.argument("id", UuidArgument.uuid())
                        .executes(c -> command.run(c, KnowledgeKey.observation(UuidArgument.getUuid(c, "id"))))))
                .then(Commands.literal("idea").then(Commands.argument("id", ResourceLocationArgument.id())
                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(KnowledgeDefinitions.current().ideas().keySet(), b))
                        .executes(c -> command.run(c, KnowledgeKey.idea(ResourceLocationArgument.getId(c, "id"))))))
                .then(Commands.literal("result").then(Commands.argument("id", ResourceLocationArgument.id())
                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(KnowledgeDefinitions.current().results().keySet(), b))
                        .executes(c -> command.run(c, KnowledgeKey.result(ResourceLocationArgument.getId(c, "id"))))));
    }

    /**
     * Temporary development completion: use the real idea/run/result lifecycle, skipping tasks only.
     */
    private static int completeProject(CommandContext<CommandSourceStack> context, ResourceLocation requestedIdea)
            throws CommandSyntaxException {
        ResourceLocation project = ResourceLocationArgument.getId(context, "project");
        var definition = KnowledgeDefinitions.current().projects().get(project);
        if (definition == null) return feedback(context, "UNKNOWN_PROJECT");
        var player = context.getSource().getPlayerOrException();
        var service = KnowledgeService.forPlayer(player);
        KnowledgeKey origin = service.state().archive().entries().keySet().stream()
                .filter(key -> key.kind() == KnowledgeKey.Kind.IDEA && service.isActive(key))
                .filter(key -> requestedIdea == null || key.definitionId().equals(requestedIdea))
                .filter(key -> KnowledgeDefinitions.current().ideas().get(key.definitionId()).projects().contains(project))
                .sorted(Comparator.comparing(KnowledgeKey::id)).findFirst().orElse(null);
        if (origin == null) {
            context.getSource().sendFailure(Component.translatable("knowledge.frostedresearch.command.project_needs_idea"));
            return 0;
        }
        // A team has one open run per project; reuse it whatever idea began it.
        UUID run = service.openRun(project).map(com.teammoeg.frostedresearch.knowledge.state.ResearchHistory::id)
                .orElseGet(() -> service.startResearch(project, origin).orElse(null));
        if (run == null) {
            context.getSource().sendFailure(Component.translatable("knowledge.frostedresearch.command.project_unavailable"));
            return 0;
        }
        var results = service.completeResearch(run);
        Component title = Component.translatableWithFallback(definition.title(), definition.title());
        context.getSource().sendSuccess(() -> Component.translatable("knowledge.frostedresearch.command.project_completed", title), false);
        for (var result : results) {
            if (!result.succeeded() && result.status() != KnowledgeService.Status.ALREADY_OWNED)
                context.getSource().sendSuccess(() -> Component.translatable("knowledge.frostedresearch.command.result_retained"), false);
        }
        return 1;
    }

    private static int operate(CommandContext<CommandSourceStack> context, String operation, KnowledgeKey key) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var service = KnowledgeService.forPlayer(player);
        return switch (operation) {
            case "grant" ->
                    key.kind() == KnowledgeKey.Kind.OBSERVATION ? feedback(context, "USE_OBSERVATION_API") : feedback(context, service.learn(service.describe(key), AcquisitionSource.of("command", player), KnowledgeService.Grant.COMMAND).status().name());
            case "forget" -> feedback(context, service.forget(key).status().name());
            case "dream" -> feedback(context, service.setDreamTopic(player.getUUID(), key).status().name());
            default -> feedback(context, service.query(key).toString());
        };
    }

    private static int feedback(CommandContext<CommandSourceStack> context, String status) {
        context.getSource().sendSuccess(() -> Component.literal(status), false);
        return status.equals("SUCCESS") ? 1 : 0;
    }
}
