/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * 玩家双方确认主题；居民由其交互系统调用 KnowledgeService.discuss。 / Mutually confirmed discussions.
 */
public final class KnowledgeDiscussion {
    private record Invitation(UUID from, UUID fromTeam, UUID toTeam, KnowledgeKey topic, long expires) {
    }

    private static final Map<UUID, Invitation> invitations = new HashMap<>();

    private KnowledgeDiscussion() {
    }

    public static boolean invite(ServerPlayer from, ServerPlayer to, KnowledgeKey topic) {
        var source = KnowledgeService.forPlayer(from);
        var target = KnowledgeService.forPlayer(to);
        if (from == to || from.level() != to.level() || from.distanceToSqr(to) > 64 || !source.isActive(topic) || !target.isActive(topic))
            return false;
        invitations.put(to.getUUID(), new Invitation(from.getUUID(), source.teamId(), target.teamId(), topic, from.serverLevel().getGameTime() + 1200));
        to.sendSystemMessage(Component.translatable("knowledge.frostedresearch.discussion_invite", from.getDisplayName(), source.describe(topic).title())
                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/knowledge discuss accept"))));
        return true;
    }

    public static boolean accept(ServerPlayer to) {
        Invitation invite = invitations.remove(to.getUUID());
        if (invite == null) return false;
        ServerPlayer from = to.server.getPlayerList().getPlayer(invite.from());
        if (from == null || to.serverLevel().getGameTime() > invite.expires() || from.level() != to.level() || from.distanceToSqr(to) > 64)
            return false;
        var a = KnowledgeService.forPlayer(from);
        var b = KnowledgeService.forPlayer(to);
        if (!a.teamId().equals(invite.fromTeam()) || !b.teamId().equals(invite.toTeam()) || !a.isActive(invite.topic()) || !b.isActive(invite.topic()))
            return false;
        long day = KnowledgeRuntime.day(to);
        var ar = a.discuss(from.getUUID(), to.getUUID(), invite.topic(), day);
        var br = b.discuss(to.getUUID(), from.getUUID(), invite.topic(), day);
        from.sendSystemMessage(Component.translatable("knowledge.frostedresearch.discussion_result", Component.translatable("knowledge.frostedresearch.status." + ar.status().name().toLowerCase(java.util.Locale.ROOT))));
        to.sendSystemMessage(Component.translatable("knowledge.frostedresearch.discussion_result", Component.translatable("knowledge.frostedresearch.status." + br.status().name().toLowerCase(java.util.Locale.ROOT))));
        return ar.succeeded() || br.succeeded();
    }

    public static void clear() {
        invitations.clear();
    }
}
