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

package com.teammoeg.frostedheart.content.world.entities;

import com.teammoeg.frostedheart.bootstrap.reference.FHSoundEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 客户端表现辅助：Boss 音乐（the_fall_of_arcana）的启停。
 * <p>
 * Client-side helper managing the looping boss music
 * (the_fall_of_arcana, OGA open-source track).
 */
@OnlyIn(Dist.CLIENT)
public final class CuriosityClientEffects {
    private CuriosityClientEffects() {
    }

    private static CuriosityMusic music;

    /** 由 CuriosityEntity 客户端 tick 调用 / Called from the entity's client tick. */
    public static void updateMusic(CuriosityEntity owner) {
        Minecraft mc = Minecraft.getInstance();
        boolean want = owner.wantsMusic();
        if (want) {
            if (music == null || music.owner != owner || !mc.getSoundManager().isActive(music)) {
                music = new CuriosityMusic(owner);
                mc.getSoundManager().play(music);
            }
        } else if (music != null && music.owner == owner) {
            mc.getSoundManager().stop(music);
            music = null;
        }
    }

    /** 循环 Boss 音乐实例 / Looping boss music instance. */
    static class CuriosityMusic extends AbstractSoundInstance implements TickableSoundInstance {
        final CuriosityEntity owner;

        CuriosityMusic(CuriosityEntity owner) {
            super(FHSoundEvents.TFOA.get(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.owner = owner;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.45F;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public boolean isStopped() {
            // 实体被移除或战斗结束时自动停止 / stops itself when the entity is gone or the fight ends
            return this.owner.isRemoved() || !this.owner.wantsMusic();
        }

        @Override
        public void tick() {
            // 停止由 isStopped 驱动 / stopping is driven by isStopped
        }
    }
}
