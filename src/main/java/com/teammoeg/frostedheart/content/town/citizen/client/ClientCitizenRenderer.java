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

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
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

	private static final double MAX_DIST2 = 96.0 * 96.0;
	private static final double LOWPOLY_DIST2 = 64.0 * 64.0;
	private static final double BILLBOARD_HALF_WIDTH = 0.3;
	private static final double BILLBOARD_HEIGHT = 1.8;
	private static final float BOB_AMP = 0.05f;
	private static final double SLEEP_SURFACE_Y = 0.58;
	private static final float SLEEP_SCALE = 0.8f;
	private static final float SLEEP_MODEL_ORIGIN = -1.18f;
	private static final int BUFFER_INITIAL_BYTES = 128 * 1024;
	private static final float TEXTURE_SIZE = 64.0f;
	private static final int LIGHT_SAMPLE_INTERVAL = 5;

	private static final BufferBuilder[] SKIN_BUFFERS = createBuffers();
	private static final boolean[] BUFFER_BEGUN = new boolean[CitizenSkins.count()];
	private static final BlockPos.MutableBlockPos LIGHT_SAMPLE_POS = new BlockPos.MutableBlockPos();
	/** Immediate-mode render-thread state applied to every vertex of the current citizen. */
	private static int currentPackedLight;

	private ClientCitizenRenderer() {
	}

	private static BufferBuilder[] createBuffers() {
		BufferBuilder[] buffers = new BufferBuilder[CitizenSkins.count()];
		for (int i = 0; i < buffers.length; i++)
			buffers[i] = new BufferBuilder(BUFFER_INITIAL_BYTES);
		return buffers;
	}

	/** Render entry called at {@link RenderLevelStageEvent.Stage#AFTER_ENTITIES}. */
	public static void render(RenderLevelStageEvent event) {
		if (ClientCitizenCache.size() == 0)
			return;

		Camera cam = event.getCamera();
		Vec3 cp = cam.getPosition();
		Vector3f left = cam.getLeftVector();
		Vector3f up = cam.getUpVector();
		Vector3f look = cam.getLookVector();
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null)
			return;
		long gameTime = level.getGameTime();
		float time = gameTime + event.getPartialTick();

		RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();

		PoseStack pose = event.getPoseStack();
		pose.pushPose();
		pose.translate(-cp.x, -cp.y, -cp.z);
		Matrix4f mat = pose.last().pose();

		for (ClientCitizen c : ClientCitizenCache.values()) {
			int state = (c.state & 0xFF) % CitizenState.STATE_COUNT;
			boolean sleeping = state == CitizenState.SLEEP;
			if (!sleeping && FakeCitizenManager.has(c.id))
				continue;

			double[] pos = c.renderPos();
			double dx = pos[0] - cp.x;
			double dy = pos[1] - cp.y;
			double dz = pos[2] - cp.z;
			double d2 = dx * dx + dy * dy + dz * dz;
			if (d2 > MAX_DIST2 || dx * look.x + dy * look.y + dz * look.z < -1.5)
				continue;

			AABB cullBox = sleeping
					? new AABB(pos[0] - 1.35, pos[1] + 0.45, pos[2] - 1.35,
							pos[0] + 1.35, pos[1] + 0.95, pos[2] + 1.35)
					: new AABB(pos[0] - 0.5, pos[1], pos[2] - 0.5,
							pos[0] + 0.5, pos[1] + 2.0, pos[2] + 0.5);
			if (!event.getFrustum().isVisible(cullBox))
				continue;

			int skin = CitizenSkins.indexFor(c.id);
			BufferBuilder buf = SKIN_BUFFERS[skin];
			if (!BUFFER_BEGUN[skin]) {
				buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
				BUFFER_BEGUN[skin] = true;
			}
			currentPackedLight = sampleLight(c, pos, sleeping, level, gameTime);

			if (sleeping && d2 <= LOWPOLY_DIST2)
				emitSleepingPlayer(buf, mat, c, pos);
			else if (sleeping)
				emitSleepingBillboard(buf, mat, c, pos);
			else if (d2 <= LOWPOLY_DIST2)
				emitStandingPlayer(buf, mat, c, pos, time);
			else
				emitStandingBillboard(buf, mat, left, up, pos);
		}
		pose.popPose();

		minecraft.gameRenderer.lightTexture().turnOnLightLayer();
		for (int i = 0; i < SKIN_BUFFERS.length; i++) {
			if (!BUFFER_BEGUN[i])
				continue;
			RenderSystem.setShaderTexture(0, CitizenSkins.textureAt(i));
			BufferUploader.drawWithShader(SKIN_BUFFERS[i].end());
			BUFFER_BEGUN[i] = false;
		}
		minecraft.gameRenderer.lightTexture().turnOffLightLayer();

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static int sampleLight(ClientCitizen citizen, double[] pos, boolean sleeping,
			ClientLevel level, long gameTime) {
		int x = Mth.floor(pos[0]);
		int y = Mth.floor(pos[1] + (sleeping ? SLEEP_SURFACE_Y : 1.0));
		int z = Mth.floor(pos[2]);
		if (x != citizen.lightBlockX || y != citizen.lightBlockY || z != citizen.lightBlockZ
				|| gameTime >= citizen.nextLightSampleTick) {
			LIGHT_SAMPLE_POS.set(x, y, z);
			citizen.packedLight = LevelRenderer.getLightColor(level, LIGHT_SAMPLE_POS);
			citizen.lightBlockX = x;
			citizen.lightBlockY = y;
			citizen.lightBlockZ = z;
			citizen.nextLightSampleTick = gameTime + LIGHT_SAMPLE_INTERVAL + (citizen.id & 3);
		}
		return citizen.packedLight;
	}

	private static void emitStandingPlayer(BufferBuilder buf, Matrix4f mat, ClientCitizen c, double[] pos, float time) {
		int yaw = c.visualYaw();
		float forwardX = CitizenState.DIR_X_256[yaw] / 1024.0f;
		float forwardZ = CitizenState.DIR_Z_256[yaw] / 1024.0f;
		float bob = c.isMoving() ? Mth.abs(Mth.sin(time * 0.6f + (c.id & 7) * 1.7f)) * BOB_AMP : 0.0f;
		double y = pos[1] + bob;

		standingPart(buf, mat, pos[0], y, pos[2], 0.0f, 0.75f, 0.5f, 0.75f, 0.25f,
				forwardX, forwardZ, 16, 16, 8, 12, 4); // body
		standingPart(buf, mat, pos[0], y, pos[2], 0.0f, 1.5f, 0.5f, 0.5f, 0.5f,
				forwardX, forwardZ, 0, 0, 8, 8, 8); // head
		standingPart(buf, mat, pos[0], y, pos[2], -0.375f, 0.75f, 0.25f, 0.75f, 0.25f,
				forwardX, forwardZ, 40, 16, 4, 12, 4); // right arm
		standingPart(buf, mat, pos[0], y, pos[2], 0.375f, 0.75f, 0.25f, 0.75f, 0.25f,
				forwardX, forwardZ, 32, 48, 4, 12, 4); // left arm
		standingPart(buf, mat, pos[0], y, pos[2], -0.125f, 0.0f, 0.25f, 0.75f, 0.25f,
				forwardX, forwardZ, 0, 16, 4, 12, 4); // right leg
		standingPart(buf, mat, pos[0], y, pos[2], 0.125f, 0.0f, 0.25f, 0.75f, 0.25f,
				forwardX, forwardZ, 16, 48, 4, 12, 4); // left leg
	}

	private static void standingPart(BufferBuilder buf, Matrix4f mat, double x, double baseY, double z,
			float sideOffset, float partBaseY, float width, float height, float depth,
			float forwardX, float forwardZ, int texU, int texV, int pixelW, int pixelH, int pixelD) {
		float rightX = -forwardZ;
		float rightZ = forwardX;
		addTexturedBox(buf, mat,
				x + rightX * sideOffset, baseY + partBaseY + height * 0.5, z + rightZ * sideOffset,
				width, height, depth,
				rightX, 0, rightZ, 0, 1, 0, -forwardX, 0, -forwardZ,
				texU, texV, pixelW, pixelH, pixelD);
	}

	private static void emitSleepingPlayer(BufferBuilder buf, Matrix4f mat, ClientCitizen c, double[] pos) {
		int yaw = CitizenState.DIR_TO_YAW[c.dir & 15] & 0xFF;
		float forwardX = CitizenState.DIR_X_256[yaw] / 1024.0f;
		float forwardZ = CitizenState.DIR_Z_256[yaw] / 1024.0f;
		sleepingPart(buf, mat, pos, forwardX, forwardZ, 0.0f, 1.125f, 0.5f, 0.75f, 0.25f,
				16, 16, 8, 12, 4);
		sleepingPart(buf, mat, pos, forwardX, forwardZ, 0.0f, 1.75f, 0.5f, 0.5f, 0.5f,
				0, 0, 8, 8, 8);
		sleepingPart(buf, mat, pos, forwardX, forwardZ, -0.375f, 1.125f, 0.25f, 0.75f, 0.25f,
				40, 16, 4, 12, 4);
		sleepingPart(buf, mat, pos, forwardX, forwardZ, 0.375f, 1.125f, 0.25f, 0.75f, 0.25f,
				32, 48, 4, 12, 4);
		sleepingPart(buf, mat, pos, forwardX, forwardZ, -0.125f, 0.375f, 0.25f, 0.75f, 0.25f,
				0, 16, 4, 12, 4);
		sleepingPart(buf, mat, pos, forwardX, forwardZ, 0.125f, 0.375f, 0.25f, 0.75f, 0.25f,
				16, 48, 4, 12, 4);
	}

	private static void sleepingPart(BufferBuilder buf, Matrix4f mat, double[] pos, float forwardX, float forwardZ,
			float sideOffset, float modelY, float width, float height, float depth,
			int texU, int texV, int pixelW, int pixelH, int pixelD) {
		float rightX = -forwardZ;
		float rightZ = forwardX;
		float scaledDepth = depth * SLEEP_SCALE;
		float lengthOffset = SLEEP_MODEL_ORIGIN + modelY * SLEEP_SCALE;
		addTexturedBox(buf, mat,
				pos[0] + rightX * sideOffset * SLEEP_SCALE + forwardX * lengthOffset,
				pos[1] + SLEEP_SURFACE_Y + scaledDepth * 0.5,
				pos[2] + rightZ * sideOffset * SLEEP_SCALE + forwardZ * lengthOffset,
				width * SLEEP_SCALE, height * SLEEP_SCALE, scaledDepth,
				rightX, 0, rightZ, forwardX, 0, forwardZ, 0, -1, 0,
				texU, texV, pixelW, pixelH, pixelD);
	}

	private static void emitStandingBillboard(BufferBuilder buf, Matrix4f mat, Vector3f left, Vector3f up, double[] pos) {
		double lx = left.x * BILLBOARD_HALF_WIDTH;
		double ly = left.y * BILLBOARD_HALF_WIDTH;
		double lz = left.z * BILLBOARD_HALF_WIDTH;
		double ux = up.x * BILLBOARD_HEIGHT;
		double uy = up.y * BILLBOARD_HEIGHT;
		double uz = up.z * BILLBOARD_HEIGHT;
		texturedQuad(buf, mat,
				(float) (pos[0] - lx), (float) (pos[1] - ly), (float) (pos[2] - lz),
				(float) (pos[0] + lx), (float) (pos[1] + ly), (float) (pos[2] + lz),
				(float) (pos[0] + lx + ux), (float) (pos[1] + ly + uy), (float) (pos[2] + lz + uz),
				(float) (pos[0] - lx + ux), (float) (pos[1] - ly + uy), (float) (pos[2] - lz + uz),
				20, 20, 28, 32);
	}

	private static void emitSleepingBillboard(BufferBuilder buf, Matrix4f mat, ClientCitizen c, double[] pos) {
		int yaw = CitizenState.DIR_TO_YAW[c.dir & 15] & 0xFF;
		float fx = CitizenState.DIR_X_256[yaw] / 1024.0f;
		float fz = CitizenState.DIR_Z_256[yaw] / 1024.0f;
		float sx = -fz * 0.27f;
		float sz = fx * 0.27f;
		float frontX = (float) pos[0] + fx * 0.38f;
		float frontZ = (float) pos[2] + fz * 0.38f;
		float backX = (float) pos[0] - fx * 1.18f;
		float backZ = (float) pos[2] - fz * 1.18f;
		float y = (float) (pos[1] + SLEEP_SURFACE_Y + 0.02);
		texturedQuad(buf, mat,
				frontX + sx, y, frontZ + sz, frontX - sx, y, frontZ - sz,
				backX - sx, y, backZ - sz, backX + sx, y, backZ + sz,
				20, 20, 28, 32);
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
				u1, vTop, u2, vSide);
		texturedQuad(buf, mat, v1x, v1y, v1z, v2x, v2y, v2z, v6x, v6y, v6z, v5x, v5y, v5z,
				u2, vSide, u3, vTop);
		texturedQuad(buf, mat, v7x, v7y, v7z, v3x, v3y, v3z, v6x, v6y, v6z, v2x, v2y, v2z,
				u0, vSide, u1, vBottom);
		texturedQuad(buf, mat, v0x, v0y, v0z, v7x, v7y, v7z, v2x, v2y, v2z, v1x, v1y, v1z,
				u1, vSide, u2, vBottom);
		texturedQuad(buf, mat, v4x, v4y, v4z, v0x, v0y, v0z, v1x, v1y, v1z, v5x, v5y, v5z,
				u2, vSide, u4, vBottom);
		texturedQuad(buf, mat, v3x, v3y, v3z, v4x, v4y, v4z, v5x, v5y, v5z, v6x, v6y, v6z,
				u4, vSide, u5, vBottom);
	}

	private static void texturedQuad(BufferBuilder buf, Matrix4f mat,
			float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4,
			float u1, float v1, float u2, float v2) {
		vertex(buf, mat, x1, y1, z1, u2, v1);
		vertex(buf, mat, x2, y2, z2, u1, v1);
		vertex(buf, mat, x3, y3, z3, u1, v2);
		vertex(buf, mat, x4, y4, z4, u2, v2);
	}

	private static void vertex(BufferBuilder buf, Matrix4f mat, float x, float y, float z, float u, float v) {
		buf.vertex(mat, x, y, z).color(255, 255, 255, 255)
				.uv(u / TEXTURE_SIZE, v / TEXTURE_SIZE).uv2(currentPackedLight).endVertex();
	}
}
