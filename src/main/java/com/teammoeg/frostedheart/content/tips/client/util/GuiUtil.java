package com.teammoeg.frostedheart.content.tips.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.util.client.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.renderer.culling.Frustum;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.util.math.vector.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class GuiUtil {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Font font = mc.font;
    private static final Camera info = mc.gameRenderer.getMainCamera();
    private static final Map<String, List<String>> textWrapCache = new HashMap<>();
    private static int leftClicked = 0;

    /**
     * 直接在屏幕中渲染一个图标按钮
     * @param icon {@link IconButton}
     * @param color 图标的颜色
     * @param BGColor 未被选中时的背景颜色，为 0 时不显示
     * @return 是否被按下
     */
    public static boolean renderIconButton(PoseStack matrixStack, Point icon, int mouseX, int mouseY, int x, int y, int color, int BGColor) {
        if (color != 0 && isMouseIn(mouseX, mouseY, x, y, 10, 10)) {
            GuiComponent.fill(matrixStack, x, y, x+10, y+10, 50 << 24 | color & 0x00FFFFFF);
        } else if (BGColor != 0) {
            GuiComponent.fill(matrixStack, x, y, x+10, y+10, BGColor);
        }
        return renderButton(matrixStack, mouseX, mouseY, x, y, 10, 10, icon.getX(), icon.getY(), 10, 10, 80, 80, color, IconButton.ICON_LOCATION);
    }

    public static boolean renderButton(PoseStack matrixStack, int mouseX, int mouseY, int x, int y, int w, int h,
                                       float uOffset, float vOffset, int uWidth, int vHeight, int textureW, int textureH, int color, ResourceLocation resourceLocation) {
        if (color != 0) {
            float alpha = (color >> 24 & 0xFF) / 255F;
            float r = (color >> 16 & 0xFF) / 255F;
            float g = (color >> 8 & 0xFF) / 255F;
            float b = (color & 0xFF) / 255F;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(r, g, b, alpha);
            mc.getTextureManager().bind(resourceLocation);
            GuiComponent.blit(matrixStack, x, y, w, h, uOffset, vOffset, uWidth, vHeight, textureW, textureH);
            RenderSystem.disableBlend();
        } else {
            mc.getTextureManager().bind(resourceLocation);
            GuiComponent.blit(matrixStack, x, y, w, h, uOffset, vOffset, uWidth, vHeight, textureW, textureH);
        }

        return isMouseIn(mouseX, mouseY, x, y, w, h) && isLeftClicked();
    }

    /**
     * 渲染一个图标
     * @param icon {@link IconButton}
     * @param color 图标的颜色
     */
    public static void renderIcon(PoseStack matrixStack, Point icon, int x, int y, int color) {
        if (color != 0) {
            float alpha = (color >> 24 & 0xFF) / 255F;
            float r = (color >> 16 & 0xFF) / 255F;
            float g = (color >> 8 & 0xFF) / 255F;
            float b = (color & 0xFF) / 255F;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(r, g, b, alpha);
            mc.getTextureManager().bind(IconButton.ICON_LOCATION);
            GuiComponent.blit(matrixStack, x, y, 10, 10, icon.getX(), icon.getY(), 10, 10, 80, 80);
            RenderSystem.disableBlend();
        } else {
            mc.getTextureManager().bind(IconButton.ICON_LOCATION);
            GuiComponent.blit(matrixStack, x, y, 10, 10, icon.getX(), icon.getY(), 10, 10, 80, 80);
        }
    }

    public static boolean isMouseIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y && mouseX <= x + w && mouseY <= y + h;
    }

    public static boolean isLeftDown() {
        return GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), 0) == 1;
    }

    public static boolean isLeftClicked() {
        if (isLeftDown()) {
            leftClicked++;
            return leftClicked == 1;
        } else {
            leftClicked = 0;
            return false;
        }
    }

    public static int getMouseX() {
        return (int)(mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / (double)mc.getWindow().getScreenWidth());
    }

    public static int getMouseY() {
        return (int)(mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / (double)mc.getWindow().getScreenHeight());
    }

    public static int formatAndDraw(Component component, PoseStack ms, float x, float y, int maxWidth, int color, int lineSpace, boolean shadow) {
        String text = component.getString().replaceAll("&(?!&)", "\u00a7")
                .replaceAll("\\$configPath\\$", FMLPaths.CONFIGDIR.get().toString().replaceAll("\\\\", "\\\\\\\\"));

        return drawWrapString(text, ms, x, y, maxWidth, color, lineSpace, shadow);
    }

    public static int drawWrapText(Component component, PoseStack ms, float x, float y, int maxWidth, int color, int lineSpace, boolean shadow) {
        return drawWrapString(component.getString(), ms, x, y, maxWidth, color, lineSpace, shadow);
    }

    public static int drawWrapString(String text, PoseStack ms, float x, float y, int maxWidth, int color, int lineSpace, boolean shadow) {
        List<String> lines = wrapString(text, maxWidth);

        for (int i = 0; i < lines.size(); i++) {
            if (i == 0) {
                if (shadow) {
                    font.drawShadow(ms, lines.get(i), x, y, color);
                } else {
                    font.draw(ms, lines.get(i), x, y, color);
                }
            } else {
                if (shadow) {
                    font.drawShadow(ms, lines.get(i), x, y + (i * lineSpace), color);
                } else {
                    font.draw(ms, lines.get(i), x, y + (i * lineSpace), color);
                }
            }
        }

        return lines.size();
    }

    public static List<String> wrapString(String text, int maxWidth) {
        //因为整不明白原版的方法所以搞了个傻子都会用的换行
        List<String> lines = new ArrayList<>();
        boolean addToCache = false;
        maxWidth = Math.max(1, maxWidth);
        if (textWrapCache.size() > 1024) textWrapCache.clear();

        if (textWrapCache.containsKey(text + maxWidth)) {
            lines = new ArrayList<>(textWrapCache.get(text + maxWidth));
        } else {
            StringBuilder line = new StringBuilder();
            String[] words = text.split(" ");
            for (String word : words) {
                if (font.width(word) > maxWidth) {
                    for (char c : word.toCharArray()) {
                        String potentialLine = line.toString() + c;
                        int width = font.width(potentialLine);

                        if (width > maxWidth) {
                            if (line.toString().endsWith("\u00A7")) {
                                line = new StringBuilder(line.substring(0, line.length() - 1));
                                lines.add(line.toString());
                                line = new StringBuilder("\u00A7" + c);
                            } else {
                                lines.add(line.toString());
                                line = new StringBuilder(String.valueOf(c));
                            }
                        } else {
                            line = new StringBuilder(potentialLine);
                        }
                    }
                    line.append(" ");
                } else {
                    String potentialLine = line + word + " ";
                    int width = font.width(potentialLine);

                    if (width > maxWidth) {
                        if (line.toString().endsWith("\u00A7")) {
                            line = new StringBuilder(line.substring(0, line.length() - 1));
                            lines.add(line.toString());
                            line = new StringBuilder("\u00A7" + word + " ");
                        } else {
                            lines.add(line.toString());
                            line = new StringBuilder(word + " ");
                        }
                    } else {
                        line = new StringBuilder(potentialLine);
                    }
                }

            }

            if (line.length() > 0) {
                lines.add(line.toString());
            }

            //为每行开头添加生效的格式化代码
            Pattern pattern = Pattern.compile("\u00A7.");
            StringBuilder formattingCode = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                lines.set(i, formattingCode + lines.get(i));

                Matcher matcher = pattern.matcher(lines.get(i).substring(formattingCode.length()));
                while (matcher.find() && formattingCode.length() < 32) {
                    if (matcher.group().equals("\u00A7r")) {
                        formattingCode = new StringBuilder();
                    } else {
                        formattingCode.append(matcher.group());
                    }
                }
            }
            addToCache = true;
        }

        if (addToCache) {
            textWrapCache.put(text + maxWidth, lines);
        }

        return lines;
    }

    /**
     * 根据 partial 绘制一个圆，如果只想绘制一个完整的圆请使用 {@link GuiUtil#drawPolygon(int, int, double, int, int)}
     * @param radius 半径
     * @param partial 圆的完整度 {@code 0.0 ~ 1.0}
     */
    public static void drawPartialCircle(int x, int y, double radius, float partial, int color) {
        float alpha = (color >> 24 & 0xFF) / 255F;
        float r = (color >> 16 & 0xFF) / 255F;
        float g = (color >> 8 & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;

        RenderSystem.color4f(r, g, b, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

        bufferBuilder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        bufferBuilder.vertex(x, y, 0).endVertex();
        for (int i = -180; i <= 360*partial-180; i++) { //为了让圆顺时针绘制
            double angle = i * Math.PI / 180;
            double x2 = x + Math.sin(-angle) * radius;
            double y2 = y + Math.cos(angle) * radius;
            bufferBuilder.vertex(x2, y2, 0).endVertex();
        }

        tessellator.end();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * 绘制一个多边形
     * @param radius 半径
     * @param sides 多边形的边数，大部分情况下 50 已经够圆了
     */
    public static void drawPolygon(int x, int y, double radius, int sides, int color) {
        sides = Mth.clamp(sides, 3, 360);

        float alpha = (color >> 24 & 0xFF) / 255F;
        float r = (color >> 16 & 0xFF) / 255F;
        float g = (color >> 8 & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;

        RenderSystem.color4f(r, g, b, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

        bufferBuilder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        bufferBuilder.vertex(x, y, 0).endVertex();
        for (int i = 0; i <= sides; i++) {
            double angle = i * (360F/sides) * Math.PI / 180;
            double x2 = x + Math.sin(angle) * radius;
            double y2 = y + Math.cos(angle) * radius;
            bufferBuilder.vertex(x2, y2, 0).endVertex();
        }

        tessellator.end();

        RenderSystem.disableBlend();
    }

    /**
     * 获取一个世界坐标显示在屏幕中的坐标
     * @param pos 世界坐标
     */
    public static Vec2 worldPosToScreenPos(Vector3f pos) {
        if (mc.player == null) return Vec2.ZERO;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        //透视矩阵
        Matrix4f projectionMatrix = mc.gameRenderer.getProjectionMatrix(info, mc.getFrameTime(), true);

        //摄像机坐标
        Vec3 cameraPos = info.getPosition();
        Matrix4f cameraPosM = new Matrix4f();
        cameraPosM.setIdentity();
        //转换为摄像机坐标系
        cameraPosM.setTranslation((float)-cameraPos.x, (float)-cameraPos.y, (float)-cameraPos.z);

        //摄像机旋转
        Quaternion cameraRotation = info.rotation().copy();
        //调整摄像机旋转
        cameraRotation.mul(new Quaternion(Vector3f.YN, 180, true));
        cameraRotation.conj();

        Vector4f finalVector = new Vector4f(pos);
        //应用摄像机坐标
        finalVector.transform(cameraPosM);
        //应用摄像机旋转
        finalVector.transform(cameraRotation);
        //当坐标超出摄像机范围时
        if (!isPosInView(pos, cameraRotation, projectionMatrix)) {
            finalVector.normalize();
            float screenX, screenY;
            float halfScreenWidth = screenWidth * 0.5F;
            float halfScreenHeight = screenHeight * 0.5F;
            float x = finalVector.x();
            float y = finalVector.y();

            if (x < 0) {
                screenY = halfScreenHeight + y / x * halfScreenWidth;
            } else {
                screenY = halfScreenHeight - y / x * halfScreenWidth;
            }

            if (y > 0) {
                screenX = halfScreenWidth + x / y * halfScreenHeight;
            } else {
                screenX = halfScreenWidth - x / y * halfScreenHeight;
            }

            return new Vec2(screenX, screenY);
        }
        //应用透视矩阵
        finalVector.transform(projectionMatrix);
        finalVector.perspectiveDivide();

        float screenX = (finalVector.x() * 0.5F + 0.5F) * screenWidth;
        float screenY = screenHeight - ((finalVector.y() * 0.5F + 0.5F) * screenHeight);
        return new Vec2(screenX, screenY);
    }

    /**
     * 检查一个坐标是否在视野中
     * @param pos 世界坐标
     */
    public static boolean isPosInView(Vector3f pos) {
        Quaternion cameraRotation = info.rotation().copy();
        cameraRotation.mul(new Quaternion(Vector3f.YN, 180, true));
        cameraRotation.conj();

        return isPosInView(pos, cameraRotation, mc.gameRenderer.getProjectionMatrix(info, mc.getFrameTime(), true));
    }

    public static boolean isPosInView(Vector3f pos, Quaternion cameraRotation, Matrix4f projection) {
        Frustum clippingHelper = new Frustum(new Matrix4f(cameraRotation), projection);
        Vec3 cameraPos = info.getPosition();
        AABB pointAABB;

        float distance = (float)cameraPos.distanceTo(new Vec3(pos));
        if (distance > 512) {
            double x = (pos.x() - cameraPos.x) / distance;
            double y = (pos.y() - cameraPos.y) / distance;
            double z = (pos.z() - cameraPos.z) / distance;

            pointAABB = new AABB(x, y, z, x, y, z);
            clippingHelper.prepare(0,0, 0);
        } else {
            pointAABB = new AABB(pos.x(), pos.y(), pos.z(), pos.x(), pos.y(), pos.z());
            clippingHelper.prepare(cameraPos.x, cameraPos.y, cameraPos.z);
        }

        return clippingHelper.isVisible(pointAABB);
    }

    public static float getTheta(Vec3 viewDirection, Vector3f cameraToPoint) {
        Vector3f viewDirection3f = new Vector3f((float) viewDirection.x, (float) viewDirection.y, (float) viewDirection.z);
        // 计算两个向量的点积
        float dotProduct = viewDirection3f.dot(cameraToPoint);

        // 计算两个向量的模
        float viewDirectionMagnitude = (float) Math.sqrt(viewDirection3f.dot(viewDirection3f));
        float cameraToPointMagnitude = (float) Math.sqrt(cameraToPoint.dot(cameraToPoint));

        // 计算夹角的余弦值
        float cosTheta = dotProduct / (viewDirectionMagnitude * cameraToPointMagnitude);

        // 计算夹角（弧度）
        return (float) Math.acos(cosTheta);
    }
}
