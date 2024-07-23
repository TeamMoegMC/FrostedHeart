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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.math.MathHelper;

public class WetSteamParticle extends TextureSheetParticle {

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            WetSteamParticle steamParticle = new WetSteamParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
            steamParticle.pickSprite(this.spriteSet);
            return steamParticle;
        }
    }

    // The motion is the distance we want the particle to move
    public WetSteamParticle(ClientLevel world, double x, double y, double z, double distanceX, double distanceY, double distanceZ) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.rCol = this.gCol = this.bCol = (float) (Math.random() * 0.15) + 0.7f;
        this.alpha = 0.8f;
        this.quadSize = 0.2f;

        // This can be better, but i can't be bothered finding out the proper way to do it
        this.lifetime = ((int) ((distanceY + 1) * -20)) + 30;
        if (lifetime == 0) lifetime = 1;

        this.gravity = 0.001f;

        this.yd = -0.002 * distanceY;
        this.xd = (distanceX / lifetime * 0.9) * 3.2;
        this.zd = (distanceZ / lifetime * 0.9) * 3.2;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public void tick() {
        if (!Double.isFinite(xd) || !Double.isFinite(yd) || !Double.isFinite(zd)) {
            remove();
            return;
        }

        // update previous position
        this.xo = x;
        this.yo = y;
        this.zo = z;

        // kill the particle if it's too old
        if (age >= lifetime)
            remove();
        this.age++;

        // apply gravity
        this.yd -= gravity;

        // move the particle
        move(xd, yd, zd);

        // natural friction decay
        this.xd *= 0.97D;
        this.zd *= 0.97D;
    }
}
