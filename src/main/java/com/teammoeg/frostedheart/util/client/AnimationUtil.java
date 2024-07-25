package com.teammoeg.frostedheart.util.client;

import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Map;


public class AnimationUtil {
    private static final Map<String, AnimationElement> animations = new HashMap<>();

    /**
     * 按照动画时长和开始时间返回动画进度
     * <p>注意：计算是根据系统时间而不是 tick
     * @param animationLength 动画时长，单位为毫秒
     * @param id 动画的 ID
     * @param loop 动画是否循环，否则停止在 1.0
     * @return 0.0 ~ 1.0
     */
    public static float progress(int animationLength, String id, boolean loop) {
        if (!animations.containsKey(id)) {
            newAnimation(animationLength, id);
        }

        float progress = animations.get(id).getProgress(Util.milliTime());
        if (loop && progress >= 1.0F) {removeAnimation(id);}
        return Math.min(progress, 1.0F);
    }

    /**
     * 在不更新进度的情况下返回动画当前的进度
     */
    public static float getProgress(String id) {
        return animations.containsKey(id) ? Math.min(1, animations.get(id).animationProgress) : 0;
    }

    /**
     * 缓出效果
     */
    public static float fadeOut(int animationLength, String id, boolean loop) {
        float progress = progress(animationLength, id, loop);
        return progress * progress;
    }

    public static float getFadeOut(String id) {
        float progress = getProgress(id);
        return progress * progress;
    }

    /**
     * 缓入效果
     */
    public static float fadeIn(int animationLength, String id, boolean loop) {
        float progress = 1- progress(animationLength, id, loop);
        return 1-(progress * progress);
    }

    public static float getFadeIn(String id) {
        float progress = 1-getProgress(id);
        return 1-(progress * progress);
    }

    /**
     * 弹跳效果，和原版的闪烁标题效果一样
     */
    public static float bounce(int animationLength, String id, boolean loop) {
        float progress = progress(animationLength, id,loop);
        return (float) Math.sin(progress*Math.PI);
    }

    public static float getBounce(String id) {
        float progress = getProgress(id);
        return (float) Math.sin(progress*Math.PI);
    }

    public static long lastCalcDelta(String id) {
        return animations.get(id) != null ? Util.milliTime() - animations.get(id).lastGet : 0;
    }

    /**
     * 移除动画
     */
    public static void removeAnimation(String id) {
        animations.remove(id);
    }

    private static void newAnimation(int animationLength, String id) {
        animations.put(id, new AnimationElement(animationLength, Util.milliTime()));
    }

    static class AnimationElement {
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
}


