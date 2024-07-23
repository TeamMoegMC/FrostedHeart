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

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.math.MathHelper;

public class BreathParticle extends GasParticle {

    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite spriteSet;

        public Factory(IAnimatedSprite spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(BasicParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            BreathParticle steamParticle = new BreathParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
            steamParticle.pickSprite(this.spriteSet);
            return steamParticle;
        }
    }

    public BreathParticle(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z, motionX, motionY, motionZ);
        this.gravity = 0.0F;
        this.rCol = this.gCol = this.bCol = (float) (Math.random() * 0.2) + 0.8f;
        this.initialScale = 0.05F;
        this.lifetime = (int) (40.0D / (Math.random() * 0.2D + 0.8D));
        // physical properties of breath
        this.density = 0.6;
        this.temperature = 373;
        this.airResistance = 0.02;
        // breadth initial velocity is slow
        this.xd *= 0.1;
        this.yd *= 0.1;
        this.zd *= 0.1;
        // must call this after setting the physical properties
        this.gravity = getEffectiveGravity();
    }

    @Override
    public void render(IVertexBuilder worldRendererIn, ActiveRenderInfo entityIn, float pt) {
        float age = (this.age + pt) / lifetime * 32.0F;
        age = MathHelper.clamp(age, 0.0F, 1.0F);
        float alpha = 0.3F * (1 - (this.age + pt) / lifetime);
        super.alpha = MathHelper.clamp(alpha, 0.0F, 0.3F);
        super.quadSize = initialScale * (age + this.age * 0.0375F) * 0.5F;
        super.render(worldRendererIn, entityIn, pt);
    }
}
