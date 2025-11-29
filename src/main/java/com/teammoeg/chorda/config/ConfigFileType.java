package com.teammoeg.chorda.config;

import java.io.File;

import com.mojang.serialization.Codec;

import net.minecraftforge.fml.loading.FMLPaths;
/**
 * Represent a subfolder with handful of configuration files
 * */
public record ConfigFileType<T>(Codec<T> codec,File folder) {
	public ConfigFileType(Codec<T> codec,String subfolder) {
		this(codec,new File(FMLPaths.CONFIGDIR.get().toFile(),subfolder));
	}

	@Override
	public String toString() {
		return folder.getName() ;
	}
}
