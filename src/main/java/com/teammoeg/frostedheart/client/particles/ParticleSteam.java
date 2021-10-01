package com.teammoeg.frostedheart.client.particles;

import java.awt.Color;

import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;

public class ParticleSteam extends FHParticle {

	public ParticleSteam(ClientWorld world, double x, double y, double z, double motionX, double motionY,
			double motionZ) {
		
		super(world,  x, y, z, motionX, motionY, motionZ);
        this.particleGravity = -0.1F;
        this.particleRed = this.particleGreen = this.particleBlue = (float) (Math.random() * 0.4) + 0.4f;
        this.maxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D));
	}
	public static class Factory implements IParticleFactory<BasicParticleType> {
		public Factory() {
		}

		@Override
		public Particle makeParticle(BasicParticleType typeIn, ClientWorld worldIn, double x, double y, double z,
				double xSpeed, double ySpeed, double zSpeed) {
			return new FHParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
		}
	}
}
