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

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import com.teammoeg.chorda.compat.ftb.FTBTeamsEvents;
import com.teammoeg.chorda.dataholders.client.CClientDataStorage;

/**
 * Chorda 模组主入口类。Chorda 是 TeamMoeg 项目的通用基础库模组，
 * 提供方块、菜单、网络、序列化、UI 等基础框架。
 * <p>
 * Main entry point for the Chorda mod. Chorda is a general-purpose library mod
 * for TeamMoeg projects, providing foundational frameworks for blocks, menus,
 * networking, serialization, UI, and more.
 */
@Mod(Chorda.MODID)
public class Chorda {
    public static final String MODID = "chorda";
    public static final String MODNAME = "Chorda";
    // 日志记录器 / Logger
    public static final Logger LOGGER = LogManager.getLogger(MODNAME);
    // 日志标记，用于分类不同阶段的日志输出
    // Log markers for categorizing log output at different stages
    public static final Marker VERSION_CHECK = MarkerManager.getMarker("Version Check");
    public static final Marker INIT = MarkerManager.getMarker("Init");
    public static final Marker SETUP = MarkerManager.getMarker("Setup");
    public static final Marker COMMON_INIT = MarkerManager.getMarker("Common").addParents(INIT);
    public static final Marker CLIENT_INIT = MarkerManager.getMarker("Client").addParents(INIT);
    public static final Marker COMMON_SETUP = MarkerManager.getMarker("Common").addParents(SETUP);
    public static final Marker CLIENT_SETUP = MarkerManager.getMarker("Client").addParents(SETUP);
    public static final Marker UI = MarkerManager.getMarker("UI");

    /**
     * 创建属于本模组命名空间的资源路径。
     * <p>
     * Creates a {@link ResourceLocation} under this mod's namespace.
     *
     * @param path 资源路径 / the resource path
     * @return 完整的资源定位符 / the full resource location
     */
    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }

    /**
     * 构造函数，Forge 模组加载时自动调用。
     * 负责注册配置、事件监听器、兼容模块，以及初始化客户端。
     * <p>
     * Constructor, automatically invoked by Forge during mod loading.
     * Responsible for registering configs, event listeners, compat modules,
     * and initializing the client side.
     */
    public Chorda() {
        IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forge = MinecraftForge.EVENT_BUS;
        LOGGER.info(COMMON_INIT, "Reading Modlist");
        CompatModule.enableCompatModule();
        // Config
        LOGGER.info(COMMON_INIT, "Loading Config");
        ChordaConfig.register();
        
        // Init
        LOGGER.info(COMMON_INIT, "Initializing " + MODNAME);

        // Compat init
        LOGGER.info(COMMON_INIT, "Initializing Mod Compatibilities");

        // Deferred Registration
        // Order doesn't matter here, as that's why we use deferred registers
        // See ForgeRegistries for more info
        LOGGER.info(COMMON_INIT, "Registering Deferred Registers");

        // Forge bus
        LOGGER.info(COMMON_INIT, "Registering Forge Event Listeners");

        // Mod bus
        LOGGER.info(COMMON_INIT, "Registering Mod Event Listeners");
        mod.addListener(this::setup);
        mod.addListener(this::processIMC);
        mod.addListener(this::enqueueIMC);
        mod.addListener(this::loadComplete);
        mod.addListener(this::clientSetup);

        // Client setup
        LOGGER.info(COMMON_INIT, "Proceeding to Client Initialization");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ChordaClient.init();
        }
        if(CompatModule.isFTBTLoaded()) {
        	FTBTeamsEvents.init();
        }
    }

    /**
     * 通用初始化，在延迟注册完成后执行。注册网络通道。
     * <p>
     * Common setup, executed after deferred registers are filled.
     * Registers network channels.
     *
     * @param event 通用初始化事件 / the common setup event
     */
    private void setup(final FMLCommonSetupEvent event) {
        ChordaNetwork.INSTANCE.register();
    }

    /**
     * 客户端初始化，加载客户端数据存储。
     * <p>
     * Client setup, loads client-side data storage.
     *
     * @param ev 客户端初始化事件 / the client setup event
     */
	public void clientSetup(final FMLClientSetupEvent ev) {
		CClientDataStorage.load();
	}

    /**
     * 发送模组间通信消息（IMC）。
     * <p>
     * Enqueue Inter-Mod Communication messages.
     *
     * @param event IMC 发送事件 / the IMC enqueue event
     */
    private void enqueueIMC(final InterModEnqueueEvent event) {

    }

    /**
     * 处理收到的模组间通信消息（IMC）。
     * <p>
     * Process received Inter-Mod Communication messages.
     *
     * @param event IMC 处理事件 / the IMC process event
     */
    private void processIMC(final InterModProcessEvent event) {

    }

    /**
     * 所有内容加载完成后的回调，通常不使用。
     * <p>
     * Callback after everything is loaded. Generally not used.
     *
     * @param event 加载完成事件 / the load complete event
     */
    private void loadComplete(FMLLoadCompleteEvent event) {

    }

}
