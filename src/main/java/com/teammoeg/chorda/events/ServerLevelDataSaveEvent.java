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

package com.teammoeg.chorda.events;

import net.minecraftforge.eventbus.api.Event;

/**
 * 当服务端世界数据即将保存时触发的事件。
 * 用于通知数据持有者在世界保存时一并保存自定义数据。
 * <p>
 * Event fired when server level data is about to be saved.
 * Used to notify data holders to save their custom data alongside world saves.
 */
public class ServerLevelDataSaveEvent extends Event {

	/**
	 * 创建服务端世界数据保存事件。
	 * <p>
	 * Creates a server level data save event.
	 */
	public ServerLevelDataSaveEvent() {

	}

}
