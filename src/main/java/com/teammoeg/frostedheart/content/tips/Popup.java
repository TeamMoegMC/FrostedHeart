package com.teammoeg.frostedheart.content.tips;

import com.teammoeg.chorda.util.CGuiHelper;
import com.teammoeg.chorda.util.client.AnimationUtil;
import com.teammoeg.chorda.util.client.ClientUtils;
import com.teammoeg.chorda.util.client.ColorHelper;
import com.teammoeg.chorda.util.lang.Components;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedList;
import java.util.Queue;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class Popup {
    private static final Queue<Component> POPUPS = new LinkedList<>();
    private static final int DISPLAY_TIME = 3000;
    private static final String FADING_ANIM_NAME = Popup.class.getName() + "fading";
    private static final String DISPLAYING_ANIM_NAME = Popup.class.getName() + "displaying";

    private static State state = State.IDLE;

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        if (POPUPS.isEmpty()) {
            state = State.IDLE;
        } else {
            var graphic = event.getGuiGraphics();
            var message = POPUPS.peek();
            if (message == null) {
                POPUPS.remove();
                return;
            }

            switch (state) {
                case IDLE -> state = State.FADING_IN;
                case FADING_IN -> {
                    float progress = AnimationUtil.fadeIn(400, FADING_ANIM_NAME, false);
                    render(progress, graphic, message);
                    if (progress == 1.0F) {
                        state = State.PROGRESSING;
                        AnimationUtil.remove(FADING_ANIM_NAME);
                    }
                }
                case PROGRESSING -> {
                    int displayTime = DISPLAY_TIME * Math.max(message.getString().length() / 48, 1);
                    float progress = AnimationUtil.fadeIn(displayTime, DISPLAYING_ANIM_NAME, false);
                    render(1, graphic, message);
                    if (progress == 1.0F) {
                        state = State.FADING_OUT;
                        AnimationUtil.remove(DISPLAYING_ANIM_NAME);
                    }
                }
                case FADING_OUT -> {
                    float progress = 1-AnimationUtil.fadeIn(400, FADING_ANIM_NAME, false);
                    render(progress, graphic, message);
                    if (progress == 0.0F) {
                        state = State.IDLE;
                        AnimationUtil.remove(FADING_ANIM_NAME);
                        POPUPS.remove();
                    }
                }
                default -> state = State.IDLE;
            }
        }
    }

    private static void render(float progress, GuiGraphics graphic, Component message) {
        var font = ClientUtils.font();
        var lines = ClientUtils.font().split(message, (int)(ClientUtils.screenWidth() * 0.8F));
        var pose = graphic.pose();
        int height = 24 + lines.size()*10;
        float y = height * progress + 24;
        float x = ClientUtils.screenWidth() * 0.5F;
        pose.pushPose();
        pose.translate(x, y-height, 0);
        CGuiHelper.drawCenteredStrings(graphic, font, lines, 0, 0, ColorHelper.CYAN, 10, true, true);
        pose.popPose();
    }

    private enum State {
        FADING_IN,
        FADING_OUT,
        PROGRESSING,
        IDLE
    }

    public static void put(Component message) {
        POPUPS.add(message);
    }

    public static void put(String message) {
        put(Components.translateOrElseStr(message));
    }

    public static boolean isEmpty() {
        return POPUPS.isEmpty();
    }

    public static void clear() {
        POPUPS.clear();
        state = State.IDLE;
        AnimationUtil.remove(FADING_ANIM_NAME);
        AnimationUtil.remove(DISPLAYING_ANIM_NAME);
    }
}
