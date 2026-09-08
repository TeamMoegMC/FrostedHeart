/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
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
        var root = Commands.literal("knowledge");
        for (String operation : List.of("grant", "forget", "status", "dream")) {
            var action = Commands.literal(operation);
            if (operation.equals("grant") || operation.equals("forget")) action.requires(s -> s.hasPermission(2));
            action.then(Commands.argument("kind", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("observation", "idea", "result"), b))
                    .then(Commands.argument("id", StringArgumentType.string()).executes(c -> operate(c, operation))));
            root.then(action);
        }
        root.then(Commands.literal("discuss")
                .then(Commands.literal("accept").executes(c -> feedback(c, KnowledgeDiscussion.accept(c.getSource().getPlayerOrException()) ? "SUCCESS" : "NO_DISCUSSION")))
                .then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("kind", StringArgumentType.word()).then(Commands.argument("id", StringArgumentType.string()).executes(c -> {
                            KnowledgeKey key = key(c);
                            if (key == null) return feedback(c, "INVALID_INPUT");
                            return feedback(c, KnowledgeDiscussion.invite(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player"), key) ? "SUCCESS" : "INVALID_DISCUSSION");
                        }))))));
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
        event.getDispatcher().register(root);
    }

    /**
     * Temporary development completion: use the real idea/run/result lifecycle, skipping tasks only.
     */
    private static int completeProject(CommandContext<CommandSourceStack> context, ResourceLocation requestedIdea)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
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
        UUID run = service.state().research().values().stream()
                .filter(record -> !record.completed() && record.project().equals(project) && record.idea().equals(origin))
                .map(com.teammoeg.frostedresearch.knowledge.state.ResearchHistory::id).findFirst()
                .orElseGet(() -> service.startResearch(project, origin).orElseThrow());
        var results = service.completeResearch(run);
        Component title = Component.translatableWithFallback(definition.title(), definition.title());
        context.getSource().sendSuccess(() -> Component.translatable("knowledge.frostedresearch.command.project_completed", title), false);
        for (var result : results) {
            if (!result.succeeded() && result.status() != KnowledgeService.Status.ALREADY_OWNED)
                context.getSource().sendSuccess(() -> Component.translatable("knowledge.frostedresearch.command.result_retained"), false);
        }
        return 1;
    }

    private static int operate(CommandContext<CommandSourceStack> context, String operation) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        KnowledgeKey key = key(context);
        if (key == null) return feedback(context, "INVALID_INPUT");
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

    private static KnowledgeKey key(CommandContext<CommandSourceStack> context) {
        try {
            var kind = KnowledgeKey.Kind.valueOf(StringArgumentType.getString(context, "kind").toUpperCase(Locale.ROOT));
            String id = StringArgumentType.getString(context, "id");
            return kind == KnowledgeKey.Kind.OBSERVATION ? KnowledgeKey.observation(UUID.fromString(id)) : new KnowledgeKey(kind, new ResourceLocation(id).toString());
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static int feedback(CommandContext<CommandSourceStack> context, String status) {
        context.getSource().sendSuccess(() -> Component.literal(status), false);
        return status.equals("SUCCESS") ? 1 : 0;
    }
}
