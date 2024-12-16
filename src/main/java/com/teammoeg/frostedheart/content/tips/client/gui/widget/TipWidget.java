package com.teammoeg.frostedheart.content.tips.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.util.client.AnimationUtil;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import com.teammoeg.frostedheart.util.client.FHGuiHelper;
import com.teammoeg.frostedheart.util.lang.Lang;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class TipWidget extends AbstractWidget {
    private static final String ANIMATION_PROGRESS_NAME = TipWidget.class.getSimpleName() + "_progress";
    private static final String ANIMATION_FADE_NAME = TipWidget.class.getSimpleName() + "_fade";
    private static final int FADE_ANIMATION_TIME_LENGTH = 400;
    private static final int BACKGROUND_BORDER = 4;
    private static final int FADE_OFFSET = 16;
    private static final int LINE_SPACE = 12;
    private static final int MIN_WIDTH = 160;
    private static final int MAX_WIDTH = 360;
    public final IconButton closeButton;
    public final IconButton pinButton;
    public Tip lastTip;
    @Setter
    @Getter
    @Nullable
    private Tip tip;
    @Getter
    @Setter
    private State state;
    private boolean alwaysVisibleOverride;
    @Getter
    private float progress;

    public TipWidget() {
        super(ClientUtils.screenWidth(), ClientUtils.screenHeight(), 0, 0, Component.literal("tip"));
        this.closeButton = new IconButton(0, 0, IconButton.Icon.CROSS, FHColorHelper.CYAN, Lang.gui("close").component(),
                b -> state = State.FADING_OUT
        );
        this.pinButton = new IconButton(0, 0, IconButton.Icon.LOCK, FHColorHelper.CYAN, Lang.gui("pin").component(),
                b -> this.alwaysVisibleOverride = true
        );
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
                float f = AnimationUtil.fadeIn(FADE_ANIMATION_TIME_LENGTH, ANIMATION_FADE_NAME, false);
                render(graphics, mouseX, mouseY, partialTick, f);
                if (f == 1F) {
                    state = isAlwaysVisible() ? State.DONE : State.PROGRESSING;
                    AnimationUtil.remove(ANIMATION_FADE_NAME);
                }
            }
            case PROGRESSING -> {
                if (isAlwaysVisible()) state = State.DONE;
                progress = AnimationUtil.progress(tip.getDisplayTime(), ANIMATION_PROGRESS_NAME, false);
                render(graphics, mouseX, mouseY, partialTick, 1);
                if (progress == 1F) state = State.FADING_OUT;
            }
            case FADING_OUT -> {
                float f = 1F - AnimationUtil.fadeIn(FADE_ANIMATION_TIME_LENGTH, ANIMATION_FADE_NAME, false);
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
        var contents = tip.getContents();
        int fontColor = FHColorHelper.setAlpha(tip.getFontColor(), Math.max(progress, 0.1F));
        int backgroundColor = FHColorHelper.setAlpha(tip.getBackgroundColor(), (isGuiOpened() ? 0.8F : 0.5F) * progress);
        int screenW = ClientUtils.screenWidth();
        int screenH = ClientUtils.screenHeight();
        setWidth(Mth.clamp((int)(screenW * 0.3F), MIN_WIDTH, MAX_WIDTH));

        // 文本
        var titleLines = ClientUtils.font().split(contents.get(0), super.getWidth()-24);
        var contentLines = new ArrayList<FormattedCharSequence>();
        if (contents.size() > 1)
            for (int i = 1; i < contents.size(); i++)
                contentLines.addAll(ClientUtils.font().split(contents.get(i), super.getWidth()-24));
        setHeight((titleLines.size() + contentLines.size()) * LINE_SPACE);

        // 跟随视角晃动
        float yaw = 0;
        float pitch = 0;
        Minecraft MC = ClientUtils.mc();
        if (MC.player != null) {
            if (MC.isPaused()) {
                yaw   = (MC.player.getViewYRot(MC.getFrameTime()) - MC.player.yBob) * -0.1F;
                pitch = (MC.player.getViewXRot(MC.getFrameTime()) - MC.player.xBob) * -0.1F;
            } else {
                yaw   = (MC.player.getViewYRot(MC.getFrameTime())
                        - Mth.lerp(MC.getFrameTime(), MC.player.yBobO, MC.player.yBob)) * -0.1F;
                pitch = (MC.player.getViewXRot(MC.getFrameTime())
                        - Mth.lerp(MC.getFrameTime(), MC.player.xBobO, MC.player.xBob)) * -0.1F;
            }
        }

        // 渲染
        setPosition((screenW - width - 8 + (int)yaw), ((int)(screenH * 0.3F) + (int)pitch));
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate((yaw - (int)yaw), (pitch - (int)pitch), 800);

        // 背景
        graphics.fill(getX()-BACKGROUND_BORDER, getY()-BACKGROUND_BORDER, getX()+width+BACKGROUND_BORDER, getY()+getHeight() + (contentLines.isEmpty() ? 1 : 6), backgroundColor);

        // 进度条
        if (state != State.FADING_OUT) {
            int y = getY() + (titleLines.size() * LINE_SPACE);
            pose.pushPose();
            pose.translate(getX()-BACKGROUND_BORDER, y, 0);
            if (state == State.PROGRESSING) {
                // 更平滑的进度条效果
                pose.scale(1 - AnimationUtil.getProgress(ANIMATION_PROGRESS_NAME), 1, 1);
            }
            graphics.fill(0, 0, super.getWidth()+(BACKGROUND_BORDER*2), 1, fontColor);
            pose.popPose();
        }

        // 标题和内容
        FHGuiHelper.drawStrings(graphics, ClientUtils.font(), titleLines, getX(), getY(), fontColor, LINE_SPACE, false);
        FHGuiHelper.drawStrings(graphics, ClientUtils.font(), contentLines, getX(), getY()+6 + (titleLines.size() * LINE_SPACE), fontColor, LINE_SPACE, false);

        // 按钮
        pose.translate(0, 0, 100);
        closeButton.color = fontColor;
        closeButton.visible = true;
        closeButton.setPosition(getX() + super.getWidth() - 10, getY());
        closeButton.render(graphics, mouseX, mouseY, partialTick);
        if (isGuiOpened() && !isAlwaysVisible() && state != State.FADING_OUT) {
            pinButton.color = fontColor;
            pinButton.visible = true;
            pinButton.setPosition(getX() + super.getWidth() - 22, getY());
            pinButton.render(graphics, mouseX, mouseY, partialTick);
        } else {
            pinButton.visible = false;
        }
        pose.popPose();
    }

    /**
     * 当前是否有打开 Gui
     */
    public boolean isGuiOpened() {
        return ClientUtils.mc().screen != null;
    }

    /**
     * 重置状态
     */
    public void resetState() {
        lastTip = tip;
        tip = null;
        progress = 0;
        state = State.IDLE;
        alwaysVisibleOverride = false;
        pinButton.visible = false;
        closeButton.visible = false;
        pinButton.setFocused(false);
        closeButton.setFocused(false);
        // 将位置设置到屏幕外避免影响屏幕内的元素
        setPosition(ClientUtils.screenWidth(), ClientUtils.screenHeight());
        AnimationUtil.remove(ANIMATION_FADE_NAME);
        AnimationUtil.remove(ANIMATION_PROGRESS_NAME);
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
            return super.getX() - FADE_OFFSET + (int)(FADE_OFFSET * AnimationUtil.getFadeIn(ANIMATION_FADE_NAME));
        if (state == State.FADING_OUT)
            return super.getX() - (int)(FADE_OFFSET * AnimationUtil.getFadeOut(ANIMATION_FADE_NAME));

        return super.getX();
    }

    @Override
    public int getWidth() {
        if (state == State.FADING_IN)
            return super.getWidth() + FADE_OFFSET - (int)(FADE_OFFSET * AnimationUtil.getFadeIn(ANIMATION_FADE_NAME));
        if (state == State.FADING_OUT)
            return super.getWidth() + (int)(FADE_OFFSET * AnimationUtil.getFadeOut(ANIMATION_FADE_NAME));

        return super.getWidth();
    }

    @Override
    protected boolean isValidClickButton(int pButton) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
    }

    @Override
    public void playDownSound(@NotNull SoundManager pHandler) {
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
