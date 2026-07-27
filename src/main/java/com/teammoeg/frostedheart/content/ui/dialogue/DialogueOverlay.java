package com.teammoeg.frostedheart.content.ui.dialogue;

import com.teammoeg.chorda.client.AnimationUtil;
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DialogueOverlay extends PrimaryLayer {
    public static final DialogueOverlay INSTANCE = new DialogueOverlay();
    private DialogueOverlay() {
        setRenderGradient(false);
        setEnabled(false);
    }

    private final Set<Selection> selections = new HashSet<>();
    private final Button close = TextButton.create(this, Component.translatable("gui.close"), FlatIcon.LEAVE.toCIcon(), mb -> {
        close();
        ClientUtils.getMc().popGuiLayer();
    });

    public boolean closeable;

    public void open(boolean closeable, Collection<Button> buttons) {
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
    }

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

        var pos = OverlayPositioner.position(this, OverlayPositioner.LeftAndRight.RIGHT.startPos(this));
        setOffsetX(pos.getX());
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

    public class Selection extends Button {
        final Button original;
        final FadeAnimationController animation = new FadeAnimationController("DialogueSelection" + hashCode(), 200);
        float hover = 0;

        public Selection(Button original) {
            super(DialogueOverlay.this, original.getTitle(), original.getIcon());
            this.original = original;
            setSize(175, 18);
        }

        @Override
        public boolean hasTooltip() {
            return original.hasTooltip();
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
            float change = 10F / ClientUtils.getMc().getFps();
            if (isEnabled())
                hover = Mth.clamp(hover + (isMouseOver ? change : -change), 0, 1);

            graphics.pose().pushPose();
            graphics.pose().translate(16*(1-animation.progress+(hover*hover*0.5F)), 0, 0);
            graphics.setColor(1, 1, 1, animation.progress);

            animation.update();
            int color = Colors.blend(Colors.themeColor(), Colors.setAlpha(Colors.BLACK, 0.5F), hover*hover*0.5F);
            TesselateHelper.getShapeTesslator()
                    .fillRect(graphics.pose().last().pose(), x, y+1, x+100, y+h-2, color)
                    .fillGradient(graphics.pose().last().pose(), x+100, y+1, x+150, y+h-2, color, Colors.setAlpha(color, 0))
                    .close();
            if (hasIcon() && animation.progress > 0.5F) {
                icon.draw(graphics, x+4, y+3, 10, 10);
            }
            if (!isEnabled())
                graphics.setColor(1, 1, 1, 0.5F);
            graphics.drawString(getFont(), getTitle(), x+18, y+4, Colors.setAlpha(Colors.themeColor(), Math.max(animation.progress, 0.05F)));

            graphics.setColor(1, 1, 1, 1);
            graphics.pose().popPose();
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
