package com.teammoeg.chorda.client.popup;

import com.teammoeg.chorda.client.AnimationUtil;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.SWUIElement;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.contentpanel.Alignment;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.math.Colors;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;

public class PopupOverlay extends PrimaryLayer {
    public static final int MAX_POPUP_STACK_SIZE = 5;
    public static final int MIN_DISPLAY_TIME = 3000;
    @Getter
    private static final Deque<PopupElement> POPUPS = new ArrayDeque<>();
    public static final PopupOverlay INSTANCE = new PopupOverlay();

    private PopupOverlay() {}

    public static void pop(String message) {
        pop(Component.literal(message));
    }

    public static void pop(String message, int displayTime) {
        pop(Component.literal(message), displayTime);
    }

    public static void pop(Component message) {
        pop(message, -1);
    }

    public static void pop(Component message, int displayTime) {
        var ele = new PopupElement(message, displayTime <= 0 ? (int) (MIN_DISPLAY_TIME * Math.max(INSTANCE.getFont().width(message) / 200F, 1)) : displayTime);
        POPUPS.addLast(ele);
        if (POPUPS.size() > MAX_POPUP_STACK_SIZE) POPUPS.peek().animState = PopupElement.State.FADING_OUT;
        INSTANCE.elements.add(0, ele);
        INSTANCE.refresh();
    }

    public static void clear() {
        POPUPS.forEach(ele -> AnimationUtil.remove(ele.animProgressId()));
        POPUPS.clear();
        INSTANCE.elements.removeAll(POPUPS);
        INSTANCE.refresh();
    }

    @Override
    public void tick() {
        POPUPS.removeIf(ele -> {
            if (ele.animState == PopupElement.State.IDLE) {
                elements.remove(ele);
                refresh();
                return true;
            }
            return false;
        });
        super.tick();
    }

    @Override
    public void refresh() {
        for (UIElement ele : elements) {
            ele.refresh();
        }
        alignWidgets();
        setSizeToContentSize();
        setOffsetY(24);
    }

    @Override
    public void alignWidgets() {
        align(false);
    }

    @Override
    public boolean shouldRenderGradient() {
        return false;
    }

    static class PopupElement extends SWUIElement {
        final Component message;
        final int displayTime;
        CGuiHelper.LineDrawingContext context;
        State animState = State.FADING_IN;

        public PopupElement(Component message) {
            super(PopupOverlay.INSTANCE);
            this.message = message;
            this.displayTime = (int) (MIN_DISPLAY_TIME * Math.max(getFont().width(message) / 200F, 1));
        }

        public PopupElement(Component message, int displayTime) {
            super(PopupOverlay.INSTANCE);
            this.message = message;
            this.displayTime = displayTime;
        }

        @Override
        public void doRender(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
            if (animState == State.IDLE) return;
            if (context == null) refresh();

            float alpha = 1;
            switch (animState) {
                case FADING_IN -> {
                    alpha = progress(animFadeId(), 100, State.PROGRESSING);
                }
                case PROGRESSING -> {
                    progress(animProgressId(), displayTime, State.FADING_OUT);
                }
                case FADING_OUT -> {
                    alpha = 1-progress(animFadeId(), 1000, State.IDLE);
                    if (animState==State.IDLE) return;
                }
            }
            graphics.pose().pushPose();
            graphics.setColor(1, 1, 1, alpha);
            graphics.pose().translate(0, 0, 400);
            CGuiHelper.drawStringLines(graphics, getFont(), context.lines(), x, y,
                    Colors.themeColor(), 1, true, Colors.setAlpha(Colors.BLACK, 128), Alignment.CENTER);
            graphics.setColor(1, 1, 1, 1);
            graphics.pose().popPose();
        }

        @Override
        public void refresh() {
            context = CGuiHelper.split(message, getFont(), (int)(ClientUtils.screenWidth()*0.8F));
            setSize(context.maxWidth(), context.lineSize()*10);
            if (isFirstFrame()) {
                forceSetPos(ClientUtils.screenWidth()/2, getY()-48);
            }
            superSetPos(ClientUtils.screenWidth()/2, getY());
        }

        float progress(String id, int length, State nextState) {
            float f = AnimationUtil.progress(length, id, false);
            if (f == 1) {
                animState = nextState;
                AnimationUtil.remove(id);
            }
            return f;
        }

        String animProgressId() {
            return "popup_progress_"+hashCode();
        }

        String animFadeId() {
            return "popup_fade_"+hashCode();
        }

        enum State {
            FADING_IN,
            FADING_OUT,
            PROGRESSING,
            IDLE
        }
    }
}
