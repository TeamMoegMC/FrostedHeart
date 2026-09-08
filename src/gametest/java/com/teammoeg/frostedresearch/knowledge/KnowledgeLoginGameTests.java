/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.chorda.dataholders.team.TeamsAPI;
import com.teammoeg.frostedresearch.api.KnowledgeDataAPI;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.UUID;

/**
 * Exercises the real Forge/FTB first-login sequence that a plain state test cannot cover.
 */
@GameTestHolder("frostedresearch_knowledge_login")
@PrefixGameTestTemplate(false)
public final class KnowledgeLoginGameTests {
    private KnowledgeLoginGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void firstLoginWaitsForTeamCreation(GameTestHelper helper) {
        helper.assertTrue("ftbteams".equals(TeamsAPI.getAPI().getProviderName()),
                "this login test must run with the real FTB Teams provider");
        AtomicBoolean sawFirstLogin = new AtomicBoolean();
        Consumer<OnDatapackSyncEvent> earlyLogin = event -> {
            if (event.getPlayer() != null) {
                helper.assertTrue(TeamsAPI.getAPI().getTeamByPlayer(event.getPlayer()) == null,
                        "the new player must have no FTB team at the early datapack event");
                sawFirstLogin.set(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, OnDatapackSyncEvent.class, earlyLogin);
        var server = helper.getLevel().getServer();
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "knowledge-test"));
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        // Forge's login hooks inspect the channel; vanilla's mock-player helper leaves it absent.
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        connection.setProtocol(ConnectionProtocol.PLAY);
        try {
            server.getPlayerList().placeNewPlayer(connection, player);
            helper.assertTrue(sawFirstLogin.get(), "the real early login event must have fired");
            helper.assertTrue(TeamsAPI.getAPI().getTeamByPlayer(player) != null,
                    "FTB Teams must have created the player's team before knowledge login completes");
            var data = KnowledgeDataAPI.getData(player).get();
            helper.assertTrue(data.initialized(), "first login must initialize the real team knowledge component");
            long revision = data.mutationRevision();
            KnowledgeRuntime.datapackSync(new OnDatapackSyncEvent(helper.getLevel().getServer().getPlayerList(), null));
            helper.assertTrue(data.mutationRevision() == revision,
                    "reload sync must retain initialized knowledge without repeating initial grants");
            helper.succeed();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(earlyLogin);
            if (server.getPlayerList().getPlayers().contains(player)) server.getPlayerList().remove(player);
            channel.finishAndReleaseAll();
        }
    }
}
