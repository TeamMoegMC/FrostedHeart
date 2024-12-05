package com.teammoeg.frostedheart.content.tips.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.tips.client.TipElement;
import com.teammoeg.frostedheart.content.tips.client.gui.EmptyScreen;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.frostedheart.util.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public class TipHUD {
    private final Minecraft MC = Minecraft.getInstance();
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

    public TipHUD(TipElement element) {
        resetAnimation();
        this.element = element;
        this.alwaysVisible = element.alwaysVisible;
    }

    public void render(GuiGraphics graphics, boolean isGUI) {
        if (!visible) return;

        float fadeProgress = 1.0F;
        float defaultBGAlpha = isGUI ? 0.75F : 0.3F;
        float BGAlpha = defaultBGAlpha;
        float fontAlpha = 1.0F;

        Point mainWindow = new Point(MC.getWindow().getGuiScaledWidth(), MC.getWindow().getGuiScaledHeight());
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
                    FHGuiHelper.drawSplitTexts(graphics, Lang.translate("tips." + FHMain.MODID + ".too_long"),
                            8, 8, element.fontColor, (int)(mainWindow.getX()*0.5F), lineSpace, true);
                }
            }
        }

        if (fadeOut) {
            float progress = 1- AnimationUtil.fadeIn(400, "TipFadeOut", false);
            fadeProgress = progress;

            if (progress == 0) {
                visible = false;
                return;
            } else {
                BGAlpha = defaultBGAlpha * progress;
                fontAlpha = Math.max(progress, 0.02F);
            }
        } else if (fadeIn) {
            float progress = AnimationUtil.fadeIn(400, "TipFadeIn", false);
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
        if (MC.player != null) {
            if (MC.isPaused()) {
                yaw   = MC.player.getViewYRot(MC.getFrameTime()) - MC.player.yBob;
                pitch = MC.player.getViewXRot(MC.getFrameTime()) - MC.player.xBob;
            } else {
                yaw   = MC.player.getViewYRot(MC.getFrameTime())
                        - Mth.lerp(MC.getFrameTime(), MC.player.yBobO, MC.player.yBob);
                pitch = MC.player.getViewXRot(MC.getFrameTime())
                        - Mth.lerp(MC.getFrameTime(), MC.player.xBobO, MC.player.xBob);
            }
        }

        graphics.pose().pushPose();
        graphics.pose().translate(-yaw*0.05F + fadeProgress*16 - 16, -pitch*0.05F, 1000);

        renderContent(graphics, element.contents, x, y, fontColor, renderPos2, BGColor);

        renderButton(graphics, renderPos2.getX() - 13, y-1, fontColor);

        if (!isAlwaysVisible() && fadeProgress == 1.0F) {
            //进度条
            float lineProgress = 1-AnimationUtil.progress(element.visibleTime, "TipVisibleTime", false);
            int lx = x-4;
            int ly = y + (titleLines+1)*lineSpace;
            int x2 = renderPos2.getX() - lx;

            if (lineProgress == 0) {
                fadeOut = true;
            } else {
                graphics.pose().pushPose();
                graphics.pose().translate(lx, ly, 0);
                graphics.pose().scale(lineProgress, 1, 1);
                graphics.fill(0, 0, x2, 1, fontColor);
                graphics.pose().popPose();
            }
        } else if (isAlwaysVisible() || fadeIn) {
            graphics.fill( x - 4, y + (titleLines+1)*lineSpace,
                    renderPos2.getX(), y + (titleLines+1)*lineSpace + 1, fontColor);
        }
        graphics.pose().popPose();
    }

    private void renderContent(GuiGraphics graphics, List<Component> texts, int x, int y, int fontColor, Point renderPos2, int BGColor) {
        int BGPosX = x - 4;
        int width = renderPos2.getX()- BGPosX;
        if (texts.size() > 1) {
            graphics.fill(BGPosX, y - 4, renderPos2.getX(),
                    renderPos2.getY()*2 + y + 4 + (descLines -1)*lineSpace, BGColor);
            descLines = 0;
            //标题
            int t = -1;
            t += FHGuiHelper.drawSplitTexts(graphics, texts.get(0), x, y, fontColor, width-16, lineSpace, false);
            descLines += t;
            titleLines = t;
            //内容
            for (int dt = 1; dt < texts.size(); dt++) {
                descLines += FHGuiHelper.drawSplitTexts(graphics, texts.get(dt),
                        x, descLines*lineSpace + y+17, fontColor, width-8, lineSpace, false);
            }
        } else {//只有标题
            graphics.fill( BGPosX, y - 4,
                renderPos2.getX(), renderPos2.getY() + y + (descLines)*lineSpace, BGColor);
            descLines = 0;

            int t = -1;
            t += FHGuiHelper.drawSplitTexts(graphics, texts.get(0), x, y, fontColor, width-16, lineSpace, false);
            descLines += t;
            titleLines = t;
        }
    }

    private void renderButton(GuiGraphics graphics, int x, int y, int color) {
        //TODO 按键绑定
        if (!isFading() && (MC.screen != null || InputConstants.isKeyDown(MC.getWindow().getWindow(), 258))) {
            if (MC.screen == null)
                MC.setScreen(new EmptyScreen());

            if (IconButton.renderIconButton(graphics, IconButton.Icon.CROSS, RawMouseHelper.getScaledX(), RawMouseHelper.getScaledY(), x, y, color, 0)) {
                fadeOut = true;
            }
            //标题超过 1 行时把锁定按钮从左边移动到下面
            if (titleLines > 1){
                if (!isAlwaysVisible() && IconButton.renderIconButton(graphics, IconButton.Icon.LOCK, RawMouseHelper.getScaledX(), RawMouseHelper.getScaledY(), x, y+10, color, 0))
                    alwaysVisibleOverride = true;
            } else {
                if (!isAlwaysVisible() && IconButton.renderIconButton(graphics, IconButton.Icon.LOCK, RawMouseHelper.getScaledX(), RawMouseHelper.getScaledY(), x-15, y, color, 0))
                    alwaysVisibleOverride = true;
            }
        } else {
            IconButton.renderIcon(graphics.pose(), IconButton.Icon.CROSS, x, y, color);
        }
    }

    public boolean isAlwaysVisible() {
        return alwaysVisible || alwaysVisibleOverride;
    }

    public boolean isFading() {
        return fadeIn || fadeOut;
    }

    public static void resetAnimation() {
        AnimationUtil.remove("TipFadeIn");
        AnimationUtil.remove("TipFadeOut");
        AnimationUtil.remove("TipVisibleTime");
    }
}
