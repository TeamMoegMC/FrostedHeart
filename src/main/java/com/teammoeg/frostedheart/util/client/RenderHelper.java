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
import org.joml.Vector4f;

public class RenderHelper {
    public static Matrix4f projectionMatrix;
    public static PoseStack poseStack;
    public static Frustum frustum;
    public static Camera camera;

    /**
     * 获取一个世界坐标显示在屏幕中的坐标
     * @param pos 世界坐标
     */
    public static Vec2 worldPosToScreenPos(Vec3 pos) { //TODO 视角晃动时结果有些问题
        if (projectionMatrix == null) return Vec2.ZERO;
        int screenWidth = ClientUtils.screenWidth();
        int screenHeight = ClientUtils.screenHeight();

        //转换为相对坐标
        Vec3 relativePos = getCameraPos().subtract(pos.x, pos.y, pos.z);
        Vector4f result = new Vector4f((float)relativePos.x, (float)relativePos.y, (float)relativePos.z, 1);

        Quaternionf camera = getCameraRotation();
        //调整摄像机旋转以用于计算
        camera.conjugate();
        camera.rotateLocalY(Mth.PI);

        result.rotate(camera);
        ClientUtils.mc().options.bobView();
        //当路径点不在摄像机范围时将屏幕坐标映射到屏幕边缘
        if (!isPosInFrustum(pos)) {
            float screenX, screenY;
            float x = -result.x;
            float y = -result.y;
            screenX = (screenWidth * 0.5F) + (x / y) * (screenHeight * 0.5F) * (y > 0 ? 1 : -1);
            screenY = (screenHeight * 0.5F) + (y / x) * (screenWidth * 0.5F) * (x < 0 ? 1 : -1);

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
        return new Quaternionf(camera.rotation());
    }

    public static Vec3 getCameraPos() {
        if (camera == null) return Vec3.ZERO;
        return camera.getPosition();
    }
}
