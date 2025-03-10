package com.teammoeg.frostedresearch;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;

@Mod(FRMain.MODID)
public class FRMain {
    // CConstants
    public static final String MODID = "frostedresearch";
    public static final String ALIAS = "fr";
    public static final String MODNAME = "Frosted Research";
    public static final Logger LOGGER = LogManager.getLogger(MODNAME);
	public FRMain() {
	}
	public static ResourceLocation rl(String path) {
		return new ResourceLocation(MODID,path);
	}

}
