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
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;

public class FHParticle extends SpriteTexturedParticle {
    protected float originalScale = 1.3F;

    protected FHParticle(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    public FHParticle(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z, motionX, motionY, motionZ);
        this.motionX *= 1.25;
        this.motionY *= 1.25;
        this.motionZ *= 1.25;
    }

    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void renderParticle(IVertexBuilder worldRendererIn, ActiveRenderInfo entityIn, float pt) {
        float age = (this.age + pt) / maxAge * 32.0F;

        age = MathHelper.clamp(age, 0.0F, 1.0F);
        super.particleAlpha = MathHelper.clamp(1 - (this.age + pt) / maxAge, 0.0F, 1.0F);
        super.particleScale = originalScale * (age + this.age * 0.0375F);
        super.renderParticle(worldRendererIn, entityIn, pt);
    }

    public void tick() {
        this.prevPosX = posX;
        this.prevPosY = posY;
        this.prevPosZ = posZ;
        if (age >= maxAge)
            setExpired();
        this.age++;
        this.motionY -= 0.04D * particleGravity;
        move(motionX, motionY, motionZ);

        if (posY == prevPosY) {
            this.motionX *= 1.1D;
            this.motionZ *= 1.1D;
        }
        this.motionX *= 0.96D;
        this.motionY *= 0.96D;
        this.motionZ *= 0.96D;

        if (onGround) {
            this.motionX *= 0.67D;
            this.motionZ *= 0.67D;
        }
    }


}
