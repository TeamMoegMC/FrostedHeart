/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.compat.curios.CuriosCompat;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.WearableThermalState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static com.teammoeg.frostedheart.infrastructure.command.CommandHelper.literal;
import static com.teammoeg.frostedheart.infrastructure.command.CommandHelper.registerCommand;

/**
 * OP-only development controls for reproducible wearable thermal reservoir stacks.
 * <p>
 * The observer is off unless a player explicitly starts it. It only reads the
 * dedicated Curios slot and never participates in production heat exchange.
 */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WarmStoneTestCommand {
    static final String COMMAND_NAME = "fh_warm_stone_test";
    static final int DEFAULT_OBSERVATION_INTERVAL_TICKS = 20;
    private static final Map<UUID, Observation> OBSERVATIONS = new HashMap<>();

    private WarmStoneTestCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        registerCommand(event.getDispatcher(), warmStoneTestCommand());
    }

    static LiteralArgumentBuilder<CommandSourceStack> warmStoneTestCommand() {
        return literal(COMMAND_NAME)
                .requires(source -> source.hasPermission(2))
                .then(giveCommand())
                .then(observeCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> giveCommand() {
        return literal("give")
                .then(giveItemCommand("warm_stone", () -> FHItems.warm_stone.get()))
                .then(giveItemCommand("hot_water_bag", () -> FHItems.hot_water_bag.get()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> giveItemCommand(
            String itemName,
            Supplier<? extends Item> itemSupplier
    ) {
        LiteralArgumentBuilder<CommandSourceStack> itemCommand = literal(itemName);
        for (TestPreset preset : TestPreset.values()) {
            itemCommand.then(literal(preset.commandName)
                    .executes(context -> give(context, itemSupplier.get(), preset)));
        }
        return itemCommand;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> observeCommand() {
        return literal("observe")
                .then(literal("start")
                        .executes(context -> startObservation(
                                context, DEFAULT_OBSERVATION_INTERVAL_TICKS))
                        .then(Commands.argument("interval_ticks",
                                        IntegerArgumentType.integer(1, 1_200))
                                .executes(context -> startObservation(context,
                                        IntegerArgumentType.getInteger(context,
                                                "interval_ticks")))))
                .then(literal("status").executes(WarmStoneTestCommand::observationStatus))
                .then(literal("stop").executes(WarmStoneTestCommand::stopObservation));
    }

    private static int give(
            CommandContext<CommandSourceStack> context,
            Item item,
            TestPreset preset
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        double environmentTemperatureC = WorldTemperature.naturalAir(
                player.serverLevel(), player.blockPosition());
        ItemStack stack = createTestStack(item, preset, environmentTemperatureC);
        TestTemperatures temperatures = preset.temperaturesAt(environmentTemperatureC);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "FH_WARM_STONE_TEST_GIVE item=%s preset=%s core_c=%.1f surface_c=%.1f",
                itemId(item), preset.commandName, temperatures.coreTemperatureC,
                temperatures.surfaceTemperatureC)), false);
        return Command.SINGLE_SUCCESS;
    }

    static ItemStack createTestStack(
            Item item,
            TestPreset preset,
            double environmentTemperatureC
    ) {
        ItemStack stack = new ItemStack(item);
        TestTemperatures temperatures = preset.temperaturesAt(environmentTemperatureC);
        new WearableThermalState(temperatures.coreTemperatureC,
                temperatures.surfaceTemperatureC).writeTo(stack);
        return stack;
    }

    private static int startObservation(
            CommandContext<CommandSourceStack> context,
            int intervalTicks
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        long currentTick = player.serverLevel().getGameTime();
        OBSERVATIONS.put(player.getUUID(), new Observation(intervalTicks,
                currentTick + intervalTicks));
        context.getSource().sendSuccess(() -> Component.literal(
                "FH_WARM_STONE_OBSERVE started interval_ticks=" + intervalTicks), false);
        emitObservation(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int observationStatus(
            CommandContext<CommandSourceStack> context
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Observation observation = OBSERVATIONS.get(player.getUUID());
        if (observation == null) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "FH_WARM_STONE_OBSERVE inactive"), false);
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "FH_WARM_STONE_OBSERVE active interval_ticks="
                        + observation.intervalTicks), false);
        emitObservation(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int stopObservation(
            CommandContext<CommandSourceStack> context
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean wasActive = OBSERVATIONS.remove(player.getUUID()) != null;
        context.getSource().sendSuccess(() -> Component.literal(
                "FH_WARM_STONE_OBSERVE stopped active=" + wasActive), false);
        return Command.SINGLE_SUCCESS;
    }

    @SubscribeEvent
    public static void observeServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || OBSERVATIONS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Observation>> iterator = OBSERVATIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Observation> entry = iterator.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            Observation observation = entry.getValue();
            long currentTick = player.serverLevel().getGameTime();
            if (currentTick < observation.nextObservationTick) {
                continue;
            }
            emitObservation(player);
            entry.setValue(new Observation(observation.intervalTicks,
                    currentTick + observation.intervalTicks));
        }
    }

    @SubscribeEvent
    public static void removeLoggedOutPlayer(PlayerEvent.PlayerLoggedOutEvent event) {
        OBSERVATIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void clearStoppedServer(ServerStoppedEvent event) {
        OBSERVATIONS.clear();
    }

    private static void emitObservation(ServerPlayer player) {
        double playerCoreTemperatureC = PlayerTemperatureData.getCapability(player)
                .map(data -> data.getCoreBodyTemp() + 37.0D)
                .orElse(Double.NaN);
        String line = observationLine(player.getGameProfile().getName(),
                player.serverLevel().getGameTime(), playerCoreTemperatureC,
                CuriosCompat.getWearableThermalReservoirInWarmStoneSlot(player));
        FHMain.LOGGER.info(line);
        player.sendSystemMessage(Component.literal(line));
    }

    static String observationLine(
            String playerName,
            long gameTick,
            double playerCoreTemperatureC,
            ItemStack reservoirStack
    ) {
        String playerCore = Double.isFinite(playerCoreTemperatureC)
                ? String.format(Locale.ROOT, "%.3f", playerCoreTemperatureC)
                : "unavailable";
        if (reservoirStack == null || reservoirStack.isEmpty()) {
            return "FH_WARM_STONE_OBSERVE player=" + playerName
                    + " game_tick=" + gameTick
                    + " player_core_c=" + playerCore
                    + " reservoir=empty";
        }
        String item = itemId(reservoirStack.getItem());
        return WearableThermalState.read(reservoirStack).map(state -> String.format(Locale.ROOT,
                "FH_WARM_STONE_OBSERVE player=%s game_tick=%d player_core_c=%s reservoir=%s "
                        + "reservoir_core_c=%.3f reservoir_surface_c=%.3f",
                playerName, gameTick, playerCore, item, state.coreTemperatureC(),
                state.surfaceTemperatureC())).orElseGet(() ->
                "FH_WARM_STONE_OBSERVE player=" + playerName
                        + " game_tick=" + gameTick
                        + " player_core_c=" + playerCore
                        + " reservoir=" + item
                        + " reservoir_state=uninitialized");
    }

    private static String itemId(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? "unknown" : id.toString();
    }

    enum TestPreset {
        COLD("cold", -20.0D, -20.0D),
        ENVIRONMENT("environment", Double.NaN, Double.NaN),
        HOT("hot", 60.0D, 60.0D),
        CORE_HOT_SURFACE_COLD("core_hot_surface_cold", 60.0D, 0.0D);

        private final String commandName;
        private final double coreTemperatureC;
        private final double surfaceTemperatureC;

        TestPreset(String commandName, double coreTemperatureC, double surfaceTemperatureC) {
            this.commandName = commandName;
            this.coreTemperatureC = coreTemperatureC;
            this.surfaceTemperatureC = surfaceTemperatureC;
        }

        TestTemperatures temperaturesAt(double environmentTemperatureC) {
            if (this == ENVIRONMENT) {
                return new TestTemperatures(environmentTemperatureC, environmentTemperatureC);
            }
            return new TestTemperatures(coreTemperatureC, surfaceTemperatureC);
        }
    }

    record TestTemperatures(double coreTemperatureC, double surfaceTemperatureC) {
    }

    private record Observation(int intervalTicks, long nextObservationTick) {
    }
}
