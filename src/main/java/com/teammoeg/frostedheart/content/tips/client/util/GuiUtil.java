package com.teammoeg.frostedheart.content.tips.client.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.util.client.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiUtil {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final FontRenderer font = mc.fontRenderer;
    private static final ActiveRenderInfo info = mc.gameRenderer.getActiveRenderInfo();
    private static final Map<String, List<String>> textWrapCache = new HashMap<>();
    private static int leftClicked = 0;

    /**
     * 直接在屏幕中渲染一个图标按钮
     * @param icon {@link IconButton}
     * @param color 图标的颜色
     * @param BGColor 未被选中时的背景颜色，为 0 时不显示
     * @return 是否被按下
     */
    public static boolean renderIconButton(MatrixStack matrixStack, Point icon, int mouseX, int mouseY, int x, int y, int color, int BGColor) {
        if (color != 0 && isMouseIn(mouseX, mouseY, x, y, 10, 10)) {
            AbstractGui.fill(matrixStack, x, y, x+10, y+10, 50 << 24 | color & 0x00FFFFFF);
        } else if (BGColor != 0) {
            AbstractGui.fill(matrixStack, x, y, x+10, y+10, BGColor);
        }
        return renderButton(matrixStack, mouseX, mouseY, x, y, 10, 10, icon.getX(), icon.getY(), 10, 10, 80, 80, color, IconButton.ICON_LOCATION);
    }

    public static boolean renderButton(MatrixStack matrixStack, int mouseX, int mouseY, int x, int y, int w, int h,
                                       float uOffset, float vOffset, int uWidth, int vHeight, int textureW, int textureH, int color, ResourceLocation resourceLocation) {
        if (color != 0) {
            float alpha = (color >> 24 & 0xFF) / 255F;
            float r = (color >> 16 & 0xFF) / 255F;
            float g = (color >> 8 & 0xFF) / 255F;
            float b = (color & 0xFF) / 255F;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(r, g, b, alpha);
            mc.getTextureManager().bindTexture(resourceLocation);
            AbstractGui.blit(matrixStack, x, y, w, h, uOffset, vOffset, uWidth, vHeight, textureW, textureH);
            RenderSystem.disableBlend();
        } else {
            mc.getTextureManager().bindTexture(resourceLocation);
            AbstractGui.blit(matrixStack, x, y, w, h, uOffset, vOffset, uWidth, vHeight, textureW, textureH);
        }

        return isMouseIn(mouseX, mouseY, x, y, w, h) && isLeftClicked();
    }

    /**
     * 渲染一个图标
     * @param icon {@link IconButton}
     * @param color 图标的颜色
     */
    public static void renderIcon(MatrixStack matrixStack, Point icon, int x, int y, int color) {
        if (color != 0) {
            float alpha = (color >> 24 & 0xFF) / 255F;
            float r = (color >> 16 & 0xFF) / 255F;
            float g = (color >> 8 & 0xFF) / 255F;
            float b = (color & 0xFF) / 255F;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(r, g, b, alpha);
            mc.getTextureManager().bindTexture(IconButton.ICON_LOCATION);
            AbstractGui.blit(matrixStack, x, y, 10, 10, icon.getX(), icon.getY(), 10, 10, 80, 80);
            RenderSystem.disableBlend();
        } else {
            mc.getTextureManager().bindTexture(IconButton.ICON_LOCATION);
            AbstractGui.blit(matrixStack, x, y, 10, 10, icon.getX(), icon.getY(), 10, 10, 80, 80);
        }
    }

    public static boolean isMouseIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y && mouseX <= x + w && mouseY <= y + h;
    }

    public static boolean isLeftDown() {
        return GLFW.glfwGetMouseButton(mc.getMainWindow().getHandle(), 0) == 1;
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
        return (int)(mc.mouseHelper.getMouseX() * (double)mc.getMainWindow().getScaledWidth() / (double)mc.getMainWindow().getWidth());
    }

    public static int getMouseY() {
        return (int)(mc.mouseHelper.getMouseY() * (double)mc.getMainWindow().getScaledHeight() / (double)mc.getMainWindow().getHeight());
    }

    public static int formatAndDraw(ITextComponent component, MatrixStack ms, float x, float y, int maxWidth, int color, int lineSpace, boolean shadow) {
        String text = component.getString().replaceAll("&(?!&)", "\u00a7")
                .replaceAll("\\$configPath\\$", FMLPaths.CONFIGDIR.get().toString().replaceAll("\\\\", "\\\\\\\\"));

        return drawWrapString(text, ms, x, y, maxWidth, color, lineSpace, shadow);
    }

    public static int drawWrapText(ITextComponent component, MatrixStack ms, float x, float y, int maxWidth, int color, int lineSpace, boolean shadow) {
        return drawWrapString(component.getString(), ms, x, y, maxWidth, color, lineSpace, shadow);
    }

    public static int drawWrapString(String text, MatrixStack ms, float x, float y, int maxWidth, int color, int lineSpace, boolean shadow) {
        List<String> lines = wrapString(text, maxWidth);

        for (int i = 0; i < lines.size(); i++) {
            if (i == 0) {
                if (shadow) {
                    font.drawStringWithShadow(ms, lines.get(i), x, y, color);
                } else {
                    font.drawString(ms, lines.get(i), x, y, color);
                }
            } else {
                if (shadow) {
                    font.drawStringWithShadow(ms, lines.get(i), x, y + (i * lineSpace), color);
                } else {
                    font.drawString(ms, lines.get(i), x, y + (i * lineSpace), color);
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
                if (font.getStringWidth(word) > maxWidth) {
                    for (char c : word.toCharArray()) {
                        String potentialLine = line.toString() + c;
                        int width = font.getStringWidth(potentialLine);

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
                    int width = font.getStringWidth(potentialLine);

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

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();

        bufferBuilder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        bufferBuilder.pos(x, y, 0).endVertex();
        for (int i = -180; i <= 360*partial-180; i++) { //为了让圆顺时针绘制
            double angle = i * Math.PI / 180;
            double x2 = x + Math.sin(-angle) * radius;
            double y2 = y + Math.cos(angle) * radius;
            bufferBuilder.pos(x2, y2, 0).endVertex();
        }

        tessellator.draw();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * 绘制一个多边形
     * @param radius 半径
     * @param sides 多边形的边数，大部分情况下 50 已经够圆了
     */
    public static void drawPolygon(int x, int y, double radius, int sides, int color) {
        sides = MathHelper.clamp(sides, 3, 360);

        float alpha = (color >> 24 & 0xFF) / 255F;
        float r = (color >> 16 & 0xFF) / 255F;
        float g = (color >> 8 & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;

        RenderSystem.color4f(r, g, b, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();

        bufferBuilder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        bufferBuilder.pos(x, y, 0).endVertex();
        for (int i = 0; i <= sides; i++) {
            double angle = i * (360F/sides) * Math.PI / 180;
            double x2 = x + Math.sin(angle) * radius;
            double y2 = y + Math.cos(angle) * radius;
            bufferBuilder.pos(x2, y2, 0).endVertex();
        }

        tessellator.draw();

        RenderSystem.disableBlend();
    }

    /**
     * 获取一个世界坐标显示在屏幕中的坐标
     * @param pos 世界坐标
     */
    public static Vector2f worldPosToScreenPos(Vector3f pos) {
        if (mc.player == null) return Vector2f.ZERO;

        int screenWidth = mc.getMainWindow().getScaledWidth();
        int screenHeight = mc.getMainWindow().getScaledHeight();
        //透视矩阵
        Matrix4f projectionMatrix = mc.gameRenderer.getProjectionMatrix(info, mc.getRenderPartialTicks(), true);

        //摄像机坐标
        Vector3d cameraPos = info.getProjectedView();
        Matrix4f cameraPosM = new Matrix4f();
        cameraPosM.setIdentity();
        //转换为摄像机坐标系
        cameraPosM.setTranslation((float)-cameraPos.x, (float)-cameraPos.y, (float)-cameraPos.z);

        //摄像机旋转
        Quaternion cameraRotation = info.getRotation().copy();
        //调整摄像机旋转
        cameraRotation.multiply(new Quaternion(Vector3f.YN, 180, true));
        cameraRotation.conjugate();

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
            float x = finalVector.getX();
            float y = finalVector.getY();

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

            return new Vector2f(screenX, screenY);
        }
        //应用透视矩阵
        finalVector.transform(projectionMatrix);
        finalVector.perspectiveDivide();

        float screenX = (finalVector.getX() * 0.5F + 0.5F) * screenWidth;
        float screenY = screenHeight - ((finalVector.getY() * 0.5F + 0.5F) * screenHeight);
        return new Vector2f(screenX, screenY);
    }

    /**
     * 检查一个坐标是否在视野中
     * @param pos 世界坐标
     */
    public static boolean isPosInView(Vector3f pos) {
        Quaternion cameraRotation = info.getRotation().copy();
        cameraRotation.multiply(new Quaternion(Vector3f.YN, 180, true));
        cameraRotation.conjugate();

        return isPosInView(pos, cameraRotation, mc.gameRenderer.getProjectionMatrix(info, mc.getRenderPartialTicks(), true));
    }

    public static boolean isPosInView(Vector3f pos, Quaternion cameraRotation, Matrix4f projection) {
        ClippingHelper clippingHelper = new ClippingHelper(new Matrix4f(cameraRotation), projection);
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

    public static float getTheta(Vector3d viewDirection, Vector3f cameraToPoint) {
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
