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

package com.teammoeg.chorda.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.chorda.events.ServerLevelDataSaveEvent;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

/**
 * MinecraftServer的Mixin，在保存所有区块时触发ServerLevelDataSaveEvent事件。
 * 使自定义数据持有者能够在世界保存时一并保存自己的数据。
 * <p>
 * Mixin for MinecraftServer that fires ServerLevelDataSaveEvent when saving all chunks.
 * Enables custom data holders to save their data alongside world saves.
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

	public MinecraftServerMixin() {

	}

	/**
	 * 在saveAllChunks方法头部注入，发布ServerLevelDataSaveEvent事件。
	 * <p>
	 * Injects at the head of saveAllChunks to post a ServerLevelDataSaveEvent.
	 *
	 * @param pSuppressLog 是否抑制日志 / Whether to suppress logging
	 * @param pFlush 是否刷新 / Whether to flush
	 * @param pForced 是否强制保存 / Whether the save is forced
	 * @param ret 回调信息 / The callback info
	 */
	@Inject(at=@At("HEAD"),method="saveAllChunks")
	public void saveAllChunks(boolean pSuppressLog, boolean pFlush, boolean pForced,CallbackInfoReturnable<Boolean> ret) {
		MinecraftForge.EVENT_BUS.post(new ServerLevelDataSaveEvent());
	}

}
