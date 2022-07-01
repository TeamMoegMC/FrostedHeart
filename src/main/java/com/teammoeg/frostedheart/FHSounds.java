package com.teammoeg.frostedheart;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHSounds {
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FHMain.MODID);
	public static final RegistryObject<SoundEvent> MC_BELL=SOUNDS.register("mc_bell",()->new SoundEvent(new ResourceLocation(FHMain.MODID,"mc_bell")));
	public static final RegistryObject<SoundEvent> MC_ROLL=SOUNDS.register("mc_roll",()->new SoundEvent(new ResourceLocation(FHMain.MODID,"mc_roll")));
	public FHSounds() {
	}

}
