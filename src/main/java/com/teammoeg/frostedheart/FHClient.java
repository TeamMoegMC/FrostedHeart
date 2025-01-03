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

package com.teammoeg.frostedheart;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static com.teammoeg.frostedheart.FHMain.*;

import com.teammoeg.frostedheart.bootstrap.client.FHDynamicModels;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FHClient {
    public FHClient() {

    }

    public static void init() {
        IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forge = MinecraftForge.EVENT_BUS;

        LOGGER.info(CLIENT_INIT, "Initializing client");
        FHDynamicModels.setup();
        // Moved to FHClientEventsMod
//        KGlyphProvider.addListener();

        LOGGER.info(CLIENT_INIT, "Registering client forge event listeners");

        LOGGER.info(CLIENT_INIT, "Registering client mod event listeners");

        LOGGER.info(CLIENT_INIT, "Finished initializing client");
    }



}
