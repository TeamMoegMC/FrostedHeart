/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.client.particles;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.math.MathHelper;

public class WetSteamParticle extends SpriteTexturedParticle {

    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite spriteSet;

        public Factory(IAnimatedSprite spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle makeParticle(BasicParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            WetSteamParticle steamParticle = new WetSteamParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
            steamParticle.selectSpriteRandomly(this.spriteSet);
            return steamParticle;
        }
    }

    // The motion is the distance we want the particle to move
    public WetSteamParticle(ClientWorld world, double x, double y, double z, double distanceX, double distanceY, double distanceZ) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.particleRed = this.particleGreen = this.particleBlue = (float) (Math.random() * 0.15) + 0.7f;
        this.particleAlpha = 0.8f;
        this.particleScale = 0.2f;

        // This can be better, but i can't be bothered finding out the proper way to do it
        this.maxAge = ((int) ((distanceY + 1) * -20)) + 30;
        if (maxAge == 0) maxAge = 1;

        this.particleGravity = 0.001f;

        this.motionY = -0.002 * distanceY;
        this.motionX = (distanceX / maxAge * 0.9) * 3.2;
        this.motionZ = (distanceZ / maxAge * 0.9) * 3.2;
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public void tick() {
        if (!Double.isFinite(motionX) || !Double.isFinite(motionY) || !Double.isFinite(motionZ)) {
            setExpired();
            return;
        }

        // update previous position
        this.prevPosX = posX;
        this.prevPosY = posY;
        this.prevPosZ = posZ;

        // kill the particle if it's too old
        if (age >= maxAge)
            setExpired();
        this.age++;

        // apply gravity
        this.motionY -= particleGravity;

        // move the particle
        move(motionX, motionY, motionZ);

        // natural friction decay
        this.motionX *= 0.97D;
        this.motionZ *= 0.97D;
    }
}
