package com.teammoeg.frostedheart.content.ui.dialogue;

import com.teammoeg.chorda.client.AnimationUtil;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.TesselateHelper;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.screenadapter.OverlayPositioner;
import com.teammoeg.chorda.client.cui.widgets.Button;
import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.math.Colors;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DialogueOverlay extends PrimaryLayer {
    public static final DialogueOverlay INSTANCE = new DialogueOverlay();
    private DialogueOverlay() {
        setRenderGradient(false);
        setEnabled(false);
    }

    private final List<Selection> selections = new ArrayList<>();
    private final Button close = TextButton.create(this, Component.translatable("gui.close"), FlatIcon.LEAVE.toCIcon(), mb -> {
        close();
        ClientUtils.getMc().popGuiLayer();
    });

    public boolean closeable;

    public void open(boolean closeable, Collection<Button> buttons) {
        // 新对话开始：清掉旧对话的选项，避免关闭后 200ms 淡出窗口内的"幽灵选项"仍可点击
        for (Selection s : this.selections) {
            elements.remove(s);
            s.animation.close();
        }
        this.selections.clear();
        // 注意：不能在这里清空 closeCallback——它由上一次 openScreen 在调用 open() 前设置，
        // 同栈帧内清空会使关闭时的监听器清理永不触发（泄漏）；close() 触发后自清空，每次 openScreen 也覆盖新值
        addSelections(buttons);
        this.closeable = closeable;
        if (closeable) {
            addSelectionInternal(close);
        }
        refresh();
        setEnabled(true);
    }

    public void close() {
        for (Selection selection : selections) {
            selection.animation.fadeOut();
        }
        setEnabled(false);
        // 触发并清空：任何方式关闭（右键/关闭按钮/点击选项）都执行一次清理回调
        if (closeCallback != null) {
            Runnable cb = closeCallback;
            closeCallback = null;
            cb.run();
        }
    }

    /**
     * 对话关闭时的清理回调（只触发一次，触发后自动清空）。
     */
    @Nullable
    public static Runnable closeCallback;

    @Override
    public void tick() {
        selections.removeIf((s) -> {
            boolean remove = s.animation.state == FadeAnimationController.State.DONE;
            if (remove) {
                elements.remove(s);
                refresh();
            }
            return remove;
        });
    }

    public void addSelection(Button button) {
        addSelectionInternal(button).setX(getContentX()+18);
    }

    public void addSelections(Collection<Button> buttons) {
        for (Button button : buttons) {
            addSelectionInternal(button);
        }
    }

    private Selection addSelectionInternal(Button button) {
        var selection = new Selection(button);
        if (add(selection))
            selections.add(selection);
        return selection;
    }

    @Override
    public void refresh() {
        for (UIElement ele : elements) {
            ele.refresh();
        }
        alignWidgets();

        var pos = OverlayPositioner.position(this, OverlayPositioner.All.MIDDLE.startPos(this));
        setOffsetX(pos.getX()+110);
        setOffsetY(pos.getY()+6);
    }

    @Override
    public void alignWidgets() {
        align(0, false);
        super.alignWidgets();
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (closeable && button.is(MouseButton.RIGHT)) {
            close.onClicked(button);
            return true;
        }
        return super.onMousePressed(button);
    }

    @Override
    public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!selections.isEmpty()) {
            int index = CInputHelper.mapNumber(keyCode, -1);
            if (index >= 0 && index < selections.size()) {
                var selection = selections.get(index);
                if (selection.isEnabled()) {
                    selection.onClicked(MouseButton.LEFT);
                    return true;
                }
            }
        }
        return super.onKeyPressed(keyCode, scanCode, modifiers);
    }

    public class Selection extends Button {
        final Button original;
        final FadeAnimationController animation = new FadeAnimationController("DialogueSelection" + hashCode(), 200);
        float hover = 0;

        public Selection(Button original) {
            super(DialogueOverlay.this, original.getTitle(), original.getIcon());
            this.original = original;
            setSize(175, 17);
        }

        @Override
        public boolean hasTooltip() {
            return isMouseOver() && isVisible();
        }

        @Override
        public void getTooltip(TooltipBuilder tooltip) {
            original.getTooltip(tooltip);
        }

        @Override
        public void onClicked(MouseButton button) {
            original.onClicked(button);
            ClientUtils.getMc().popGuiLayer();
        }

        @Override
        public boolean isEnabled() {
            return original.isEnabled();
        }

        @Override
        public boolean isVisible() {
            return original.isVisible();
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
            if (!isVisible()) return;
            if (isEnabled()) {
                float change = 10F / ClientUtils.getMc().getFps();
                hover = Mth.clamp(hover + (isMouseOver ? change : -change), 0, 1.3F);
            }

            // 到动画实际显示前的延迟
            float delay = 0.3F;
            float h2 = hover < delay ? 0 : hover-delay;

            graphics.pose().pushPose();
            graphics.pose().translate(16*(1-animation.progress+(h2*h2*0.5F)), 0, 0);
            graphics.setColor(1, 1, 1, animation.progress);

            animation.update();
            int color = Colors.blend(Colors.themeColor(), Colors.setAlpha(Colors.BLACK, 0.5F), h2*h2*0.5F);
            TesselateHelper.getShapeTesslator()
                    .fillRect(graphics.pose().last().pose(), x, y+1, x+100, y+h-1, color)
                    .fillGradient(graphics.pose().last().pose(), x+100, y+1, x+150, y+h-1, color, Colors.setAlpha(color, 0))
                    .close();
            if (hasIcon() && animation.progress > 0.5F)
                icon.draw(graphics, x + 4, y + 3, 10, 10);
            if (!isEnabled())
                graphics.setColor(1, 1, 1, 0.35F);
            graphics.drawString(getFont(), getTitle(), x+18, y+4, Colors.setAlpha(Colors.themeColor(), Math.max(animation.progress, 0.05F)));

            graphics.setColor(1, 1, 1, 1);
            graphics.pose().popPose();
        }

        @Override
        public void onClosed() {
            super.onClosed();
            animation.close();
        }
    }

    @Getter
    public static class FadeAnimationController {
        private final String name;
        private final int fadeTime;

        protected float progress = 0F;
        protected State state = State.FADING_IN;

        public FadeAnimationController(String name, int fadeTime) {
            this.name = name;
            this.fadeTime = fadeTime;
        }

        public void update() {
            switch (state) {
                case FADING_IN -> {
                    float f = AnimationUtil.fadeIn(fadeTime, name, false);
                    if (f == 1) {
                        state = State.IDLE;
                        AnimationUtil.remove(name);
                    }
                    progress = f;
                }
                case FADING_OUT -> {
                    float f = AnimationUtil.fadeIn(fadeTime, name, false);
                    if (f == 1) {
                        state = State.DONE;
                        AnimationUtil.remove(name);
                    }
                    progress = 1 - f;
                }
                case DONE -> AnimationUtil.remove(name);
                default -> {}
            }
        }

        public void fadeOut() {
            state = State.FADING_OUT;
        }

        public void close() {
            AnimationUtil.remove(name);
        }

        public enum State {
            /**
             * 正在播放淡入动画
             */
            FADING_IN,
            /**
             * FADING_IN 动画播放完毕，等待播放 FADING_OUT
             */
            IDLE,
            /**
             * 正在播放淡出动画
             */
            FADING_OUT,
            /**
             * FADING_OUT 动画播放完毕
             */
            DONE
        }
    }
}
