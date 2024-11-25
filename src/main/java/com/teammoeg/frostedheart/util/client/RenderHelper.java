package com.teammoeg.frostedheart.util.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class RenderHelper {
    public static Matrix4f projectionMatrix;
    public static PoseStack poseStack;
    public static Frustum frustum;
    public static Camera camera;

    /**
     * 获取一个世界坐标显示在屏幕中的坐标
     * @param worldPos 世界坐标
     */
    public static Vec2 worldPosToScreenPos(Vec3 worldPos) {
        if (projectionMatrix == null) return Vec2.ZERO;
        int screenWidth = ClientUtils.screenWidth();
        int screenHeight = ClientUtils.screenHeight();
        // 调整摄像机旋转以用于计算
        Quaternionf cameraRotation = getCameraRotation().conjugate(new Quaternionf()).rotateLocalY(Mth.PI);
        Vector4f result;

        // 当路径点不在摄像机范围时将屏幕坐标映射到屏幕边缘
        if (!isPosInFrustum(worldPos)) {
            Vector3f cameraPos = getCameraPos().subtract(worldPos).toVector3f();
            result = new Vector4f(cameraPos, 1F).rotate(cameraRotation);

            float x = -result.x;
            float y = -result.y;
            float screenX = (screenWidth * 0.5F) + (x / y) * (screenHeight * 0.5F) * (y > 0 ? 1 : -1);
            float screenY = (screenHeight * 0.5F) + (y / x) * (screenWidth * 0.5F) * (x < 0 ? 1 : -1);
            return new Vec2(screenX, screenY);
        }

        // 视图矩阵
        // 下面的代码来自 (https://github.com/LouisQuepierts/ThatSkyInteractions/blob/teacon-jiachen/src/main/java/net/quepierts/thatskyinteractions/client/gui/layer/World2ScreenWidgetLayer.java)
        Matrix4f viewMatrix = new Matrix4f()
                .rotation(cameraRotation)
                .translate(getCameraPos().reverse().toVector3f());
        result = new Vector4f(worldPos.toVector3f(), 1F)
                .mul(new Matrix4f(projectionMatrix).mul(viewMatrix));

        float screenX = (result.x / result.z * 0.5F + 0.5F) * screenWidth;
        float screenY = (1.0F - (result.y / result.z * 0.5F + 0.5F)) * screenHeight;
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
        if (camera == null) return new Quaternionf();
        return camera.rotation();
    }

    public static Vec3 getCameraPos() {
        if (camera == null) return Vec3.ZERO;
        return camera.getPosition();
    }
}
