/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.chorda;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Chorda 客户端初始化类。负责注册客户端专用的事件监听器和渲染器。
 * <p>
 * Client-side initialization class for Chorda. Responsible for registering
 * client-only event listeners and renderers.
 */
public class ChordaClient {

    /**
     * 初始化客户端，注册客户端事件监听器。仅在物理客户端上调用。
     * <p>
     * Initializes the client side, registering client event listeners.
     * Only called on the physical client.
     */
    public static void init() {
        // 仅客户端初始化 / Client only init
        IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forge = MinecraftForge.EVENT_BUS;

        Chorda.LOGGER.info(Chorda.CLIENT_INIT, "Initializing client");

        Chorda.LOGGER.info(Chorda.CLIENT_INIT, "Registering client forge event listeners");

        Chorda.LOGGER.info(Chorda.CLIENT_INIT, "Registering client mod event listeners");

        Chorda.LOGGER.info(Chorda.CLIENT_INIT, "Finished initializing client");
    }
}
