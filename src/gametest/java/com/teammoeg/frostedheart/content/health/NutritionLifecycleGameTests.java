/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.health;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import com.teammoeg.frostedheart.content.health.handler.HealthScreenHandler;
import com.teammoeg.frostedheart.content.health.nutrition.PlayerNutritionState;
import com.teammoeg.frostedheart.content.health.screen.HealthStatMenu;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_nutrition_lifecycle")
@PrefixGameTestTemplate(false)
public final class NutritionLifecycleGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void hungerSettlesOnceAndDeadPlayersKeepNutritionAndOpenTheirNewMenu(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        double originalRate = FHConfig.SERVER.NUTRITION.nutritionConsumptionRate.get();
        boolean regeneration = level.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
        List<Object> observations = new ArrayList<>();
        try {
            level.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false, level.getServer());
            FHConfig.SERVER.NUTRITION.nutritionConsumptionRate.set(0.25D);
            try (Fixture fixture = new Fixture(helper)) {
                var player = fixture.player;
                var cap = capability(player);
                cap.set(PlayerNutritionState.uniform(15));
                player.getFoodData().setSaturation(0);
                player.getFoodData().setExhaustion(4.1F);
                tick(player);
                state(helper, 14.75F, cap, "one live hunger loss consumes exactly once");
                for (int i = 0; i < 100; i++) tick(player);
                state(helper, 14.75F, cap, "no repeated consumption without another hunger loss");
                observations.add(Map.of("case", "live-one-hunger", "nutritionAfter101Ticks", cap.get()));
            }
            for (double rate : new double[]{0.25D, 25.0D}) {
                FHConfig.SERVER.NUTRITION.nutritionConsumptionRate.set(rate);
                try (Fixture fixture = new Fixture(helper)) {
                    ServerPlayer old = fixture.player;
                    var cap = capability(old);
                    cap.set(PlayerNutritionState.uniform(15));
                    cap.addAttributes(old);
                    old.getFoodData().setSaturation(0);
                    old.getFoodData().setExhaustion(40);
                    HealthScreenHandler.open(old);
                    helper.assertTrue(old.containerMenu instanceof HealthStatMenu, "living player opens health data");
                    var oldMenu = old.containerMenu;
                    old.setHealth(0);
                    old.die(old.damageSources().genericKill());
                    helper.assertTrue(!oldMenu.stillValid(old), "menu becomes invalid as its owner dies");
                    for (int i = 0; i < 200; i++) tick(old);
                    state(helper, 15, cap, "dead ticks preserve nutrition even with pending exhaustion and an inflated rate");
                    helper.assertTrue(old.isRemoved(), "the original dead entity has been removed");
                    var beforeRequest = old.containerMenu;
                    HealthScreenHandler.open(old);
                    HealthScreenHandler.open(old);
                    helper.assertTrue(old.containerMenu == beforeRequest, "dead requests do not open a zero-filled menu");
                    fixture.player = old.server.getPlayerList().respawn(old, false);
                    ServerPlayer current = fixture.player;
                    state(helper, 15, capability(current), "respawn retains all four reserves");
                    helper.assertTrue(current.containerMenu instanceof HealthStatMenu,
                            "the deferred request automatically opens after the real respawn event");
                    var menu = (HealthStatMenu) current.containerMenu;
                    helper.assertTrue(menu.stillValid(current) && !menu.stillValid(old), "the menu belongs to the current entity");
                    helper.assertTrue(((DataSlot) menu.fat).get() == 1500, "the new menu sends 15%, not zero");
                    HealthScreenHandler.open(old);
                    helper.assertTrue(current.containerMenu instanceof HealthStatMenu
                                    && ((DataSlot) ((HealthStatMenu) current.containerMenu).fat).get() == 1500,
                            "an in-flight request carrying the old entity resolves to the current player");
                    observations.add(Map.of("case", "dead-wait-and-deferred-respawn-menu", "rate", rate,
                            "deadFood", old.getFoodData().getFoodLevel(), "nutrition", capability(current).get(),
                            "menuFatEncoded", ((DataSlot) ((HealthStatMenu) current.containerMenu).fat).get(),
                            "maxHealth", current.getMaxHealth()));
                }
            }
            try (Fixture fixture = new Fixture(helper)) {
                var old = fixture.player;
                old.setHealth(0);
                old.die(old.damageSources().genericKill());
                HealthScreenHandler.open(old);
                HealthScreenHandler.onLogout(new PlayerEvent.PlayerLoggedOutEvent(old));
                fixture.player = old.server.getPlayerList().respawn(old, false);
                helper.assertTrue(!(fixture.player.containerMenu instanceof HealthStatMenu), "logout clears an abandoned deferred request");
                observations.add(Map.of("case", "abandoned-request-cleared-on-logout"));
            }
        } finally {
            FHConfig.SERVER.NUTRITION.nutritionConsumptionRate.set(originalRate);
            level.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(regeneration, level.getServer());
        }
        Path output = Path.of("nutrition-fix");
        Files.createDirectories(output);
        Files.writeString(output.resolve("lifecycle-results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(observations));
        FHMain.LOGGER.info("NUTRITION_LIFECYCLE_REGRESSION,{}", new GsonBuilder().create().toJson(observations));
        helper.succeed();
    }

    private static void tick(ServerPlayer player) {
        player.tickCount++;
        player.tick();
        player.doTick();
    }

    private static NutritionCapability capability(ServerPlayer player) {
        return NutritionCapability.getCapability(player).orElseThrow(IllegalStateException::new);
    }

    private static void state(GameTestHelper helper, float expected, NutritionCapability cap, String message) {
        helper.assertTrue(cap.get().equals(PlayerNutritionState.uniform(expected)), message + ": " + cap.get());
    }

    private static final class Fixture implements AutoCloseable {
        ServerPlayer player;
        final EmbeddedChannel channel;

        Fixture(GameTestHelper helper) {
            var level = helper.getLevel();
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "nutrition-fix"));
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            connection.setProtocol(ConnectionProtocol.PLAY);
            level.getServer().getPlayerList().placeNewPlayer(connection, player);
            player.setGameMode(GameType.SURVIVAL);
            player.setInvulnerable(true);
            BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
            player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        }

        @Override public void close() {
            player.server.getPlayerList().remove(player);
            channel.finishAndReleaseAll();
        }
    }
}
