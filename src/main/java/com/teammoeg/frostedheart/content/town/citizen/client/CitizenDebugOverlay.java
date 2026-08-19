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

import java.util.Locale;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Toggleable low-allocation HUD for the citizen render counters. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CitizenDebugOverlay {

	public static final IGuiOverlay OVERLAY = CitizenDebugOverlay::render;
	private static final String[] EMPTY_LINES = new String[0];

	private static boolean enabled;
	private static long lastRefreshTick = Long.MIN_VALUE;
	private static String[] lines = EMPTY_LINES;

	private CitizenDebugOverlay() {
	}

	@SubscribeEvent
	public static void register(RegisterGuiOverlaysEvent event) {
		event.registerAboveAll("citizen_render_debug", OVERLAY);
	}

	static void setEnabled(boolean value) {
		enabled = value;
		invalidate();
	}

	static boolean isEnabled() {
		return enabled;
	}

	static void invalidate() {
		lastRefreshTick = Long.MIN_VALUE;
	}

	private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!enabled || minecraft.options.hideGui || minecraft.level == null)
			return;
		long tick = minecraft.level.getGameTime();
		if (lines.length == 0 || tick - lastRefreshTick >= 20 || tick < lastRefreshTick) {
			refreshLines();
			lastRefreshTick = tick;
		}

		Font font = minecraft.font;
		int panelWidth = 0;
		for (String line : lines)
			panelWidth = Math.max(panelWidth, font.width(line));
		int panelHeight = lines.length * 10 + 6;
		graphics.fill(4, 4, panelWidth + 10, panelHeight, 0xA0000000);
		for (int i = 0; i < lines.length; i++)
			graphics.drawString(font, lines[i], 7, 7 + i * 10, 0xFFFFFFFF, true);
	}

	private static void refreshLines() {
		CitizenRenderMetrics.Snapshot metrics = CitizenRenderMetrics.snapshot();
		lines = new String[] {
				"Citizen render | active " + CitizenRenderCoordinator.backendName()
						+ " | requested " + CitizenRenderCoordinator.requestedBackendName()
						+ " | benchmark " + CitizenClientBenchmark.status(),
				"cache " + metrics.cacheCount() + " | detailed " + metrics.detailedCount()
						+ " | batch-frustum " + metrics.frustumBatchCount(),
				"body " + metrics.bodyCount() + " | billboard " + metrics.billboardCount()
						+ " | batch draws " + metrics.drawCalls(),
				"backend hook CPU " + millis(metrics.latestFrameNanos()) + " ms | p95 "
						+ millis(metrics.p95FrameNanos()) + " ms (" + metrics.sampleCount() + ")",
				"light samples " + metrics.lightSamples() + " | dirty latest/peak "
						+ metrics.instanceDirtyBytes() + "/" + metrics.peakInstanceDirtyBytes() + " B"
		};
	}

	private static String millis(long nanos) {
		return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0);
	}
}
