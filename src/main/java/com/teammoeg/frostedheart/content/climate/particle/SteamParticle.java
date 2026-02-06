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

package com.teammoeg.frostedheart.content.climate.particle;

import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class SteamParticle extends GasParticle {

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            SteamParticle steamParticle = new SteamParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
            steamParticle.pickSprite(this.spriteSet);
            return steamParticle;
        }
    }

    public SteamParticle(ClientLevel world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z, motionX, motionY, motionZ);
        // steam is gray, with a random tint
        this.rCol = this.gCol = this.bCol = (float) (Math.random() * 0.2) + 0.8f;
        this.lifetime = (int) (200D / (Math.random() * 0.2D + 0.8D));
        this.initialScale = 0.5F;
        // steam physical properties
        this.density = 0.6;
        this.temperature = 373;
        this.airResistance = 0.05;
        // must call this after setting the physical properties
        this.gravity = getEffectiveGravity();
    }
}
