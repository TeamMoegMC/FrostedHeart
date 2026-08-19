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
 *
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Client citizen batch renderer. Awake near-range citizens are handled by
 * {@link FakeCitizenManager}; all remaining citizens use textured Steve-proportion
 * LOD geometry backed by Minecraft's built-in player skins.
 * <p>
 * Visibility is evaluated once per frame. Visible vertices are written directly
 * into seven reusable skin buffers, so skin variety costs at most seven draw calls
 * without seven full cache scans or per-frame citizen collections.
 */
public final class ClientCitizenRenderer {

	private static final int BUFFER_INITIAL_BYTES = 128 * 1024;
	private static final float TEXTURE_SIZE = 64.0f;
	private static final int LIGHT_SAMPLE_INTERVAL = 5;

	private static final RenderType[] SKIN_RENDER_TYPES = createRenderTypes();
	private static final BufferBuilder[] SKIN_BUFFERS = createBuffers();
	private static final boolean[] BUFFER_BEGUN = new boolean[CitizenSkins.count()];
	private static final BlockPos.MutableBlockPos LIGHT_SAMPLE_POS = new BlockPos.MutableBlockPos();
	private static final CitizenBatchRenderLayout.MotionSample BODY_MOTION =
			new CitizenBatchRenderLayout.MotionSample();
	/** Immediate-mode render-thread state applied while filling the current skin buffers. */
	private static int currentPackedLight;
	private static Matrix3f currentNormalMatrix;
	private static int frameLightSamples;

	private ClientCitizenRenderer() {
	}

	private static RenderType[] createRenderTypes() {
		RenderType[] renderTypes = new RenderType[CitizenSkins.count()];
		for (int i = 0; i < renderTypes.length; i++)
			renderTypes[i] = CitizenBatchRenderLayout.skinRenderType(CitizenSkins.textureAt(i));
		return renderTypes;
	}

	private static BufferBuilder[] createBuffers() {
		BufferBuilder[] buffers = new BufferBuilder[CitizenSkins.count()];
		for (int i = 0; i < buffers.length; i++)
			buffers[i] = new BufferBuilder(BUFFER_INITIAL_BYTES);
		return buffers;
	}

	/** Render entry called at {@link RenderLevelStageEvent.Stage#AFTER_ENTITIES}. */
	public static void render(RenderLevelStageEvent event) {
		long frameStart = System.nanoTime();
		int cacheCount = ClientCitizenCache.size();
		frameLightSamples = 0;
		if (cacheCount == 0) {
			recordMetrics(frameStart, 0, 0, 0, 0);
			return;
		}

		Camera cam = event.getCamera();
		Vec3 cp = cam.getPosition();
		Vector3f left = cam.getLeftVector();
		Vector3f up = cam.getUpVector();
		Vector3f look = cam.getLookVector();
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			recordMetrics(frameStart, 0, 0, 0, 0);
			return;
		}
		long gameTime = level.getGameTime();
		double timeSeconds = (gameTime + event.getPartialTick()) / 20.0;
		int frustumBatchCount = 0;
		int bodyCount = 0;
		int billboardCount = 0;
		int drawCalls = 0;

		PoseStack pose = event.getPoseStack();
		pose.pushPose();
		pose.translate(-cp.x, -cp.y, -cp.z);
		Matrix4f mat = pose.last().pose();
		currentNormalMatrix = pose.last().normal();

		for (ClientCitizen c : ClientCitizenCache.values()) {
			int state = (c.state & 0xFF) % CitizenState.STATE_COUNT;
			boolean sleeping = state == CitizenState.SLEEP;
			if (CitizenRenderCoordinator.hasDetailedOwnership(c))
				continue;

			double[] pos = c.renderPos();
			double dx = pos[0] - cp.x;
			double dy = pos[1] - cp.y;
			double dz = pos[2] - cp.z;
			double d2 = dx * dx + dy * dy + dz * dz;
			CitizenRenderOwner owner = CitizenRenderCoordinator.batchOwnerFor(c, d2);
			if (owner == CitizenRenderOwner.NONE || owner == CitizenRenderOwner.DETAILED_ENTITY)
				continue;

			if (dx * look.x + dy * look.y + dz * look.z < -1.5)
				continue;

			if (!event.getFrustum().isVisible(c.cullingBox()))
				continue;
			frustumBatchCount++;

			int skin = CitizenSkins.indexFor(c.id);
			BufferBuilder buf = SKIN_BUFFERS[skin];
			if (!BUFFER_BEGUN[skin]) {
				RenderType renderType = SKIN_RENDER_TYPES[skin];
				buf.begin(renderType.mode(), renderType.format());
				BUFFER_BEGUN[skin] = true;
			}
			currentPackedLight = sampleLight(c, pos, sleeping, level, gameTime);

			if (owner == CitizenRenderOwner.BODY_BATCH && sleeping) {
				bodyCount++;
				emitSleepingPlayer(buf, mat, c, pos);
			} else if (owner == CitizenRenderOwner.BILLBOARD_BATCH && sleeping) {
				billboardCount++;
				emitSleepingBillboard(buf, mat, c, pos);
			} else if (owner == CitizenRenderOwner.BODY_BATCH) {
				bodyCount++;
				emitStandingPlayer(buf, mat, c, pos, timeSeconds);
			} else {
				billboardCount++;
				emitStandingBillboard(buf, mat, left, up, look, pos);
			}
		}
		pose.popPose();

		for (int i = 0; i < SKIN_BUFFERS.length; i++) {
			if (!BUFFER_BEGUN[i])
				continue;
			SKIN_RENDER_TYPES[i].end(SKIN_BUFFERS[i], VertexSorting.DISTANCE_TO_ORIGIN);
			BUFFER_BEGUN[i] = false;
			drawCalls++;
		}
		recordMetrics(frameStart, frustumBatchCount, bodyCount, billboardCount, drawCalls);
	}

	private static void recordMetrics(long frameStart, int frustumBatchCount, int bodyCount,
			int billboardCount, int drawCalls) {
		CitizenRenderMetrics.recordFrame(System.nanoTime() - frameStart, ClientCitizenCache.size(),
				FakeCitizenManager.activeCount(), frustumBatchCount, bodyCount, billboardCount,
				drawCalls, frameLightSamples, 0L);
	}

	private static int sampleLight(ClientCitizen citizen, double[] pos, boolean sleeping,
			ClientLevel level, long gameTime) {
		int x = Mth.floor(pos[0]);
		int y = Mth.floor(pos[1] + (sleeping ? CitizenBatchRenderLayout.SLEEP_SURFACE_Y : 1.0));
		int z = Mth.floor(pos[2]);
		if (x != citizen.lightBlockX || y != citizen.lightBlockY || z != citizen.lightBlockZ
				|| gameTime >= citizen.nextLightSampleTick) {
			LIGHT_SAMPLE_POS.set(x, y, z);
			citizen.packedLight = LevelRenderer.getLightColor(level, LIGHT_SAMPLE_POS);
			citizen.lightBlockX = x;
			citizen.lightBlockY = y;
			citizen.lightBlockZ = z;
			citizen.nextLightSampleTick = gameTime + LIGHT_SAMPLE_INTERVAL + (citizen.id & 3);
			frameLightSamples++;
		}
		return citizen.packedLight;
	}

	private static void emitStandingPlayer(BufferBuilder buf, Matrix4f mat, ClientCitizen c,
			double[] pos, double timeSeconds) {
		int yaw = c.visualYaw();
		CitizenBatchRenderLayout.Axes axes = CitizenBatchRenderLayout.standingAxes(yaw);
		CitizenBatchRenderLayout.sampleBodyMotion(c, timeSeconds, BODY_MOTION);
		for (int index = 0; index < CitizenBatchRenderLayout.bodyPartCount(); index++) {
			CitizenBatchRenderLayout.BodyPart part = CitizenBatchRenderLayout.bodyPartAt(index);
			standingPart(buf, mat, pos, axes, part,
					CitizenBatchRenderLayout.limbAngle(part, BODY_MOTION));
		}
	}

	private static void standingPart(BufferBuilder buf, Matrix4f mat, double[] pos,
			CitizenBatchRenderLayout.Axes axes, CitizenBatchRenderLayout.BodyPart part, float angle) {
		float sin = Mth.sin(angle);
		float cos = Mth.cos(angle);
		float relativeY = part.centerY() - part.pivotY();
		float modelY = -part.pivotY() - cos * relativeY;
		float modelZ = -sin * relativeY;
		float upX = axes.yX() * cos + axes.zX() * sin;
		float upY = axes.yY() * cos + axes.zY() * sin;
		float upZ = axes.yZ() * cos + axes.zZ() * sin;
		float backX = -axes.yX() * sin + axes.zX() * cos;
		float backY = -axes.yY() * sin + axes.zY() * cos;
		float backZ = -axes.yZ() * sin + axes.zZ() * cos;
		addTexturedBox(buf, mat,
				pos[0] + axes.xX() * part.sideOffset() + axes.yX() * modelY + axes.zX() * modelZ,
				pos[1] + axes.xY() * part.sideOffset() + axes.yY() * modelY + axes.zY() * modelZ,
				pos[2] + axes.xZ() * part.sideOffset() + axes.yZ() * modelY + axes.zZ() * modelZ,
				part.width(), part.height(), part.depth(),
				axes.xX(), axes.xY(), axes.xZ(),
				upX, upY, upZ, backX, backY, backZ,
				part.textureU(), part.textureV(), part.widthPixels(), part.heightPixels(), part.depthPixels());
	}

	private static void emitSleepingPlayer(BufferBuilder buf, Matrix4f mat, ClientCitizen c, double[] pos) {
		int yaw = CitizenState.DIR_TO_YAW[c.dir & 15] & 0xFF;
		CitizenBatchRenderLayout.Axes axes = CitizenBatchRenderLayout.sleepingAxes(yaw);
		for (int index = 0; index < CitizenBatchRenderLayout.bodyPartCount(); index++)
			sleepingPart(buf, mat, pos, axes, CitizenBatchRenderLayout.bodyPartAt(index));
	}

	private static void sleepingPart(BufferBuilder buf, Matrix4f mat, double[] pos,
			CitizenBatchRenderLayout.Axes axes, CitizenBatchRenderLayout.BodyPart part) {
		float forwardX = -axes.yX();
		float forwardZ = -axes.yZ();
		float scaledDepth = part.depth() * CitizenBatchRenderLayout.SLEEP_SCALE;
		float lengthOffset = CitizenBatchRenderLayout.SLEEP_MODEL_ORIGIN
				+ part.centerY() * CitizenBatchRenderLayout.SLEEP_SCALE;
		addTexturedBox(buf, mat,
				pos[0] + axes.xX() * part.sideOffset() * CitizenBatchRenderLayout.SLEEP_SCALE
						+ forwardX * lengthOffset,
				pos[1] + CitizenBatchRenderLayout.SLEEP_SURFACE_Y + scaledDepth * 0.5,
				pos[2] + axes.xZ() * part.sideOffset() * CitizenBatchRenderLayout.SLEEP_SCALE
						+ forwardZ * lengthOffset,
				part.width() * CitizenBatchRenderLayout.SLEEP_SCALE,
				part.height() * CitizenBatchRenderLayout.SLEEP_SCALE, scaledDepth,
				axes.xX(), axes.xY(), axes.xZ(),
				axes.yX(), axes.yY(), axes.yZ(),
				axes.zX(), axes.zY(), axes.zZ(),
				part.textureU(), part.textureV(), part.widthPixels(), part.heightPixels(), part.depthPixels());
	}

	private static void emitStandingBillboard(BufferBuilder buf, Matrix4f mat, Vector3f left, Vector3f up,
			Vector3f look, double[] pos) {
		for (int quadIndex = 0; quadIndex < CitizenBatchRenderLayout.billboardQuadCount(); quadIndex++)
			emitStandingBillboardQuad(buf, mat, left, up, look, pos,
					CitizenBatchRenderLayout.billboardQuadAt(quadIndex));
	}

	private static void emitStandingBillboardQuad(BufferBuilder buf, Matrix4f mat, Vector3f left, Vector3f up,
			Vector3f look, double[] pos, CitizenBatchRenderLayout.BillboardQuad quad) {
		float halfWidth = CitizenBatchRenderLayout.standingBillboardHalfWidth(quad);
		float bottom = CitizenBatchRenderLayout.standingBillboardY(quad.minY());
		float top = CitizenBatchRenderLayout.standingBillboardY(quad.maxY());
		double lx = left.x * halfWidth;
		double ly = left.y * halfWidth;
		double lz = left.z * halfWidth;
		double bottomX = up.x * bottom;
		double bottomY = up.y * bottom;
		double bottomZ = up.z * bottom;
		double topX = up.x * top;
		double topY = up.y * top;
		double topZ = up.z * top;
		standingBillboardQuad(buf, mat,
				(float) (pos[0] - lx + bottomX), (float) (pos[1] - ly + bottomY),
				(float) (pos[2] - lz + bottomZ),
				(float) (pos[0] + lx + bottomX), (float) (pos[1] + ly + bottomY),
				(float) (pos[2] + lz + bottomZ),
				(float) (pos[0] + lx + topX), (float) (pos[1] + ly + topY),
				(float) (pos[2] + lz + topZ),
				(float) (pos[0] - lx + topX), (float) (pos[1] - ly + topY),
				(float) (pos[2] - lz + topZ),
				-look.x, -look.y, -look.z,
				quad.minU(), quad.minV(), quad.maxU(), quad.maxV());
	}

	private static void emitSleepingBillboard(BufferBuilder buf, Matrix4f mat, ClientCitizen c, double[] pos) {
		int yaw = CitizenState.DIR_TO_YAW[c.dir & 15] & 0xFF;
		float fx = CitizenState.DIR_X_256[yaw] / 1024.0f;
		float fz = CitizenState.DIR_Z_256[yaw] / 1024.0f;
		for (int quadIndex = 0; quadIndex < CitizenBatchRenderLayout.billboardQuadCount(); quadIndex++)
			emitSleepingBillboardQuad(buf, mat, pos, fx, fz,
					CitizenBatchRenderLayout.billboardQuadAt(quadIndex));
	}

	private static void emitSleepingBillboardQuad(BufferBuilder buf, Matrix4f mat, double[] pos,
			float fx, float fz, CitizenBatchRenderLayout.BillboardQuad quad) {
		float halfWidth = CitizenBatchRenderLayout.sleepingBillboardHalfWidth(quad);
		float sx = -fz * halfWidth;
		float sz = fx * halfWidth;
		float frontLength = CitizenBatchRenderLayout.sleepingBillboardLength(quad.minY());
		float backLength = CitizenBatchRenderLayout.sleepingBillboardLength(quad.maxY());
		float frontX = (float) pos[0] + fx * frontLength;
		float frontZ = (float) pos[2] + fz * frontLength;
		float backX = (float) pos[0] + fx * backLength;
		float backZ = (float) pos[2] + fz * backLength;
		float y = (float) pos[1] + CitizenBatchRenderLayout.SLEEP_BILLBOARD_Y;
		texturedQuad(buf, mat,
				frontX + sx, y, frontZ + sz, frontX - sx, y, frontZ - sz,
				backX - sx, y, backZ - sz, backX + sx, y, backZ + sz,
				0, 1, 0,
				quad.minU(), quad.minV(), quad.maxU(), quad.maxV());
	}

	/** Emits a box using the same six-face UV unfolding as vanilla ModelPart.Cube. */
	private static void addTexturedBox(BufferBuilder buf, Matrix4f mat,
			double centerX, double centerY, double centerZ, float width, float height, float depth,
			float rightX, float rightY, float rightZ,
			float upX, float upY, float upZ,
			float backX, float backY, float backZ,
			int texU, int texV, int pixelW, int pixelH, int pixelD) {
		float hxX = rightX * width * 0.5f, hxY = rightY * width * 0.5f, hxZ = rightZ * width * 0.5f;
		float hyX = upX * height * 0.5f, hyY = upY * height * 0.5f, hyZ = upZ * height * 0.5f;
		float hzX = backX * depth * 0.5f, hzY = backY * depth * 0.5f, hzZ = backZ * depth * 0.5f;
		float cx = (float) centerX, cy = (float) centerY, cz = (float) centerZ;

		float v7x = cx - hxX - hyX - hzX, v7y = cy - hxY - hyY - hzY, v7z = cz - hxZ - hyZ - hzZ;
		float v0x = cx + hxX - hyX - hzX, v0y = cy + hxY - hyY - hzY, v0z = cz + hxZ - hyZ - hzZ;
		float v1x = cx + hxX + hyX - hzX, v1y = cy + hxY + hyY - hzY, v1z = cz + hxZ + hyZ - hzZ;
		float v2x = cx - hxX + hyX - hzX, v2y = cy - hxY + hyY - hzY, v2z = cz - hxZ + hyZ - hzZ;
		float v3x = cx - hxX - hyX + hzX, v3y = cy - hxY - hyY + hzY, v3z = cz - hxZ - hyZ + hzZ;
		float v4x = cx + hxX - hyX + hzX, v4y = cy + hxY - hyY + hzY, v4z = cz + hxZ - hyZ + hzZ;
		float v5x = cx + hxX + hyX + hzX, v5y = cy + hxY + hyY + hzY, v5z = cz + hxZ + hyZ + hzZ;
		float v6x = cx - hxX + hyX + hzX, v6y = cy - hxY + hyY + hzY, v6z = cz - hxZ + hyZ + hzZ;

		float u0 = texU;
		float u1 = texU + pixelD;
		float u2 = u1 + pixelW;
		// Vanilla f7 (top end) and f8 (east end) branch independently from f6.
		// They are equal only for cubes; chaining f8 after f7 shifts side/back UVs.
		float u3 = u2 + pixelW;
		float u4 = u2 + pixelD;
		float u5 = u4 + pixelW;
		float vTop = texV;
		float vSide = texV + pixelD;
		float vBottom = vSide + pixelH;

		texturedQuad(buf, mat, v4x, v4y, v4z, v3x, v3y, v3z, v7x, v7y, v7z, v0x, v0y, v0z,
				-upX, -upY, -upZ,
				u1, vTop, u2, vSide);
		texturedQuad(buf, mat, v1x, v1y, v1z, v2x, v2y, v2z, v6x, v6y, v6z, v5x, v5y, v5z,
				upX, upY, upZ,
				u2, vSide, u3, vTop);
		texturedQuad(buf, mat, v7x, v7y, v7z, v3x, v3y, v3z, v6x, v6y, v6z, v2x, v2y, v2z,
				-rightX, -rightY, -rightZ,
				u0, vSide, u1, vBottom);
		texturedQuad(buf, mat, v0x, v0y, v0z, v7x, v7y, v7z, v2x, v2y, v2z, v1x, v1y, v1z,
				-backX, -backY, -backZ,
				u1, vSide, u2, vBottom);
		texturedQuad(buf, mat, v4x, v4y, v4z, v0x, v0y, v0z, v1x, v1y, v1z, v5x, v5y, v5z,
				rightX, rightY, rightZ,
				u2, vSide, u4, vBottom);
		texturedQuad(buf, mat, v3x, v3y, v3z, v4x, v4y, v4z, v5x, v5y, v5z, v6x, v6y, v6z,
				backX, backY, backZ,
				u4, vSide, u5, vBottom);
	}

	private static void texturedQuad(BufferBuilder buf, Matrix4f mat,
			float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4,
			float nx, float ny, float nz,
			float u1, float v1, float u2, float v2) {
		float transformedX = currentNormalMatrix.m00() * nx + currentNormalMatrix.m10() * ny + currentNormalMatrix.m20() * nz;
		float transformedY = currentNormalMatrix.m01() * nx + currentNormalMatrix.m11() * ny + currentNormalMatrix.m21() * nz;
		float transformedZ = currentNormalMatrix.m02() * nx + currentNormalMatrix.m12() * ny + currentNormalMatrix.m22() * nz;
		vertex(buf, mat, x1, y1, z1, u2, v1, transformedX, transformedY, transformedZ);
		vertex(buf, mat, x2, y2, z2, u1, v1, transformedX, transformedY, transformedZ);
		vertex(buf, mat, x3, y3, z3, u1, v2, transformedX, transformedY, transformedZ);
		vertex(buf, mat, x4, y4, z4, u2, v2, transformedX, transformedY, transformedZ);
	}

	private static void standingBillboardQuad(BufferBuilder buf, Matrix4f mat,
			float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4,
			float nx, float ny, float nz,
			float u1, float v1, float u2, float v2) {
		float transformedX = currentNormalMatrix.m00() * nx + currentNormalMatrix.m10() * ny + currentNormalMatrix.m20() * nz;
		float transformedY = currentNormalMatrix.m01() * nx + currentNormalMatrix.m11() * ny + currentNormalMatrix.m21() * nz;
		float transformedZ = currentNormalMatrix.m02() * nx + currentNormalMatrix.m12() * ny + currentNormalMatrix.m22() * nz;
		vertex(buf, mat, x1, y1, z1, u2, v2, transformedX, transformedY, transformedZ);
		vertex(buf, mat, x2, y2, z2, u1, v2, transformedX, transformedY, transformedZ);
		vertex(buf, mat, x3, y3, z3, u1, v1, transformedX, transformedY, transformedZ);
		vertex(buf, mat, x4, y4, z4, u2, v1, transformedX, transformedY, transformedZ);
	}

	private static void vertex(BufferBuilder buf, Matrix4f mat, float x, float y, float z, float u, float v,
			float nx, float ny, float nz) {
		buf.vertex(mat, x, y, z).color(255, 255, 255, 255)
				.uv(u / TEXTURE_SIZE, v / TEXTURE_SIZE).overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(currentPackedLight).normal(nx, ny, nz).endVertex();
	}
}
