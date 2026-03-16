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

import com.teammoeg.chorda.util.struct.BitObserverList;

/**
 * Chorda 元事件注册表。用于在模组间协调注册时序等元操作。
 * <p>
 * Meta-event registry for Chorda. Used to coordinate meta-operations
 * such as registration timing across mods.
 */
public class ChordaMetaEvents {
	/**
	 * Immersive Engineering 注册事件的观察者列表，
	 * 用于在 IE 注册完成时通知相关监听器。
	 * <p>
	 * Observer list for Immersive Engineering registry events,
	 * used to notify listeners when IE registration is complete.
	 */
	public static final BitObserverList IE_REGISTRY=new BitObserverList();
	public ChordaMetaEvents() {
	}

}
