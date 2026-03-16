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

package com.teammoeg.chorda.recipe;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;


import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.crafting.Recipe;

/**
 * 配方重载监听器，在数据包资源重载时通知所有已注册的 {@link CodecRecipeSerializer}。
 * 使用线程安全的集合存储已注册的序列化器，确保在多线程环境下安全运行。
 * 通过 {@link CodecRecipeSerializer#setManaged()} 注册的序列化器会在资源重载时自动接收更新。
 * <p>
 * A recipe reload listener that notifies all registered {@link CodecRecipeSerializer} instances
 * when datapack resources are reloaded. Uses a thread-safe set to store registered serializers,
 * ensuring safe operation in multi-threaded environments. Serializers registered via
 * {@link CodecRecipeSerializer#setManaged()} will automatically receive updates on resource reload.
 *
 * @see CodecRecipeSerializer#setManaged()
 */
public class RecipeReloadListener implements ResourceManagerReloadListener {
    /** 数据包注册表引用 / Reference to the datapack registries */
    private final ReloadableServerResources dataPackRegistries;

    /**
     * 线程安全的已注册序列化器集合。
     * <p>
     * Thread-safe set of registered serializers.
     */
    public static final Set<CodecRecipeSerializer<?>> registeredSerializer=Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * 使用指定的数据包注册表构造重载监听器。
     * <p>
     * Constructs a reload listener with the specified datapack registries.
     *
     * @param dataPackRegistries 可重载的服务端资源 / The reloadable server resources
     */
    public RecipeReloadListener(ReloadableServerResources dataPackRegistries) {
        this.dataPackRegistries = dataPackRegistries;
    }

    /**
     * 当资源管理器重载时调用，遍历所有已注册的序列化器并通知其更新配方。
     * <p>
     * Called when the resource manager reloads. Iterates over all registered serializers
     * and notifies them to update their recipes.
     *
     * @param resourceManager 资源管理器 / The resource manager
     */
    @Override
    public void onResourceManagerReload(@Nonnull ResourceManager resourceManager) {
    	Collection<Recipe<?>> recipes=dataPackRegistries.getRecipeManager().getRecipes();
        for(CodecRecipeSerializer<?> i:registeredSerializer) {
        	//i.updateRecipes(recipes);
        }
    }
}