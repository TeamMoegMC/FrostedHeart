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
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * 客户端居民批量渲染器（v2：三级 LOD，单次 draw call 提交）。
 * 近距（&lt; 24 格）由 FakeCitizenManager 的假实体接管，本类直接跳过；
 * 中距（24–64 格）绘制低模人形（躯干+头两个盒体，16 向朝向旋转、行走起伏）；
 * 远距（64–96 格）绘制纯色广告牌。中远距全部合并进一个 POSITION_COLOR 批次，
 * 成本 = 每中距居民 48 顶点 / 每远距居民 4 顶点 + 1 次 draw call。
 * <p>
 * Client citizen batch renderer (v2: three-tier LOD, single draw call).
 * Near range (&lt; 24 blocks) is owned by FakeCitizenManager's fake entities
 * and skipped here; mid range (24–64) draws low-poly humanoids (body+head
 * boxes, 16-way yaw rotation, walk bobbing); far range (64–96) draws plain
 * billboards. Mid and far geometry is merged into one POSITION_COLOR batch:
 * 48 vertices per mid-range / 4 per far-range citizen + 1 draw call.
 */
public final class ClientCitizenRenderer {

	/** 最大渲染距离（与 AOI 半径一致） / Max render distance (matches AOI radius) */
	private static final double MAX_DIST2 = 96.0 * 96.0;
	/** 低模人形的最大距离平方；更近由假实体接管 / Low-poly humanoid max distance squared; nearer is owned by fake entities */
	private static final double LOWPOLY_DIST2 = 64.0 * 64.0;
	/** 各状态配色（RGB） / Per-state colors (RGB) */
	private static final float[][] COLORS = {
			{ 0.85f, 0.75f, 0.55f }, // IDLE 米色
			{ 0.55f, 0.80f, 0.55f }, // WANDER 浅绿
			{ 0.95f, 0.60f, 0.35f }, // RETURN_HOME 橙
			{ 0.45f, 0.55f, 0.90f }, // SLEEP 蓝
			{ 0.90f, 0.85f, 0.40f }, // WORK 黄
	};
	/** 头部肤色 / Head skin tone */
	private static final float[] HEAD_COLOR = { 0.85f, 0.72f, 0.60f };
	private static final float ALPHA = 0.92f;
	/** 广告牌尺寸 / Billboard dimensions */
	private static final double BILLBOARD_HALF_WIDTH = 0.3;
	private static final double BILLBOARD_HEIGHT = 1.8;
	/** 低模躯干尺寸（宽/高/厚） / Low-poly body dimensions (width/height/depth) */
	private static final float BODY_W = 0.55f;
	private static final float BODY_H = 1.05f;
	private static final float BODY_D = 0.35f;
	/** 低模头部边长 / Low-poly head size */
	private static final float HEAD_SIZE = 0.45f;
	/** 行走起伏幅度（方块） / Walk bob amplitude (blocks) */
	private static final float BOB_AMP = 0.05f;

	private ClientCitizenRenderer() {
	}

	/**
	 * 渲染入口（RenderLevelStageEvent AFTER_ENTITIES 阶段调用）。
	 * <p>
	 * Render entry (called at RenderLevelStageEvent AFTER_ENTITIES stage).
	 *
	 * @param event 渲染事件 / the render event
	 */
	public static void render(RenderLevelStageEvent event) {
		if (ClientCitizenCache.size() == 0)
			return;
		Camera cam = event.getCamera();
		Vec3 cp = cam.getPosition();
		Vector3f left = cam.getLeftVector();
		Vector3f up = cam.getUpVector();
		float time = Minecraft.getInstance().level != null
				? Minecraft.getInstance().level.getGameTime() + event.getPartialTick()
				: 0.0f;

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();

		PoseStack pose = event.getPoseStack();
		pose.pushPose();
		pose.translate(-cp.x, -cp.y, -cp.z);
		Matrix4f mat = pose.last().pose();

		Tesselator tes = Tesselator.getInstance();
		BufferBuilder buf = tes.getBuilder();
		buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		for (ClientCitizen c : ClientCitizenCache.values()) {
			// 近距已由假实体渲染，跳过 / near range is rendered by fake entities, skip
			if (FakeCitizenManager.has(c.id))
				continue;
			double[] pos = c.renderPos();
			double dx = pos[0] - cp.x;
			double dy = pos[1] - cp.y;
			double dz = pos[2] - cp.z;
			double d2 = dx * dx + dy * dy + dz * dz;
			if (d2 > MAX_DIST2)
				continue;
			AABB cullBox = new AABB(pos[0] - 0.4, pos[1], pos[2] - 0.4, pos[0] + 0.4, pos[1] + 1.9, pos[2] + 0.4);
			if (!event.getFrustum().isVisible(cullBox))
				continue;
			int s = (c.state & 0xFF) % CitizenState.STATE_COUNT;
			float[] col = COLORS[s];
			if (d2 <= LOWPOLY_DIST2)
				emitHumanoid(buf, mat, c, pos, col, time);
			else
				emitBillboard(buf, mat, left, up, pos, col);
		}
		tes.end();
		pose.popPose();
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	/**
	 * 远距 LOD：以脚底为锚点的竖直广告牌（4 顶点）。
	 * <p>
	 * Far-range LOD: vertical billboard anchored at the feet (4 vertices).
	 */
	private static void emitBillboard(BufferBuilder buf, Matrix4f mat, Vector3f left, Vector3f up, double[] pos,
			float[] col) {
		double lx = left.x * BILLBOARD_HALF_WIDTH;
		double ly = left.y * BILLBOARD_HALF_WIDTH;
		double lz = left.z * BILLBOARD_HALF_WIDTH;
		double ux = up.x * BILLBOARD_HEIGHT;
		double uy = up.y * BILLBOARD_HEIGHT;
		double uz = up.z * BILLBOARD_HEIGHT;
		quad(buf, mat,
				(float) (pos[0] - lx), (float) (pos[1] - ly), (float) (pos[2] - lz),
				(float) (pos[0] + lx), (float) (pos[1] + ly), (float) (pos[2] + lz),
				(float) (pos[0] + lx + ux), (float) (pos[1] + ly + uy), (float) (pos[2] + lz + uz),
				(float) (pos[0] - lx + ux), (float) (pos[1] - ly + uy), (float) (pos[2] - lz + uz),
				col[0], col[1], col[2]);
	}

	/**
	 * 中距 LOD：低模人形（躯干 + 头两个盒体，按 16 向朝向旋转，移动时起伏）。
	 * <p>
	 * Mid-range LOD: low-poly humanoid (body + head boxes, rotated by the
	 * 16-way facing, bobbing while moving).
	 */
	private static void emitHumanoid(BufferBuilder buf, Matrix4f mat, ClientCitizen c, double[] pos, float[] col,
			float time) {
		int d = c.lastDir & 15; // lastDir 保证非 NONE / lastDir is guaranteed non-NONE
		float cos = CitizenState.DIR_X[d] / 1024.0f;
		float sin = CitizenState.DIR_Z[d] / 1024.0f;
		float bob = 0.0f;
		if (c.isMoving())
			bob = Mth.abs(Mth.sin(time * 0.6f + (c.id & 7) * 1.7f)) * BOB_AMP;
		double cy = pos[1] + bob;
		addBox(buf, mat, pos[0], cy, pos[2], BODY_W, BODY_H, BODY_D, cos, sin, col[0], col[1], col[2]);
		addBox(buf, mat, pos[0], cy + BODY_H, pos[2], HEAD_SIZE, HEAD_SIZE, HEAD_SIZE, cos, sin,
				HEAD_COLOR[0], HEAD_COLOR[1], HEAD_COLOR[2]);
	}

	/**
	 * 发射一个绕 Y 轴旋转的盒体（24 顶点）。局部 +X 为"前方"（移动方向）。
	 * <p>
	 * Emits a Y-rotated box (24 vertices). Local +X is "forward" (movement direction).
	 */
	private static void addBox(BufferBuilder buf, Matrix4f mat, double cx, double baseY, double cz,
			float w, float h, float dep, float cos, float sin, float r, float g, float b) {
		float hw = w * 0.5f;
		float hd = dep * 0.5f;
		// 前向半向量（局部 +X）与侧向半向量（局部 +Z） / forward half-vector (local +X) and side half-vector (local +Z)
		float fx = hw * cos, fz = hw * sin;
		float sx = -hd * sin, sz = hd * cos;
		float cxF = (float) cx, czF = (float) cz;
		float y0 = (float) baseY, y1 = (float) (baseY + h);
		// 8 角点：v[前/后][左/右] / 8 corners: v[front/back][left/right]
		float xBL = cxF - fx + sx, zBL = czF - fz + sz; // 后左 / back-left
		float xBR = cxF - fx - sx, zBR = czF - fz - sz; // 后右 / back-right
		float xFR = cxF + fx - sx, zFR = czF + fz - sz; // 前右 / front-right
		float xFL = cxF + fx + sx, zFL = czF + fz + sz; // 前左 / front-left
		// 底/顶 / bottom & top
		quad(buf, mat, xBL, y0, zBL, xBR, y0, zBR, xFR, y0, zFR, xFL, y0, zFL, r, g, b);
		quad(buf, mat, xBL, y1, zBL, xFL, y1, zFL, xFR, y1, zFR, xBR, y1, zBR, r, g, b);
		// 前/后 / front & back
		quad(buf, mat, xFR, y0, zFR, xFL, y0, zFL, xFL, y1, zFL, xFR, y1, zFR, r, g, b);
		quad(buf, mat, xBL, y0, zBL, xBR, y0, zBR, xBR, y1, zBR, xBL, y1, zBL, r, g, b);
		// 左/右 / left & right
		quad(buf, mat, xFL, y0, zFL, xBL, y0, zBL, xBL, y1, zBL, xFL, y1, zFL, r, g, b);
		quad(buf, mat, xBR, y0, zBR, xFR, y0, zFR, xFR, y1, zFR, xBR, y1, zBR, r, g, b);
	}

	private static void quad(BufferBuilder buf, Matrix4f mat,
			float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4,
			float r, float g, float b) {
		buf.vertex(mat, x1, y1, z1).color(r, g, b, ALPHA).endVertex();
		buf.vertex(mat, x2, y2, z2).color(r, g, b, ALPHA).endVertex();
		buf.vertex(mat, x3, y3, z3).color(r, g, b, ALPHA).endVertex();
		buf.vertex(mat, x4, y4, z4).color(r, g, b, ALPHA).endVertex();
	}
}
