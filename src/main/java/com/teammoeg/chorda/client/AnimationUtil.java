/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.chorda.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.Util;


/**
 * 基于系统时间的动画工具类，提供线性、缓入、缓出和弹跳等动画效果。
 * 动画进度基于系统毫秒时间计算，而非游戏tick，因此不受游戏帧率和暂停影响。
 * <p>
 * Animation utility class based on system time, providing linear, fade-in, fade-out,
 * and bounce animation effects. Animation progress is calculated from system milliseconds
 * rather than game ticks, so it is unaffected by game framerate or pausing.
 */
public class AnimationUtil {
    private static final Map<String, AnimationElement> animations = new HashMap<>();

    /**
     * 按照动画时长和开始时间返回动画进度。
     * <p>注意：计算是根据系统时间而不是 tick。
     * <p>
     * Returns animation progress based on animation duration and start time.
     * Note: calculation uses system time, not game ticks.
     *
     * @param animationLength 动画时长，单位为毫秒 / Animation duration in milliseconds
     * @param id 动画的唯一标识符 / Unique identifier for the animation
     * @param loop 动画是否循环，否则停止在1.0 / Whether the animation loops; if false, stops at 1.0
     * @return 0.0到1.0之间的进度值 / Progress value between 0.0 and 1.0
     */
    public static float progress(int animationLength, String id, boolean loop) {
        if (!animations.containsKey(id)) {
            add(animationLength, id);
        }

        float progress = animations.get(id).getProgress(Util.getMillis());
        if (loop && progress >= 1.0F) {
            remove(id);}
        return Math.min(progress, 1.0F);
    }

    /**
     * 在不更新进度的情况下返回动画当前的进度。
     * <p>
     * Returns the current animation progress without advancing the timer.
     *
     * @param id 动画的唯一标识符 / Unique identifier for the animation
     * @return 0.0到1.0之间的进度值 / Progress value between 0.0 and 1.0
     */
    public static float getProgress(String id) {
        return animations.containsKey(id) ? Math.min(1, animations.get(id).animationProgress) : 0;
    }

    /**
     * 缓出效果（二次曲线衰减）。
     * <p>
     * Ease-out effect using quadratic curve deceleration.
     *
     * @param animationLength 动画时长，单位为毫秒 / Animation duration in milliseconds
     * @param id 动画的唯一标识符 / Unique identifier for the animation
     * @param loop 动画是否循环 / Whether the animation loops
     * @return 经过缓出曲线变换的进度值 / Progress value transformed with ease-out curve
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
     * 缓入效果（二次曲线加速）。
     * <p>
     * Ease-in effect using quadratic curve acceleration.
     *
     * @param animationLength 动画时长，单位为毫秒 / Animation duration in milliseconds
     * @param id 动画的唯一标识符 / Unique identifier for the animation
     * @param loop 动画是否循环 / Whether the animation loops
     * @return 经过缓入曲线变换的进度值 / Progress value transformed with ease-in curve
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
     * 弹跳效果（正弦曲线），和原版的闪烁标题效果一样。
     * <p>
     * Bounce effect using a sine curve, similar to vanilla's splash title animation.
     *
     * @param animationLength 动画时长，单位为毫秒 / Animation duration in milliseconds
     * @param id 动画的唯一标识符 / Unique identifier for the animation
     * @param loop 动画是否循环 / Whether the animation loops
     * @return 经过正弦弹跳变换的进度值 / Progress value transformed with sine bounce curve
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
        return animations.get(id) != null ? Util.getMillis() - animations.get(id).lastGet : 0;
    }

    /**
     * 移除指定ID的动画实例。
     * <p>
     * Removes the animation instance with the specified ID.
     *
     * @param id 要移除的动画标识符 / The animation identifier to remove
     */
    public static void remove(String id) {
        animations.remove(id);
    }

    private static void add(int animationLength, String id) {
        animations.put(id, new AnimationElement(animationLength, Util.getMillis()));
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


