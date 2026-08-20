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

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Lifecycle contract for an exclusive citizen crowd render backend. */
interface CitizenRenderBackend extends AutoCloseable {

	String name();

	default boolean initialize() {
		return true;
	}

	default void onCitizenAdded(ClientCitizen citizen) {
	}

	default void onCitizenUpdated(ClientCitizen citizen) {
	}

	default void onCitizenRemoved(int citizenId) {
	}

	default void tick(Minecraft minecraft) {
	}

	void render(RenderLevelStageEvent event);

	default void clear() {
	}

	default void onResourceReload() {
	}

	default boolean onClientLevelChanged(ClientLevel level) {
		clear();
		return true;
	}

	default boolean onRenderersReloaded(ClientLevel level) {
		return true;
	}

	default boolean isHealthy() {
		return true;
	}

	@Override
	default void close() {
	}
}
