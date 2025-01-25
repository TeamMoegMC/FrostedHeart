/*
 * Copyright (c) 2024 TeamMoeg
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

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * A particle that simulates the behavior of a gas.
 * <p>
 * For example, steam, smoke, or any other gas-like particle.
 */
public class GasParticle extends TextureSheetParticle {
    /** The original scale of the particle. Default: 1.0F */
    protected float initialScale;
    /** The scaling increase per tick. Default  */
    protected float scaleSpeed;
    /** The temperature of the particle. Range: 0K - 5000K. Default: 273K. */
    protected double temperature;
    /** The density of the particle. Unit: kg/m^3 */
    protected double density;
    /** The coefficient of air resistance of the particle. Range: [0, 1] */
    protected double airResistance;

    protected static final double AIR_DENSITY = 1.225; // Density of air at sea level in kg/m^3
    protected static final double AIR_TEMPERATURE = 273; // Temperature of air at sea level in K
    protected static final double GRAVITY_ACC = 9.81; // Gravity acceleration in m/s^2
    protected static final double DENSITY_TEMPERATURE_SENSITIVITY = 0.001; // Unit is K^-1


    public GasParticle(ClientLevel world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z, motionX, motionY, motionZ);
        this.initialScale = 1.0F;
        this.scaleSpeed = 0.02F;
        this.lifetime = (int) (100.0D / (Math.random() * 0.2D + 0.8D));
        // default gas physical properties
        this.density = 1.0;
        this.airResistance = 0.01;
        this.temperature = 300;
        // compute the effective gravity
        this.gravity = getEffectiveGravity();
    }

    protected float getEffectiveGravity() {
        // effective density:
        // formula: rho_gas_effective = rho_gas * (1 - beta * (T - T_air))
        double effectiveDensity = density * (1 - DENSITY_TEMPERATURE_SENSITIVITY * (temperature - AIR_TEMPERATURE));
        // gravity acceleration in m/s^2 = g * (rho_air - 2 * rho_gas) / rho_gas
        // divide by 400 to convert to blocks/tick^2
        return (float) (GRAVITY_ACC * (AIR_DENSITY - 2 * effectiveDensity) / effectiveDensity) / 400;
    }

    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(VertexConsumer worldRendererIn, Camera entityIn, float pt) {
        // simulate age-based particle scaling and alpha decay
        float ageFraction = Mth.clamp((this.age + pt) / lifetime, 0.0F, 1.0F);
        // the particle will fade out as it ages
        super.alpha = Mth.clamp(1 - ageFraction, 0.0F, 1.0F);
        // the particle will grow as it ages
        super.quadSize = initialScale * (1 + (this.age + pt) * scaleSpeed);
        super.render(worldRendererIn, entityIn, pt);
    }

    public void tick() {
        // update previous position
        this.xo = x;
        this.yo = y;
        this.zo = z;

        // kill the particle if it's too old
        if (age >= lifetime)
            remove();
        this.age++;

        // Apply gravity acceleration
        yd += gravity;

        move(xd, yd, zd);

        // Simulate air resistance
        xd *= (1 - airResistance);
        yd *= (1 - airResistance);
        zd *= (1 - airResistance);

        // Friction increases by 33% when on ground
        if (onGround) {
            this.xd *= (1 - airResistance) * 0.67D;
            this.zd *= (1 - airResistance) * 0.67D;
        }

        // Spread hitting a ceiling or other obstacle
        if (y == yo) {
            xd += yd;
            zd += yd;
        }
    }

}
