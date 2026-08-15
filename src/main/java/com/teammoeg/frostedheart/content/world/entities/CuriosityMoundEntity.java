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

import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 地表隆起表现实体：追踪者在地表激起的雪丘，短暂存活并喷射雪雾粒子。
 * <p>
 * Short-lived visual entity: the snow mound raised on the surface where the
 * underground tracker passes, emitting snow fog particles. Never saved, no
 * collision, purely client rendered.
 */
public class CuriosityMoundEntity extends Entity {
    private int life;

    public CuriosityMoundEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                this.level().addParticle(ParticleTypes.SNOWFLAKE,
                        this.getX() + (this.random.nextDouble() - 0.5) * 1.4,
                        this.getY() + this.random.nextDouble() * 0.8,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 1.4,
                        (this.random.nextDouble() - 0.5) * 0.15,
                        0.05 + this.random.nextDouble() * 0.1,
                        (this.random.nextDouble() - 0.5) * 0.15);
            }
            this.level().addParticle(ParticleTypes.POOF,
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.8,
                    this.getY() + this.random.nextDouble() * 0.5,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.8,
                    (this.random.nextDouble() - 0.5) * 0.1,
                    0.05,
                    (this.random.nextDouble() - 0.5) * 0.1);
        } else if (++this.life >= FHConfig.SERVER.CURIOSITY.moundLifetimeTicks.get()) {
            this.discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
