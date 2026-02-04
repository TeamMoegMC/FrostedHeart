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

package com.teammoeg.frostedresearch;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.teammoeg.chorda.CompatModule;
import com.teammoeg.frostedresearch.compat.CreateCompat;
import com.teammoeg.frostedresearch.compat.ftb.FRRewardTypes;
import com.teammoeg.frostedresearch.compat.ftb.FTBTeamsEvents;
import com.teammoeg.frostedresearch.compat.tetra.TetraCompat;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FRMain.MODID)
public class FRMain {
    // CConstants
    public static final String MODID = "frostedresearch";
    public static final String ALIAS = "research";
    public static final String MODNAME = "Frosted Research";
    public static final Logger LOGGER = LogManager.getLogger(MODNAME);
	public FRMain() {
	    IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forge = MinecraftForge.EVENT_BUS;
        CompatModule.enableCompatModule();
        FRContents.init(mod);
        FRSpecialDataTypes.init();
        FRConfig.register();
        mod.addListener(this::setup);
        if(CompatModule.isTetraLoaded())
        	TetraCompat.init();
        if(CompatModule.isFTBQLoaded())
        	FRRewardTypes.init();
        if(CompatModule.isFTBTLoaded())
        	FTBTeamsEvents.init();
        if(CompatModule.isCreateLoaded())
        	CreateCompat.init();
        
	}
	public static ResourceLocation rl(String path) {
		return new ResourceLocation(MODID,path);
	}
    private void setup(final FMLCommonSetupEvent event) {
        
        FRNetwork.INSTANCE.register();

    }
}
