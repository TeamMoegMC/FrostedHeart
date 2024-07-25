package com.teammoeg.frostedheart.util.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.*;

public class RenderHelper {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final ActiveRenderInfo info = MC.gameRenderer.getActiveRenderInfo();

    public static Matrix4f projectionMatrix;
    /**
     * 获取一个世界坐标显示在屏幕中的坐标
     * @param pos 世界坐标
     */
    public static Vector2f worldPosToScreenPos(Vector3f pos) {
        if (projectionMatrix == null) return Vector2f.ZERO;
        int screenWidth = ClientUtils.screenWidth();
        int screenHeight = ClientUtils.screenHeight();

        Vector3f relativePos = new Vector3f(info.getProjectedView().subtract(pos.getX(), pos.getY(), pos.getZ()).inverse());
        Vector4f result = new Vector4f(relativePos);

        Quaternion cameraRotation = getCameraRotation();
        result.transform(cameraRotation);
        //当坐标超出摄像机范围时
        if (!isPointInFrustum(pos, cameraRotation)) {
            float screenX, screenY;
            float x = result.getX();
            float y = result.getY();

            screenY = x < 0 ? (screenHeight * 0.5F) + y / x * (screenWidth  * 0.5F) : (screenHeight * 0.5F) - y / x * (screenWidth  * 0.5F);
            screenX = y > 0 ? (screenWidth  * 0.5F) + x / y * (screenHeight * 0.5F) : (screenWidth  * 0.5F) - x / y * (screenHeight * 0.5F);

            return new Vector2f(screenX, screenY);
        }
        //应用投影矩阵
        result.transform(projectionMatrix);
        result.perspectiveDivide();

        float screenX = screenWidth * (0.5F + result.getX() * 0.5F);
        float screenY = screenHeight * (0.5F - result.getY() * 0.5F);
        return new Vector2f(screenX, screenY);
    }
    /**
     * 检查一个坐标是否在视野中
     * @param pos 世界坐标
     */
    public static boolean isPointInFrustum(Vector3f pos, Quaternion cameraRotation) {
        ClippingHelper clippingHelper = new ClippingHelper(new Matrix4f(cameraRotation), projectionMatrix);
        Vector3d cameraPos = info.getProjectedView();
        AxisAlignedBB pointAABB;

        float distance = (float)cameraPos.distanceTo(new Vector3d(pos));
        if (distance > 512) {
            double x = (pos.getX() - cameraPos.x) / distance;
            double y = (pos.getY() - cameraPos.y) / distance;
            double z = (pos.getZ() - cameraPos.z) / distance;

            pointAABB = new AxisAlignedBB(x, y, z, x, y, z);
            clippingHelper.setCameraPosition(0,0, 0);
        } else {
            pointAABB = new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
            clippingHelper.setCameraPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        }

        return clippingHelper.isBoundingBoxInFrustum(pointAABB);
    }

    public static Quaternion getCameraRotation() {
        Quaternion cameraRotation = info.getRotation().copy();
        //调整旋转轴
        cameraRotation.multiply(new Quaternion(Vector3f.YN, 180, true));
        cameraRotation.conjugate();

        return cameraRotation;
    }

    public static Vector3d getCameraPos() {
        return info.getProjectedView();
    }
}
