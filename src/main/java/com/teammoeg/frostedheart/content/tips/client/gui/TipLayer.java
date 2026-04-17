package com.teammoeg.frostedheart.content.tips.client.gui;

import com.teammoeg.chorda.client.AnimationUtil;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.contentpanel.ContentPanel;
import com.teammoeg.chorda.client.cui.contentpanel.FlatIconButton;
import com.teammoeg.chorda.client.cui.contentpanel.ImageLine;
import com.teammoeg.chorda.client.cui.contentpanel.Line;
import com.teammoeg.chorda.client.cui.contentpanel.LineHelper;
import com.teammoeg.chorda.client.cui.screenadapter.OverlayPositioner;
import com.teammoeg.chorda.client.cui.widgets.Button;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipHelper;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class TipLayer extends UILayer {
    boolean isEditing;
    final ContentPanel panel;
    final Button closeBtn;
    final Button pinBtn;

    @NotNull
    Tip tip = Tip.EMPTY;
    Tip.Display display = Tip.Display.DEFAULT;
    Tip lastTip;
    State state = State.IDLE;
    float progress;
    boolean alwaysVisibleOverride;

    public TipLayer(UIElement parent) {
        super(parent);
        this.panel = new ContentPanel(this) {
            @Override
            public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {}

            @Override
            public void refresh() {
                setWidth(parent.getWidth());
                for (UIElement element : elements) {
                    element.refresh();
                }
                alignWidgets();
                setHeight(getContentHeight());
                scrollBar.setPosAndSize(getX() + getWidth()+7, -7, 6, getHeight()+14);
            }

            @Override
            public void alignWidgets() {
                align(3, false);
            }
        };
        this.panel.setScissorEnabled(false);
        this.closeBtn = new FlatIconButton(this, Component.translatable("gui.close"), FlatIcon.CROSS, b ->
                TipOverlay.removeCurrent()
        );
        this.pinBtn = new FlatIconButton(this, Component.translatable("gui.frostedheart.pin"), FlatIcon.PIN, b -> {
            this.alwaysVisibleOverride = true;
            refresh();
        });
        this.setScissorEnabled(false);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        if (!isVisible() || tip == Tip.EMPTY) {
            state = State.IDLE;
            return;
        }

        float anim = switch (state) {
            case FADING_IN -> {
                float f = AnimationUtil.fadeIn(400, "fh_tip_fading", false);
                if (f == 1) {
                    AnimationUtil.remove("fh_tip_fading");
                    state = State.PROGRESSING;
                }
                yield f;
            }
            case FADING_OUT -> {
                float f = 1-AnimationUtil.fadeOut(400, "fh_tip_fading", false);
                if (f == 0) {
                    AnimationUtil.remove("fh_tip_fading");
                    AnimationUtil.remove("fh_tip_progress");
                    setTip(Tip.EMPTY);
                    state = State.IDLE;
                }
                yield f;
            }
            case PROGRESSING -> {
                if (tip.display().alwaysVisible() || alwaysVisibleOverride) {
                    state = State.DONE;
                    yield 1F;
                }
                progress = AnimationUtil.progress(display.displayTime(), "fh_tip_progress", false);
                if (progress == 1) {
                    state = State.FADING_OUT;
                    AnimationUtil.remove("fh_tip_progress");
                }
                yield 1F;
            }
            default -> 1F;
        };
        int offset = switch (FHConfig.CLIENT.tipPosition.get()) {
            case MIDDLE_LEFT, TOP_LEFT, BOTTOM_LEFT -> -16;
            case MIDDLE_RIGHT, TOP_RIGHT, BOTTOM_RIGHT -> 16;
            default -> 0;
        };

        var pose = graphics.pose();
        updateYP();
        setOffsetX((int)yaw);
        setOffsetY((int)pitch);
        pose.pushPose();
        pose.translate((yaw-(int)yaw) + (offset*(1-anim)), pitch-(int)pitch, 800); // FIXME 和 FTB 小地图冲突
        graphics.setColor(1, 1, 1, anim);
        hint.theme(this).drawUIBackground(graphics, x+(int)yaw, y+(int)pitch, w, h);
        for (UIElement ele : panel.getLines())
            if (ele instanceof ImageLine i)
                i.setBlitColor(Colors.setAlpha(-1, anim));
        super.render(graphics, x, y, w, h, hint);
        graphics.setColor(1, 1, 1, 1);
        pose.popPose();
    }

    protected void setTip(@NotNull Tip tip) {
        this.lastTip = this.tip;
        this.tip = tip;
        this.display = tip.display();
        state = State.FADING_IN;
        progress = 0;
        alwaysVisibleOverride = false;
        refresh();
    }

    private void updateYP() {
        var MC = ClientUtils.getMc();
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
        } else {
            yaw = 0;
            pitch = 0;
        }
    }

    float yaw, pitch;
    @Override
    public void refresh() {
        if (tip == Tip.EMPTY) return;
        // 1. 获取初始坐标和最大宽度
        int sw = ClientUtils.screenWidth();
        int sh = ClientUtils.screenHeight();
        int maxWidth = (int)Math.min(sw*0.35F, sh*0.6F);

        // 2. 获取内容最大宽度
        int contentWidth = 0;
        for (String c : tip.contents()) {
            contentWidth = Math.max(contentWidth, getFont().width(Component.translatable(c)));
        }
        if (TipHelper.hasImage(tip)) {
            contentWidth = Math.max(contentWidth, CGuiHelper.getImgSize(tip.image()).width);
        }
        setWidth((int) Mth.clamp(contentWidth, Math.max(sw*0.2F, 120), maxWidth));

        // 3. 刷新
        addUIElements();
        for (UIElement ele : getElements()) {
            ele.refresh();
        }
        alignWidgets();
        setHeight(getContentHeight());

        // 4. 调整位置
        var pos = OverlayPositioner.position(this, FHConfig.CLIENT.tipPosition.get().startPos(this));
        setPos(pos.getX(), pos.getY());
    }

    @Override
    public void addUIElements() {
        clearElement();
        add(panel);
        panel.clearElement();
        if (tip.contents().isEmpty()) return;

        var lines = new ArrayList<Line<?>>();
        List<String> contents = tip.contents();
        for (int i = 0; i < contents.size(); i++) {
            String text = contents.get(i);
            var line = LineHelper.text(panel, Component.translatable(text));
            if (i == 0) {
                if (parent instanceof TipOverlay) {
                    line.button(closeBtn);
                    if (!tip.display().alwaysVisible() && !alwaysVisibleOverride) {
                        line.button(pinBtn);
                    }
                }
                line.button(tip.clickAction());
            }
            lines.add(line);
        }
        lines.add(1, new ProgressBar(panel).height(ProgressBar.DEF_HEIGHT));
        if (!tip.display().displayItems().isEmpty()) {
            lines.add(LineHelper.items(panel, tip.display().displayItems()));
        }
        if (TipHelper.hasImage(tip)) {
            lines.add(LineHelper.img(panel, tip.image()));
        }
        panel.fillContent(lines);
    }

    @Override
    public void alignWidgets() {
        align(false);
    }

    @Override
    public boolean isVisible() {
        return isEnabled();
    }

    @Override
    public boolean isEnabled() {
        return state != State.IDLE;
    }

    class ProgressBar extends Line<ProgressBar> {
        public static final int DEF_HEIGHT = 0;

        public ProgressBar(UIElement parent) {
            super(parent);
            setScissorEnabled(false);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(x-4, y-1, 0);
            if (!(display.alwaysVisible() || alwaysVisibleOverride)) {
                pose.scale(1-progress, 1, 1);
            }
            graphics.fill(0, 0, w+8, 1, display.fontColor());
            pose.popPose();
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
         * 动画播放完毕
         */
        DONE,
        /**
         * 无 tip
         */
        IDLE
    }
}
