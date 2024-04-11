package com.teammoeg.frostedheart.content.tips.client.hud;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.tips.client.TipElement;
import com.teammoeg.frostedheart.content.tips.client.gui.EmptyScreen;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.content.tips.client.util.AnimationUtil;
import com.teammoeg.frostedheart.content.tips.client.util.GuiUtil;
import com.teammoeg.frostedheart.util.client.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

public class TipHUD extends AbstractGui {
    private final Minecraft mc = Minecraft.getInstance();
    private final MatrixStack ms;
    private final TipElement element;
    private final int lineSpace = 12;
    private final boolean alwaysVisible;

    private int descLines = 0;
    private int titleLines = 0;
    private int extendedWidth = 0;
    private int extendedHeight = 0;
    private boolean fadeIn = true;
    private boolean fadeOut = false;
    private boolean alwaysVisibleOverride = false;

    public boolean visible = true;

    public TipHUD(MatrixStack matrixStack, TipElement element) {
        this.ms = matrixStack;
        this.element = element;
        this.alwaysVisible = element.alwaysVisible;
    }

    public void render(boolean isGUI) {
        if (!visible) return;

        float fadeProgress = 1.0F;
        float defaultBGAlpha = isGUI ? 0.75F : 0.3F;
        float BGAlpha = defaultBGAlpha;
        float fontAlpha = 1.0F;

        Point mainWindow = new Point(mc.getMainWindow().getScaledWidth(), mc.getMainWindow().getScaledHeight());
        int x = (int)(mainWindow.getX() * 0.7)-extendedWidth;
        int y = (int)(mainWindow.getY() * 0.3)-extendedHeight;
        Point renderPos2 = new Point((int)(mainWindow.getX() * 0.99F), lineSpace);

        //最大和最小宽度
        if (renderPos2.getX() - x > 360) {
            x = x - (360 - (renderPos2.getX() - x));
        } else if (renderPos2.getX() - x < 160) {
            x = x - (160 - (renderPos2.getX() - x));
        }

        //文本超出窗口时调整尺寸
        if ((descLines + titleLines+1)*lineSpace + y > mainWindow.getY()) {
            if (descLines >= element.contents.size() && y > 40) {
                extendedHeight += 24;

            } else if ((descLines + titleLines+1)*lineSpace + y > mainWindow.getY()) {
                if (descLines >= element.contents.size() && x > mainWindow.getX() * 0.5) {
                    extendedWidth += 24;
                } else {
                    GuiUtil.drawWrapString(I18n.format("tips." + FHMain.MODID + ".too_long"), ms, mc.fontRenderer,
                            8, 8, (int)(mainWindow.getX()*0.5F),
                            element.fontColor, lineSpace, true);
                }
            }
        }

        if (fadeOut) {
            float progress = 1- AnimationUtil.calcFadeIn(400, "TipFadeOut", false);
            fadeProgress = progress;

            if (progress == 0) {
                visible = false;
                return;
            } else {
                BGAlpha = defaultBGAlpha * progress;
                fontAlpha = Math.max(progress, 0.02F);
            }
        } else if (fadeIn) {
            float progress = AnimationUtil.calcFadeIn(400, "TipFadeIn", false);
            fadeProgress = progress;

            if (progress == 1.0F) {
                fadeIn = false;
            } else {
                BGAlpha = defaultBGAlpha * progress;
                fontAlpha = Math.max(progress, 0.02F);
            }
        }

        int BGColor = (int)(BGAlpha * 255.0F) << 24 | element.BGColor & 0x00FFFFFF;
        int fontColor = (int)(fontAlpha * 255.0F) << 24 | element.fontColor & 0x00FFFFFF;
        float yaw = 0;
        float pitch = 0;
        if (mc.player != null) {
            if (mc.isGamePaused()) {
                yaw   = mc.player.getYaw(mc.getRenderPartialTicks()) - mc.player.renderArmYaw;
                pitch = mc.player.getPitch(mc.getRenderPartialTicks()) - mc.player.renderArmPitch;
            } else {
                yaw   = mc.player.getYaw(mc.getRenderPartialTicks())
                        - MathHelper.lerp(mc.getRenderPartialTicks(), mc.player.prevRenderArmYaw, mc.player.renderArmYaw);
                pitch = mc.player.getPitch(mc.getRenderPartialTicks())
                        - MathHelper.lerp(mc.getRenderPartialTicks(), mc.player.prevRenderArmPitch, mc.player.renderArmPitch);
            }
        }

        ms.push();
        ms.translate(-yaw*0.1F + fadeProgress*16 - 16, -pitch*0.1F, 1000);

        renderContent(element.contents, x, y, fontColor, renderPos2, BGColor);

        renderButton(renderPos2.getX() - 13, y-1, fontColor);

        if (!isAlwaysVisible() && fadeProgress == 1.0F) {
            //进度条
            float lineProgress = 1-AnimationUtil.calcProgress(element.visibleTime, "TipVisibleTime", false);
            int lx = x-4;
            int ly = y + (titleLines+1)*lineSpace;
            int x2 = renderPos2.getX() - lx;

            if (lineProgress == 0) {
                fadeOut = true;
            } else {
                ms.push();
                ms.translate(lx, ly, 0);
                ms.scale(lineProgress, 1, 1);
                fill(ms, 0, 0, x2, 1, fontColor);
                ms.pop();
            }
        } else if (isAlwaysVisible() || fadeIn) {
            fill(ms, x - 4, y + (titleLines+1)*lineSpace,
                    renderPos2.getX(), y + (titleLines+1)*lineSpace + 1, fontColor);
        }
        ms.pop();
    }

    private void renderContent(List<ITextComponent> texts, int x, int y, int fontColor, Point renderPos2, int BGColor) {
        int BGPosX = x - 4;
        int width = renderPos2.getX()- BGPosX;
        if (texts.size() > 1) {
            fill(ms, BGPosX, y - 4, renderPos2.getX(),
                    renderPos2.getY()*2 + y + 4 + (descLines -1)*lineSpace, BGColor);
            descLines = 0;
            //标题
            int t = -1;
            t += GuiUtil.formatAndDraw(texts.get(0), ms, mc.fontRenderer, x, y,
                    width-16, fontColor, lineSpace, false);
            descLines += t;
            titleLines = t;
            //内容
            for (int dt = 1; dt < texts.size(); dt++) {
                descLines += GuiUtil.formatAndDraw(texts.get(dt), ms, mc.fontRenderer,
                        x, descLines*lineSpace + y+17,
                        width-8, fontColor, lineSpace, false);
            }
        } else {//只有标题
            fill(ms, BGPosX, y - 4,
                renderPos2.getX(), renderPos2.getY() + y + (descLines)*lineSpace, BGColor);
            descLines = 0;

            int t = -1;
            t += GuiUtil.formatAndDraw(texts.get(0), ms, mc.fontRenderer,
                    x, y, width-16, fontColor, lineSpace, false);
            descLines += t;
            titleLines = t;
        }
    }

    private void renderButton(int x, int y, int color) {
        if (!isFading() && (mc.currentScreen != null || InputMappings.isKeyDown(mc.getMainWindow().getHandle(), 258))) {
            if (mc.currentScreen == null) mc.displayGuiScreen(new EmptyScreen());

            if (GuiUtil.renderIconButton(ms, IconButton.ICON_CROSS, GuiUtil.getMouseX(), GuiUtil.getMouseY(), x, y, color, 0)) {
                if (!isFading()) {
                    fadeOut = true;
                }
            }
            //标题超过 1 行时把锁定按钮从左边移动到下面
            if (titleLines > 1){
                if (!isAlwaysVisible() && GuiUtil.renderIconButton(ms, IconButton.ICON_LOCK, GuiUtil.getMouseX(), GuiUtil.getMouseY(), x, y+10, color, 0))
                    alwaysVisibleOverride = true;
            } else {
                if (!isAlwaysVisible() && GuiUtil.renderIconButton(ms, IconButton.ICON_LOCK, GuiUtil.getMouseX(), GuiUtil.getMouseY(), x-15, y, color, 0))
                    alwaysVisibleOverride = true;
            }
        } else {
            GuiUtil.renderIcon(ms, IconButton.ICON_CROSS, x, y, color);
        }
    }

    public boolean isAlwaysVisible() {
        return alwaysVisible || alwaysVisibleOverride;
    }

    public boolean isFading() {
        return fadeIn || fadeOut;
    }
}
