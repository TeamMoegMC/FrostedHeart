package com.teammoeg.frostedheart.client.particles;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHParticleTypes {
	public static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, FHMain.MODID);

	public static final RegistryObject<BasicParticleType> STEAM = REGISTER.register("steam", () -> new BasicParticleType(false));
}
