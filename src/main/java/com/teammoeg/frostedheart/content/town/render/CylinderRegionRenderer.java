/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */
package com.teammoeg.frostedheart.content.town.render;

import com.lowdragmc.lowdraglib.client.shader.management.ShaderProgram;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.bootstrap.client.FHShaders;
import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.OccupiedCell;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

/**
 * 柱体区域渲染器。
 *
 * 每个柱体的数据格式 (小端序)：
 *     [0..1]  short 柱体数量 n
 *     [2..)   n 条记录, 每条 12 字节:
 *         +0  int   x
 *         +4  short y
 *         +6  int   z
 *         +10 byte  h1
 *         +11 byte  h2
 *
 * 视觉分段（在片元着色器中判断）：
 *   - (x, y+1, z) 那一格的顶面   : 红蓝斜条纹
 *   - y .. h1 的柱体侧面        : 淡红/淡蓝斜条纹
 *   - h1 .. h2 的柱体侧面       : 淡黄绿色棋盘格
 *
 */
@OnlyIn(Dist.CLIENT)
public final class CylinderRegionRenderer {

    // ---- 布局常量 ----------------------------------------------------
    private static final int VERTICES_PER_INSTANCE = 42; // 36 外壳 + 6 顶面
    private static final int SSBO_BINDING          = 0;
    private static final int ENTRY_BYTES           = 12;
    private static final int HEADER_BYTES          = 2;

    // ---- GL 资源 ----------------------------------------------------
    private static ShaderProgram program;
    private static int  vao            = 0;
    private static int  ssbo           = 0;
    private static int  ssboCapacity   = 0;
    private static RenderTarget overlayTarget;
    private CylinderRegionRenderer() {}

    // ==================================================================
    // 公共入口
    // ==================================================================
    private static RenderTarget getOrCreateOverlayTarget(int width, int height) {
        if (overlayTarget == null) {
            overlayTarget = new TextureTarget(
                    width, height, false, Minecraft.ON_OSX);
            overlayTarget.setClearColor(0, 0, 0, 0);
        } else if (overlayTarget.width != width
                || overlayTarget.height != height) {
            overlayTarget.resize(width, height, Minecraft.ON_OSX);
        }
        return overlayTarget;
    }
    /**
     * 渲染一批柱体。应在世界渲染阶段调用（比如
     * {@code RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_TERRAIN}）。
     *
     * @param poseStack 当前世界渲染的 pose（已含相机反向位移）
     * @param cylinders 柱体列表
     */
    public static void render(Collection<OccupiedCell> cylinders) {
        /*if (cylinders.isEmpty()) return;
        RenderSystem.assertOnRenderThread();
        Minecraft minecraft =Minecraft.getInstance();
        ensureResources();
        uploadCylinders(cylinders);

        // 保存外部 GL 状态
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao     = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        RenderTarget overlay = getOrCreateOverlayTarget(
                mainTarget.width, mainTarget.height);
        program.use(cache -> {
			Minecraft mc = Minecraft.getInstance();
			float time;
			if (mc.player != null) {
				time = (mc.player.tickCount + mc.getFrameTime()) / 20;
			} else {
				time = System.currentTimeMillis() / 1000f;
			}
			cache.glUniform1F("iTime", time);
			cache.glUniform2F("iResolution", overlay.width, overlay.height);
		});

        GL30.glBindVertexArray(vao);
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, SSBO_BINDING, ssbo);

        // 半透明叠加，不写深度
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        GL31.glDrawArraysInstanced(
                GL11.GL_TRIANGLES, 0, VERTICES_PER_INSTANCE, cylinders.size());

        // 恢复
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        GL30.glBindVertexArray(prevVao);
        program.release();
        GL20.glUseProgram(prevProgram);*/
    }

    /** 资源释放（在客户端卸载 / 世界退出时调用）。 */
    public static void reset() {
        if (RenderSystem.isOnRenderThread()) {
            release();
        } else {
            RenderSystem.recordRenderCall(CylinderRegionRenderer::release);
        }
    }

    // ==================================================================
    // 数据上传
    // ==================================================================

    private static void uploadCylinders(Collection<OccupiedCell> cylinders) {
        int count      = cylinders.size();
        int totalBytes = HEADER_BYTES + count * ENTRY_BYTES;
        int padded     = (totalBytes + 3) & ~3;   // std430 uint 数组需 4 字节对齐

        ByteBuffer buf = MemoryUtil.memAlloc(padded);
        try {
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.putShort((short) count);
            for (OccupiedCell c : cylinders) {
                buf.putInt(c.getPos().getX());
                buf.putShort((short) c.getPos().getY());
                buf.putInt(c.getPos().getZ());
                buf.put((byte) c.getReachableHeight());
                buf.put((byte) c.height());
            }
            while (buf.position() < padded) buf.put((byte) 0);
            buf.flip();

            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssbo);
            if (padded > ssboCapacity) {
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER,
                        buf, GL15.GL_DYNAMIC_DRAW);
                ssboCapacity = padded;
            } else {
                GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, buf);
            }
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        } finally {
            MemoryUtil.memFree(buf);
        }
    }

    // ==================================================================
    // 初始化 / 释放
    // ==================================================================

    private static void ensureResources() {
        if (program != null) return;

        program = new ShaderProgram();
        FHShaders.getRegion().attachShader(program);
        program.linkProgram();
        program.release();
        vao   = GL30.glGenVertexArrays();
        ssbo  = GL15.glGenBuffers();
    }

    private static void release() {
        if (program != null) { program.delete(); }
        if (vao     != 0) { GL30.glDeleteVertexArrays(vao);  vao     = 0; }
        if (ssbo    != 0) { GL15.glDeleteBuffers(ssbo);      ssbo    = 0; }
        ssboCapacity = 0;
    }
}