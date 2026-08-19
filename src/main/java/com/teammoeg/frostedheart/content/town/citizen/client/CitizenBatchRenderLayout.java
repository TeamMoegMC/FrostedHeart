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
import net.minecraft.util.Mth;

/** Shared, allocation-free layout data for the citizen batch renderer. */
final class CitizenBatchRenderLayout {

	static final float SLEEP_SCALE = 0.8f;
	static final float SLEEP_MODEL_ORIGIN = -1.18f;
	static final float SLEEP_SURFACE_Y = 0.58f;
	static final float SLEEP_BILLBOARD_Y = 0.60f;
	static final float WALK_PHASE_PER_BLOCK = 0.6662f;
	static final float WALK_PHASE_PERIOD = (float) (Math.PI * 2.0);

	private static final float MODEL_PIXELS_PER_BLOCK = 16.0f;
	private static final float TEXTURE_SIZE = 64.0f;
	private static final int VERTICES_PER_QUAD = 4;
	private static final int YAW_STEPS = 256;
	private static final Axes[] STANDING_AXES = new Axes[YAW_STEPS];
	private static final Axes[] SLEEPING_AXES = new Axes[YAW_STEPS];
	private static final BodyPart[] BODY_PARTS = {
			new BodyPart(0, 0.0f, 0.75f, 16, 16, 8, 12, 4, 0.0f, 0.0f, 0.0f),
			new BodyPart(1, 0.0f, 1.50f, 0, 0, 8, 8, 8, 0.0f, 0.0f, 0.0f),
			new BodyPart(2, -0.375f, 0.75f, 40, 16, 4, 12, 4, 1.50f, 1.0f, 1.0f),
			new BodyPart(3, 0.375f, 0.75f, 32, 48, 4, 12, 4, 1.50f, 1.0f, -1.0f),
			new BodyPart(4, -0.125f, 0.0f, 0, 16, 4, 12, 4, 0.75f, 1.4f, -1.0f),
			new BodyPart(5, 0.125f, 0.0f, 16, 48, 4, 12, 4, 0.75f, 1.4f, 1.0f)
	};
	private static final BillboardQuad[] BILLBOARD_QUADS = {
			new BillboardQuad(1.0f, 0.0f, 0.75f, 20, 20, 28, 32),
			new BillboardQuad(0.75f, 0.75f, 1.0f, 8, 8, 16, 16)
	};

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

	static int bodyPartCount() {
		return BODY_PARTS.length;
	}

	static BodyPart bodyPartAt(int index) {
		return BODY_PARTS[index];
	}

	static int billboardQuadCount() {
		return BILLBOARD_QUADS.length;
	}

	static int billboardVertexCount() {
		return BILLBOARD_QUADS.length * VERTICES_PER_QUAD;
	}

	static BillboardQuad billboardQuadAt(int index) {
		return BILLBOARD_QUADS[index];
	}

	static int billboardQuadIndex(int vertexIndex) {
		return vertexIndex / VERTICES_PER_QUAD;
	}

	static float billboardModelX(int vertexIndex) {
		BillboardQuad quad = billboardQuadAt(billboardQuadIndex(vertexIndex));
		int corner = vertexIndex % VERTICES_PER_QUAD;
		return corner == 0 || corner == 3 ? -quad.halfWidth : quad.halfWidth;
	}

	static float billboardModelY(int vertexIndex) {
		BillboardQuad quad = billboardQuadAt(billboardQuadIndex(vertexIndex));
		return vertexIndex % VERTICES_PER_QUAD < 2 ? quad.minY : quad.maxY;
	}

	static float billboardModelU(int vertexIndex) {
		BillboardQuad quad = billboardQuadAt(billboardQuadIndex(vertexIndex));
		int corner = vertexIndex % VERTICES_PER_QUAD;
		return (corner == 0 || corner == 3 ? quad.maxU : quad.minU) / TEXTURE_SIZE;
	}

	static float billboardModelV(int vertexIndex) {
		BillboardQuad quad = billboardQuadAt(billboardQuadIndex(vertexIndex));
		return (vertexIndex % VERTICES_PER_QUAD < 2 ? quad.maxV : quad.minV) / TEXTURE_SIZE;
	}

	static float standingBillboardHalfWidth(BillboardQuad quad) {
		return quad.halfWidth * 0.30f;
	}

	static float standingBillboardY(float modelY) {
		return modelY * 1.80f;
	}

	static float sleepingBillboardHalfWidth(BillboardQuad quad) {
		return quad.halfWidth * 0.27f;
	}

	static float sleepingBillboardLength(float modelY) {
		return 0.38f + (SLEEP_MODEL_ORIGIN - 0.38f) * modelY;
	}

	static float initialWalkPhase(int citizenId) {
		return wrapWalkPhase((citizenId & 7) * 1.7f);
	}

	static float advanceWalkPhase(float phase, double x0, double z0, double x1, double z1) {
		double dx = x1 - x0;
		double dz = z1 - z0;
		return wrapWalkPhase(phase + (float) Math.sqrt(dx * dx + dz * dz) * WALK_PHASE_PER_BLOCK);
	}

	static float wrapWalkPhase(float phase) {
		float wrapped = phase % WALK_PHASE_PERIOD;
		return wrapped < 0.0f ? wrapped + WALK_PHASE_PERIOD : wrapped;
	}

	static void sampleBodyMotion(ClientCitizen citizen, double nowSeconds, MotionSample sample) {
		double interval = Mth.clamp(citizen.snapshotEndSeconds() - citizen.snapshotStartSeconds(), 0.05, 1.0);
		float durationTicks = (float) (interval * 20.0);
		float elapsedTicks = (float) Math.max(0.0, (nowSeconds - citizen.snapshotStartSeconds()) * 20.0);
		float blend = Mth.clamp(elapsedTicks / durationTicks, 0.0f, 1.0f);
		float dx = (float) (citizen.x1 - citizen.x0);
		float dz = (float) (citizen.z1 - citizen.z0);
		float snapshotDistance = Mth.sqrt(dx * dx + dz * dz);
		float extrapolationTicks = Mth.clamp(elapsedTicks - durationTicks, 0.0f, 30.0f);
		float extrapolationSpeed = 0.0f;
		if (citizen.isMoving())
			extrapolationSpeed = CitizenState.SPEED[citizen.state & 0xFF] / (float) CitizenState.FIXED_SCALE;
		float speed = extrapolationTicks > 0.0f ? extrapolationSpeed : snapshotDistance / durationTicks;
		float phase = citizen.walkPhase()
				+ blend * snapshotDistance * WALK_PHASE_PER_BLOCK
				+ extrapolationTicks * extrapolationSpeed * WALK_PHASE_PER_BLOCK;
		sample.swing = Mth.sin(wrapWalkPhase(phase)) * speed;
	}

	static float limbAngle(BodyPart part, MotionSample sample) {
		return sample.swing * part.swingScale * part.swingSign;
	}

	record Axes(
			float xX, float xY, float xZ,
			float yX, float yY, float yZ,
			float zX, float zY, float zZ) {
	}

	record BodyPart(int id, float sideOffset, float minY,
			int textureU, int textureV, int widthPixels, int heightPixels, int depthPixels,
			float pivotY, float swingScale, float swingSign) {

		float width() {
			return widthPixels / MODEL_PIXELS_PER_BLOCK;
		}

		float height() {
			return heightPixels / MODEL_PIXELS_PER_BLOCK;
		}

		float depth() {
			return depthPixels / MODEL_PIXELS_PER_BLOCK;
		}

		float centerY() {
			return minY + height() * 0.5f;
		}

		float modelStartX() {
			return (sideOffset - width() * 0.5f) * MODEL_PIXELS_PER_BLOCK;
		}

		float modelStartY() {
			return -(minY + height()) * MODEL_PIXELS_PER_BLOCK;
		}

		float modelStartZ() {
			return -depthPixels * 0.5f;
		}
	}

	record BillboardQuad(float halfWidth, float minY, float maxY,
			int minU, int minV, int maxU, int maxV) {
	}

	static final class MotionSample {
		private float swing;
	}
}
