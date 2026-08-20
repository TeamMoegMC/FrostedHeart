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

import net.minecraftforge.client.event.RenderLevelStageEvent;

/** Compatibility backend wrapping the existing immediate CPU batch renderer. */
final class CpuBatchCitizenBackend implements CitizenRenderBackend {

	@Override
	public String name() {
		return "cpu_batch";
	}

	@Override
	public void render(RenderLevelStageEvent event) {
		ClientCitizenRenderer.render(event);
	}
}
