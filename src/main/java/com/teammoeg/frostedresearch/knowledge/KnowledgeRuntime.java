/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.chorda.events.TeamCreatedEvent;
import com.teammoeg.chorda.events.TeamLoadedEvent;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClockSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

/**
 * 队伍初始化及符合条件的睡眠联想。 / Lifecycle wiring and successful-sleep association.
 */
@Mod.EventBusSubscriber(modid = FRMain.MODID)
public final class KnowledgeRuntime {
    private KnowledgeRuntime() {
    }

    public static long day(ServerPlayer player) {
        return WorldClimate.getSec(player.server.overworld()) / WorldClockSource.secondsPerDay;
    }

    @SubscribeEvent
    public static void created(TeamCreatedEvent event) {
        KnowledgeService.forTeam(event.getTeamData(), 0).initialize();
    }

    @SubscribeEvent
    public static void loaded(TeamLoadedEvent event) {
        KnowledgeService.forTeam(event.getTeamData(), 0).initialize();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        // Architectury/FTB Teams establishes a new player's team earlier in this event.
        if (event.getEntity() instanceof ServerPlayer player) {
            KnowledgeService.forPlayer(player).initialize();
            TeamResearchService.sync(com.teammoeg.frostedresearch.api.KnowledgeDataAPI.getData(player).team());
        }
    }

    @SubscribeEvent
    public static void slept(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        for (ServerPlayer player : level.players())
            if (player.isSleeping() && player.getSleepTimer() >= 100) {
                var result = KnowledgeService.forPlayer(player).dream(player.getUUID(), day(player));
                if (result.succeeded())
                    player.sendSystemMessage(Component.translatable("knowledge.frostedresearch.dream_hint"));
            }
    }

    @SubscribeEvent
    public static void datapackSync(net.minecraftforge.event.OnDatapackSyncEvent event) {
        // The player-targeted form fires before PlayerLoggedInEvent, while a first-time
        // player's FTB team does not exist yet. Login sends knowledge after team setup.
        if (event.getPlayer() != null) return;
        // A reload targets already logged-in players, with the final tags installed.
        java.util.Set<java.util.UUID> sent = new java.util.HashSet<>();
        for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            var closure = com.teammoeg.frostedresearch.api.KnowledgeDataAPI.getData(player);
            if (sent.add(closure.team().getId())) {
                KnowledgeService.forPlayer(player).initialize();
                TeamResearchService.sync(closure.team());
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        KnowledgeDiscussion.clear();
    }
}
