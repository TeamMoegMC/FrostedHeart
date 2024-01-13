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

public class BreathParticle extends FHParticle {

    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite spriteSet;

        public Factory(IAnimatedSprite spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle makeParticle(BasicParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            BreathParticle steamParticle = new BreathParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
            steamParticle.selectSpriteRandomly(this.spriteSet);
            return steamParticle;
        }
    }

    public BreathParticle(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z, motionX, motionY, motionZ);
        this.particleGravity = 0.0F;
        this.particleRed = this.particleGreen = this.particleBlue = (float) (Math.random() * 0.2) + 0.8f;
        this.originalScale = 0.05F;
        this.maxAge = (int) (40.0D / (Math.random() * 0.2D + 0.8D));
        this.motionX *= 0.1;
        this.motionY *= 0.1;
        this.motionZ *= 0.1;
    }

    @Override
    public void renderParticle(IVertexBuilder worldRendererIn, ActiveRenderInfo entityIn, float pt) {
        float age = (this.age + pt) / maxAge * 32.0F;
        age = MathHelper.clamp(age, 0.0F, 1.0F);
        float alpha = 0.3F * (1 - (this.age + pt) / maxAge);
        super.particleAlpha = MathHelper.clamp(alpha, 0.0F, 0.3F);
        super.particleScale = originalScale * (age + this.age * 0.0375F) * 0.5F;
        super.renderParticle(worldRendererIn, entityIn, pt);
    }
}
