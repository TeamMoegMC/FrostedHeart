/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.tips.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.AnimationUtil;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.widget.IconButton;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.util.Lang;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.common.util.Size2i;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TipWidget extends AbstractWidget {
    public final IconButton closeButton;
    public final IconButton pinButton;
    public Tip lastTip;
    @Nullable
    public Tip tip;
    @Getter
    private State state;
    @Getter
    private float progress;
    private RenderContext context;
    private boolean alwaysVisibleOverride;

    /**
     * TipWidget实例
     */
    public static final TipWidget INSTANCE = new TipWidget();

    private TipWidget() {
        super(0, 0, 0, 0, Component.literal("tip"));
        this.closeButton = new IconButton(0, 0, IconButton.Icon.CROSS, ColorHelper.CYAN, Lang.gui("close").component(),
                b -> {
                    close();
                    b.setFocused(false);
                });
        this.pinButton = new IconButton(0, 0, IconButton.Icon.LOCK, ColorHelper.CYAN, Lang.gui("pin").component(),
                b -> {
                    this.alwaysVisibleOverride = true;
                    b.setFocused(false);
                });
        this.visible = false;
        this.closeButton.visible = false;
        this.pinButton.visible = false;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (tip == null) {
            state = State.IDLE;
            return;
        }
        switch (state) {
            case IDLE -> state = State.FADING_IN;
            case FADING_IN -> {
                float f = AnimationUtil.fadeIn(RenderContext.FADE_ANIM_LENGTH, RenderContext.FADE_ANIM_NAME, false);
                render(graphics, mouseX, mouseY, partialTick, f);
                if (f == 1F) {
                    pinButton.setAlpha(1F);
                    closeButton.setAlpha(1F);
                    state = isAlwaysVisible() ? State.DONE : State.PROGRESSING;
                    AnimationUtil.remove(RenderContext.FADE_ANIM_NAME);
                }
            }
            case PROGRESSING -> {
                if (isAlwaysVisible()) state = State.DONE;
                progress = AnimationUtil.progress(tip.getDisplayTime(), RenderContext.PROGRESS_ANIM_NAME, false);
                render(graphics, mouseX, mouseY, partialTick, 1);
                if (progress == 1F) state = State.FADING_OUT;
            }
            case FADING_OUT -> {
                float f = 1F - AnimationUtil.fadeIn(RenderContext.FADE_ANIM_LENGTH, RenderContext.FADE_ANIM_NAME, false);
                render(graphics, mouseX, mouseY, partialTick, f);
                if (f == 0F) resetState();
            }
            case DONE -> {
                progress = 1F;
                render(graphics, mouseX, mouseY, partialTick, progress);
            }
            default -> state = State.IDLE;
        }
    }

    private void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, float progress) {
        if (tip == null || tip.getContents().isEmpty()) return;

        if (context == null) context = new RenderContext(tip);
        context.update();
        setWidth(context.size.width);
        setHeight(context.size.height);
        setX(context.screenSize.width - super.getWidth() - 4 - RenderContext.BG_BORDER + (int) context.pYaw);
        setY((int)(context.screenSize.height * 0.3F) + (int) context.pPitch);
        setAlpha(progress);
        graphics.setColor(1, 1, 1, alpha);

        int border = RenderContext.BG_BORDER;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate((context.pYaw - (int) context.pYaw), (context.pPitch - (int) context.pPitch), 800);

        // 背景
        graphics.fill(
                getX() - border,
                getY() - border,
                getX() + super.getWidth() + border,
                getY() + getHeight() + (!context.contentLines.isEmpty() || context.hasImage ? 2 + border : 1),
                context.BGColor);

        // 进度条
        if (state != State.FADING_OUT) {
            int y = getY() + (context.titleLines.size() * RenderContext.LINE_SPACE);
            pose.pushPose();
            pose.translate(getX()- border, y, 0);
            if (state == State.PROGRESSING) {
                // 更平滑的进度条效果
                pose.scale(1 - AnimationUtil.getProgress(RenderContext.PROGRESS_ANIM_NAME), 1, 1);
            }
            graphics.fill(0, 0, super.getWidth()+(border *2), 1, context.fontColor);
            pose.popPose();
        }

        // 图片
        if (context.hasImage) {
            CGuiHelper.bindTexture(tip.getImage());
            CGuiHelper.blitColored(
                    pose,
                    getX() + (super.getWidth() / 2) - (context.imageSize.width / 2),
                    getY() + border + (context.totalLineSize * RenderContext.LINE_SPACE),
                    context.imageSize.width, context.imageSize.height,
                    0, 0,
                    context.imageSize.width, context.imageSize.height,
                    context.imageSize.width, context.imageSize.height,
                    0xFFFFFFFF, alpha);
        }

        // 按钮
        pose.translate(0, 0, 100);
        closeButton.color = context.fontColor;
        closeButton.visible = true;
        closeButton.setPosition(getX() + super.getWidth() - 10, getY());
        closeButton.render(graphics, mouseX, mouseY, partialTick);
        if (isGuiOpened() && !isAlwaysVisible() && state != State.FADING_OUT) {
            pinButton.color = context.fontColor;
            pinButton.visible = true;
            pinButton.setPosition(getX() + super.getWidth() - 22, getY());
            pinButton.render(graphics, mouseX, mouseY, partialTick);
        } else {
            pinButton.visible = false;
        }

        // 标题和内容
        CGuiHelper.drawStrings(graphics, ClientUtils.font(), context.titleLines, getX(), getY(), context.fontColor, RenderContext.LINE_SPACE, false, false);
        CGuiHelper.drawStrings(graphics, ClientUtils.font(), context.contentLines, getX(), getY()+6 + (context.titleLines.size() * RenderContext.LINE_SPACE), context.fontColor, RenderContext.LINE_SPACE, false, false);

        pose.popPose();
        graphics.setColor(1, 1, 1, 1);
    }

    /**
     * 当前是否有打开 Gui
     */
    public boolean isGuiOpened() {
        return ClientUtils.mc().screen != null;
    }

    public void close() {
        state = State.FADING_OUT;
    }

    /**
     * 重置状态
     */
    public void resetState() {
        lastTip = tip;
        tip = null;
        progress = 0;
        context = null;
        state = State.IDLE;
        alwaysVisibleOverride = false;
        visible = false;
        pinButton.visible = false;
        closeButton.visible = false;
        setFocused(false);
        pinButton.setFocused(false);
        closeButton.setFocused(false);
        pinButton.setAlpha(1F);
        closeButton.setAlpha(1F);
        AnimationUtil.remove(RenderContext.FADE_ANIM_NAME);
        AnimationUtil.remove(RenderContext.PROGRESS_ANIM_NAME);
        // 将位置设置到屏幕外避免影响屏幕内的元素
        setPosition(ClientUtils.screenWidth(), ClientUtils.screenHeight());
    }

    /**
     * 是否始终显示
     */
    public boolean isAlwaysVisible() {
        return alwaysVisibleOverride || (tip != null && tip.isAlwaysVisible());
    }

    public Rect2i getRect() {
        return new Rect2i(getX(), getY(), getWidth(), getHeight());
    }

    @Override
    public int getX() {
        if (state == State.FADING_IN)
            return super.getX() - RenderContext.FADE_OFFSET + (int)(RenderContext.FADE_OFFSET * AnimationUtil.getFadeIn(RenderContext.FADE_ANIM_NAME));
        if (state == State.FADING_OUT)
            return super.getX() - (int)(RenderContext.FADE_OFFSET * AnimationUtil.getFadeOut(RenderContext.FADE_ANIM_NAME));

        return super.getX();
    }

    @Override
    public int getWidth() {
        if (state == State.FADING_IN)
            return super.getWidth() + RenderContext.FADE_OFFSET - (int)(RenderContext.FADE_OFFSET * AnimationUtil.getFadeIn(RenderContext.FADE_ANIM_NAME));
        if (state == State.FADING_OUT)
            return super.getWidth() + (int)(RenderContext.FADE_OFFSET * AnimationUtil.getFadeOut(RenderContext.FADE_ANIM_NAME));

        return super.getWidth();
    }

    @Override
    protected boolean isValidClickButton(int pButton) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
    }

    private class RenderContext {
        static final String FADE_ANIM_NAME = TipWidget.class.getSimpleName() + "_fade";
        static final String PROGRESS_ANIM_NAME = TipWidget.class.getSimpleName() + "_progress";
        static final int FADE_ANIM_LENGTH = 400;
        static final int BG_BORDER = 4;
        static final int FADE_OFFSET = 16;
        static final int LINE_SPACE = 12;
        static final int MIN_WIDTH = 160;
        static final int MAX_WIDTH = 360;

        final Tip tip;
        final Size2i originalImageSize;

        List<FormattedCharSequence> titleLines;
        List<FormattedCharSequence> contentLines;
        Size2i imageSize;
        Size2i screenSize;
        Size2i size;
        boolean hasImage;
        int totalLineSize;
        int BGColor;
        int fontColor;
        float pYaw;
        float pPitch;

        RenderContext(Tip tip) {
            this.tip = tip;
            originalImageSize = getImgSize(tip.getImage());
        }

        void update() {
            BGColor = ColorHelper.setAlpha(tip.getBackgroundColor(), (isGuiOpened() ? 0.8F : 0.5F));
            fontColor = ColorHelper.setAlpha(tip.getFontColor(), 1.0F);

            // 跟随视角晃动
            Minecraft MC = ClientUtils.mc();
            if (MC.player != null) {
                if (MC.isPaused()) {
                    pYaw   = (MC.player.getViewYRot(MC.getFrameTime()) - MC.player.yBob) * -0.1F;
                    pPitch = (MC.player.getViewXRot(MC.getFrameTime()) - MC.player.xBob) * -0.1F;
                } else {
                    pYaw   = (MC.player.getViewYRot(MC.getFrameTime())
                            - Mth.lerp(MC.getFrameTime(), MC.player.yBobO, MC.player.yBob)) * -0.1F;
                    pPitch = (MC.player.getViewXRot(MC.getFrameTime())
                            - Mth.lerp(MC.getFrameTime(), MC.player.xBobO, MC.player.xBob)) * -0.1F;
                }
            } else {
                pYaw = 0;
                pPitch = 0;
            }

            // 检查屏幕尺寸是否更新
            Size2i newSize = new Size2i(ClientUtils.screenWidth(), ClientUtils.screenHeight());
            if (this.screenSize != null && this.screenSize.equals(newSize)) return;
            this.screenSize = newSize;
            int width = (int)Mth.clamp(screenSize.width * 0.3F, MIN_WIDTH, MAX_WIDTH);

            // 文本换行
            var contents = tip.getContents();
            titleLines = ClientUtils.font().split(contents.get(0), width);
            contentLines = new ArrayList<>();
            if (contents.size() > 1)
                for (int i = 1; i < contents.size(); i++)
                    contentLines.addAll(ClientUtils.font().split(contents.get(i), width));
            totalLineSize = titleLines.size() + contentLines.size();
            int height = (totalLineSize * LINE_SPACE);

            // 图片
            int imgW = 0;
            int imgH = 0;
            if (hasImage) {
                imgW = originalImageSize.width;
                imgH = originalImageSize.height;
                // 缩放图片以适应屏幕
                if (Math.abs(imgW - imgH) < 8 && imgW <= 32) {
                    imgW = imgW * (32 / imgW);
                    imgH = imgH * (32 / imgH);
                }
                if (imgW > width) {
                    float scale = (float)width / imgW;
                    imgH = (int) (imgH * scale);
                    imgW = (int) (imgW * scale);
                }
                if (screenSize.height * 0.3F + height + imgH > screenSize.height) {
                    float availableHeight = screenSize.height - screenSize.height * 0.3F - height - 8;
                    if (availableHeight > 0) {
                        float scale = availableHeight / imgH;
                        imgH = (int) (imgH * scale);
                        imgW = (int) (imgW * scale);
                    }
                }
            }

            imageSize = new Size2i(imgW, imgH);
            size = new Size2i(width, height + imageSize.height);
        }

        Size2i getImgSize(ResourceLocation location) {
            if (location != null) {
                var resource = ClientUtils.mc().getResourceManager().getResource(location);
                if (resource.isPresent()) {
                    try (InputStream stream = resource.get().open()) {
                        BufferedImage image= ImageIO.read(stream);
                        hasImage = true;
                        return new Size2i(image.getWidth(), image.getHeight());
                    } catch (IOException e) {
                    }
                }
            }

            hasImage = false;
            return new Size2i(0, 0);
        }
    }

    public enum State {
        /**
         * 正在播放淡入动画
         */
        FADING_IN,
        /**
         * 正在播放淡出动画
         */
        FADING_OUT,
        /**
         * 正在走显示时间进度条
         */
        PROGRESSING,
        /**
         * {@link #FADING_IN} 动画播放完毕且 {@link #isAlwaysVisible()} 为 {@code true}
         */
        DONE,
        /**
         * 无 tip
         */
        IDLE
    }
}
