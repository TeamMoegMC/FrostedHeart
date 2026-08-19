/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Shared, allocation-free layout data for the citizen batch renderer. */
final class CitizenBatchRenderLayout {

	private static final int YAW_STEPS = 256;
	private static final Axes[] STANDING_AXES = new Axes[YAW_STEPS];
	private static final Axes[] SLEEPING_AXES = new Axes[YAW_STEPS];

	static {
		for (int yaw = 0; yaw < YAW_STEPS; yaw++) {
			float forwardX = CitizenState.DIR_X_256[yaw] / (float) CitizenState.FIXED_SCALE;
			float forwardZ = CitizenState.DIR_Z_256[yaw] / (float) CitizenState.FIXED_SCALE;
			float inverseLength = 1.0f / (float) Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
			forwardX *= inverseLength;
			forwardZ *= inverseLength;
			float modelXx = forwardZ;
			float modelXz = -forwardX;

			// Vanilla LivingEntityRenderer applies scale(-1, -1, 1). ModelPart's
			// local -Z is the skin front and local -Y points toward the head/top.
			STANDING_AXES[yaw] = new Axes(
					modelXx, 0.0f, modelXz,
					0.0f, -1.0f, 0.0f,
					-forwardX, 0.0f, -forwardZ);
			SLEEPING_AXES[yaw] = new Axes(
					modelXx, 0.0f, modelXz,
					-forwardX, 0.0f, -forwardZ,
					0.0f, -1.0f, 0.0f);
		}
	}

	private CitizenBatchRenderLayout() {
	}

	static RenderType skinRenderType(ResourceLocation texture) {
		return RenderType.entityCutoutNoCull(texture, false);
	}

	static Axes standingAxes(int yaw) {
		return STANDING_AXES[yaw & 0xFF];
	}

	static Axes sleepingAxes(int yaw) {
		return SLEEPING_AXES[yaw & 0xFF];
	}

	record Axes(
			float xX, float xY, float xZ,
			float yX, float yY, float yZ,
			float zX, float zY, float zZ) {
	}
}
