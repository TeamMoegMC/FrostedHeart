package com.teammoeg.frostedheart.util.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public class RenderHelper {
    public static Matrix4f projectionMatrix;
    public static Frustum frustum;
    public static Camera camera;

    /**
     * 获取一个世界坐标显示在屏幕中的坐标
     * @param pos 世界坐标
     */
    public static Vec2 worldPosToScreenPos(Vec3 pos) {
        if (projectionMatrix == null) return Vec2.ZERO;
        int screenWidth = ClientUtils.screenWidth();
        int screenHeight = ClientUtils.screenHeight();

        Vec3 relativePos = getCameraPos().subtract(pos.x, pos.y, pos.z).reverse();
        Vector4f result = new Vector4f((float)relativePos.x, (float)relativePos.y, (float)relativePos.z, 1);

        result.rotate(getCameraRotation());
        //当坐标超出摄像机范围时
        if (!isPosInFrustum(pos)) {
            float screenX, screenY;
            float x = result.x;
            float y = result.y;

            screenY = (x < 0 ? screenHeight * 0.5F + y : screenHeight * 0.5F - y) / x*(screenWidth  * 0.5F);
            screenX = (y > 0 ? screenWidth  * 0.5F + x : screenWidth  * 0.5F - x) / y*(screenHeight * 0.5F);

            return new Vec2(screenX, screenY);
        }
        //应用投影矩阵
        result.mul(projectionMatrix);
        result.div(result.w);

        float screenX = screenWidth * (0.5F + result.x * 0.5F);
        float screenY = screenHeight * (0.5F - result.y * 0.5F);
        return new Vec2(screenX, screenY);
    }

    /**
     * 检查一个坐标是否在视野中
     * @param pos 世界坐标
     */
    public static boolean isPosInFrustum(Vec3 pos) {
        if (frustum == null) return false;
        Vec3 cameraPos = getCameraPos();
        AABB pointAABB;

        //1.16中远距离判定会出问题，如果1.20没问题可以删掉
        double distance = cameraPos.distanceTo(pos);
        if (distance > 512) {
            double x = (pos.x() - cameraPos.x) / distance;
            double y = (pos.y() - cameraPos.y) / distance;
            double z = (pos.z() - cameraPos.z) / distance;

            pointAABB = new AABB(x, y, z, x, y, z);
            frustum.prepare(0,0, 0);
        } else {
            pointAABB = new AABB(pos.x(), pos.y(), pos.z(), pos.x(), pos.y(), pos.z());
            frustum.prepare(cameraPos.x, cameraPos.y, cameraPos.z);
        }

        return frustum.isVisible(pointAABB);
    }

    public static Quaternionf getCameraRotation() {
        if (camera == null) return new Quaternionf(0, 0, 0, 0);
        Quaternionf cameraRotation = new Quaternionf(camera.rotation());
        //调整旋转轴
        cameraRotation.rotateLocalY(Mth.PI);
        cameraRotation.conjugate();

        return cameraRotation;
    }

    public static Vec3 getCameraPos() {
        if (camera == null) return Vec3.ZERO;
        return camera.getPosition();
    }

    /**
     * 在屏幕中绘制一个圆
     * @param radius 半径
     * @param partial 圆的完整度 {@code 0.0 ~ 1.0}
     */
    public static void drawPartialCircle(int x, int y, double radius, float partial, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(x, y, 0).endVertex();
        for (int i = -180; i <= 360*partial-180; i++) { //为了让圆顺时针绘制
            double angle = i * Math.PI / 180;
            double x2 = x + Math.sin(-angle) * radius;
            double y2 = y + Math.cos(angle) * radius;
            bufferBuilder.vertex(x2, y2, 0).color(color).endVertex();
        }

        tessellator.end();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * 在屏幕中绘制一个多边形
     * @param radius 半径
     * @param sides 多边形的边数
     */
    public static void drawPolygon(int x, int y, double radius, int sides, int color) {
        sides = Mth.clamp(sides, 3, 360);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(x, y, 0).endVertex();
        for (int i = 0; i <= sides; i++) {
            double angle = i * (360F/sides) * Math.PI / 180;
            double x2 = x + Math.sin(angle) * radius;
            double y2 = y + Math.cos(angle) * radius;
            bufferBuilder.vertex(x2, y2, 0).color(color).endVertex();
        }

        tessellator.end();

        RenderSystem.disableBlend();
    }
}
