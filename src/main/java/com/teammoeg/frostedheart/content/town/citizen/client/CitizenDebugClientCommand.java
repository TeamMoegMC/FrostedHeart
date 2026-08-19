/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import static com.teammoeg.frostedheart.infrastructure.command.CommandHelper.bool;
import static com.teammoeg.frostedheart.infrastructure.command.CommandHelper.integer;
import static com.teammoeg.frostedheart.infrastructure.command.CommandHelper.literal;
import static com.teammoeg.frostedheart.infrastructure.command.CommandHelper.registerCommand;
import static com.teammoeg.frostedheart.infrastructure.command.CommandHelper.string;

import java.util.Locale;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client commands for deterministic citizen render benchmarks and counters. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CitizenDebugClientCommand {

	private CitizenDebugClientCommand() {
	}

	@SubscribeEvent
	public static void register(RegisterClientCommandsEvent event) {
		var command = literal("citizen_debug")
				.then(literal("metrics")
						.executes(CitizenDebugClientCommand::showMetrics)
						.then(literal("reset").executes(CitizenDebugClientCommand::resetMetrics)))
				.then(literal("backend")
						.then(literal("status").executes(CitizenDebugClientCommand::showBackendStatus))
						.then(literal("cpu_batch").executes(CitizenDebugClientCommand::useCpuBackend))
						.then(literal("flywheel_m3").executes(CitizenDebugClientCommand::useFlywheelPocBackend))
						.then(literal("flywheel_poc").executes(CitizenDebugClientCommand::useFlywheelPocBackend)))
				.then(literal("overlay")
						.then(bool("enabled").executes(CitizenDebugClientCommand::setOverlay)))
				.then(literal("benchmark")
						.then(literal("status").executes(CitizenDebugClientCommand::showBenchmarkStatus))
						.then(literal("clear").executes(CitizenDebugClientCommand::clearBenchmark))
						.then(literal("load")
								.then(integer("count")
										.suggests((context, builder) -> SharedSuggestionProvider.suggest(
												new String[] { "32", "64", "256", "1024" }, builder))
										.then(string("mode")
												.suggests((context, builder) -> SharedSuggestionProvider.suggest(
														new String[] { "moving", "sleeping" }, builder))
												.executes(CitizenDebugClientCommand::loadBenchmark)))));
		registerCommand(event.getDispatcher(), command);
	}

	private static int showMetrics(CommandContext<CommandSourceStack> context) {
		CitizenRenderMetrics.Snapshot metrics = CitizenRenderMetrics.snapshot();
		send(context, "Citizen render: cache=" + metrics.cacheCount() + ", detailed=" + metrics.detailedCount()
				+ ", batchFrustum=" + metrics.frustumBatchCount() + ", body=" + metrics.bodyCount()
				+ ", billboard=" + metrics.billboardCount() + ", batchDraws=" + metrics.drawCalls());
		send(context, String.format(Locale.ROOT,
				"Backend hook CPU: latest=%.3f ms, average=%.3f ms, p95=%.3f ms, samples=%d",
				metrics.latestFrameNanos() / 1_000_000.0, metrics.averageFrameNanos() / 1_000_000.0,
				metrics.p95FrameNanos() / 1_000_000.0, metrics.sampleCount()));
		send(context, "Per-frame: lightSamples=" + metrics.lightSamples() + ", instanceDirtyBytes="
				+ metrics.instanceDirtyBytes() + ", peakInstanceDirtyBytes=" + metrics.peakInstanceDirtyBytes()
				+ ", backend=" + CitizenRenderCoordinator.backendName()
				+ ", benchmark=" + CitizenClientBenchmark.status());
		return Command.SINGLE_SUCCESS;
	}

	private static int showBackendStatus(CommandContext<CommandSourceStack> context) {
		send(context, "Citizen render backend: active=" + CitizenRenderCoordinator.backendName()
				+ ", requested=" + CitizenRenderCoordinator.requestedBackendName()
				+ ", compatibilityFallback=" + CitizenRenderCoordinator.isCompatibilityFallbackActive());
		return Command.SINGLE_SUCCESS;
	}

	private static int useCpuBackend(CommandContext<CommandSourceStack> context) {
		CitizenRenderCoordinator.useCpuBackend();
		CitizenRenderMetrics.reset();
		CitizenDebugOverlay.invalidate();
		send(context, "Citizen render backend switched to cpu_batch");
		return Command.SINGLE_SUCCESS;
	}

	private static int useFlywheelPocBackend(CommandContext<CommandSourceStack> context) {
		if (!CitizenRenderCoordinator.useFlywheelPocBackend()) {
			CitizenRenderMetrics.reset();
			CitizenDebugOverlay.invalidate();
			send(context, "Citizen render backend requested flywheel_m3_instancing; active="
					+ CitizenRenderCoordinator.backendName()
					+ " until Flywheel INSTANCING becomes available; renderer reloads retry automatically");
			return Command.SINGLE_SUCCESS;
		}
		CitizenRenderMetrics.reset();
		CitizenDebugOverlay.invalidate();
		send(context, "Citizen render backend switched to " + CitizenRenderCoordinator.backendName()
				+ "; snapshots and rigid-part animation now run on the GPU");
		return Command.SINGLE_SUCCESS;
	}

	private static int resetMetrics(CommandContext<CommandSourceStack> context) {
		CitizenRenderMetrics.reset();
		CitizenDebugOverlay.invalidate();
		send(context, "Citizen render metrics reset");
		return Command.SINGLE_SUCCESS;
	}

	private static int setOverlay(CommandContext<CommandSourceStack> context) {
		boolean enabled = BoolArgumentType.getBool(context, "enabled");
		CitizenDebugOverlay.setEnabled(enabled);
		send(context, "Citizen render overlay " + (enabled ? "enabled" : "disabled"));
		return Command.SINGLE_SUCCESS;
	}

	private static int loadBenchmark(CommandContext<CommandSourceStack> context) {
		int count = IntegerArgumentType.getInteger(context, "count");
		String modeName = StringArgumentType.getString(context, "mode");
		try {
			CitizenClientBenchmark.Mode mode = CitizenClientBenchmark.Mode.parse(modeName);
			int loaded = CitizenClientBenchmark.load(Minecraft.getInstance(), count, mode);
			CitizenRenderMetrics.reset();
			CitizenDebugOverlay.invalidate();
			send(context, "Loaded " + loaded + " deterministic " + modeName.toLowerCase(Locale.ROOT)
					+ " benchmark citizens; synchronized citizens were left untouched");
			return Command.SINGLE_SUCCESS;
		} catch (IllegalArgumentException | IllegalStateException exception) {
			context.getSource().sendFailure(Component.literal(exception.getMessage()));
			return 0;
		}
	}

	private static int clearBenchmark(CommandContext<CommandSourceStack> context) {
		int removed = CitizenClientBenchmark.activeCount();
		CitizenClientBenchmark.clear();
		CitizenRenderMetrics.reset();
		CitizenDebugOverlay.invalidate();
		send(context, "Removed " + removed + " benchmark citizens");
		return Command.SINGLE_SUCCESS;
	}

	private static int showBenchmarkStatus(CommandContext<CommandSourceStack> context) {
		send(context, "Citizen benchmark: " + CitizenClientBenchmark.status());
		return Command.SINGLE_SUCCESS;
	}

	private static void send(CommandContext<CommandSourceStack> context, String message) {
		context.getSource().sendSuccess(() -> Component.literal(message), false);
	}
}
