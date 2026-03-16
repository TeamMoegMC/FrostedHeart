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

import com.teammoeg.chorda.network.ContainerDataSyncMessageS2C;
import com.teammoeg.chorda.network.ContainerOperationMessageC2S;
import com.teammoeg.chorda.network.CBaseNetwork;

/**
 * Chorda 网络通道管理类。注册并管理客户端与服务端之间的网络消息。
 * <p>
 * Network channel manager for Chorda. Registers and manages
 * network messages between client and server.
 */
public class ChordaNetwork extends CBaseNetwork{
	/** 单例实例 / Singleton instance */
	public static final ChordaNetwork INSTANCE=new ChordaNetwork();

    private ChordaNetwork() {
		super(Chorda.MODID);
	}

    /**
     * 注册所有网络消息类型。包括容器操作和容器数据同步消息。
     * <p>
     * Registers all network message types. Includes container operation
     * and container data synchronization messages.
     */
    @Override
	public void registerMessages() {
        // 基础消息 / Fundamental messages
        registerMessage("container_operation", ContainerOperationMessageC2S.class);
        registerMessage("container_sync", ContainerDataSyncMessageS2C.class);

    }
}
