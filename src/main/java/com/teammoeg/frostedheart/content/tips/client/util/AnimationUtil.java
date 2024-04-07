package com.teammoeg.frostedheart.content.tips.client.util;

import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Map;


public class AnimationUtil {
    private static final Map<String, AnimationElement> animationList = new HashMap<>();

    public static float calcProgress(int animationLength, String id, boolean loop) {
        if (!animationList.containsKey(id)) {
            newAnimation(animationLength, id);
        }

        float progress = animationList.get(id).getProgress(Util.milliTime());
        if (loop && progress >= 1.0F) {removeAnimation(id);}
        return Math.min(progress, 1.0F);
    }

    public static float getProgress(String id) {
        return animationList.containsKey(id) ? Math.min(1, animationList.get(id).animationProgress) : 0;
    }

    public static float calcFadeOut(int animationLength, String id, boolean loop) {
        float progress = calcProgress(animationLength, id, loop);
        return progress * progress;
    }

    public static float getFadeOut(String id) {
        float progress = getProgress(id);
        return progress * progress;
    }

    public static float calcFadeIn(int animationLength, String id, boolean loop) {
        float progress = 1-calcProgress(animationLength, id, loop);
        return 1-(progress * progress);
    }

    public static float getFadeIn(String id) {
        float progress = 1-getProgress(id);
        return 1-(progress * progress);
    }

    public static float calcBounce(int animationLength, String id, boolean loop) {
        float progress = calcProgress(animationLength, id,loop);
        return (float) Math.sin(progress*Math.PI);
    }

    public static float getBounce(String id) {
        float progress = getProgress(id);
        return (float) Math.sin(progress*Math.PI);
    }

    public static long lastCalcDelta(String id) {
        return animationList.get(id) != null ? Util.milliTime() - animationList.get(id).lastGet : 0;
    }

    public static void removeAnimation(String id) {
        animationList.remove(id);
    }

    private static void newAnimation(int animationLength, String id) {
        animationList.put(id, new AnimationElement(animationLength, Util.milliTime()));
    }
}

class AnimationElement {
    final int animationLength;
    final long startTime;
    long lastGet;
    float animationProgress;

    AnimationElement(int animationLength, long startTime) {
        this.animationLength = animationLength;
        this.startTime = startTime;
    }

    float getProgress(long currentTime) {
        this.animationProgress = this.animationProgress < 1.0F ? (currentTime - startTime) / (float) animationLength : 1.0F;
        this.lastGet = currentTime;
        return animationProgress;
    }
}
