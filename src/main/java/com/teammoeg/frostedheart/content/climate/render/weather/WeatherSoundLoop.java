/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.render.weather;

import com.teammoeg.frostedheart.bootstrap.reference.FHSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundSource;

/** Exactly one non-positional V1 wind loop, with state-driven volume. */
public final class WeatherSoundLoop {
    public static final WeatherSoundLoop INSTANCE = new WeatherSoundLoop();
    private WindSound active;

    private WeatherSoundLoop() {
    }

    public void tick(Minecraft minecraft, boolean enabled) {
        if (!enabled || minecraft.level == null || minecraft.player == null || !ClientWeatherState.INSTANCE.hasGrid()) {
            stop(minecraft);
            return;
        }
        var sample = ClientWeatherState.INSTANCE.tickCameraSample();
        float target = sample.windIntensity * (0.12F + 0.28F * sample.whiteoutIntensity);
        if (target <= 0.01F) {
            stop(minecraft);
            return;
        }
        float targetPitch = 0.68F + sample.windIntensity * 0.16F;
        if (active == null || !minecraft.getSoundManager().isActive(active)) {
            active = new WindSound(target, targetPitch);
            minecraft.getSoundManager().play(active);
        }
        active.targetVolume = target;
        active.targetPitch = targetPitch;
    }

    public void stop(Minecraft minecraft) {
        if (active != null) {
            minecraft.getSoundManager().stop(active);
            active = null;
        }
    }

    private static final class WindSound extends AbstractSoundInstance implements TickableSoundInstance {
        private float targetVolume;
        private float targetPitch = 0.75F;
        private boolean stopped;

        private WindSound(float initialVolume, float initialPitch) {
            super(FHSoundEvents.WIND.get(), SoundSource.WEATHER, SoundInstance.createUnseededRandom());
            looping = true;
            delay = 0;
            attenuation = SoundInstance.Attenuation.NONE;
            targetVolume = initialVolume;
            targetPitch = initialPitch;
            volume = initialVolume;
            pitch = initialPitch;
        }

        @Override
        public boolean isStopped() {
            return stopped;
        }

        @Override
        public void tick() {
            volume += (targetVolume - volume) * 0.15F;
            pitch += (targetPitch - pitch) * 0.10F;
        }
    }
}
