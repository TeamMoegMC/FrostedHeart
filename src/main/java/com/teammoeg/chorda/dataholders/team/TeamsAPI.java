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

package com.teammoeg.chorda.dataholders.team;

/**
 * 团队 API 的入口点。提供团队系统的全局访问接口，
 * 默认使用 {@link SinglePlayerTeamAPIProvider}，可通过 {@link #register} 注册其他实现（如 FTB Teams）。
 * <p>
 * Entry point for the Teams API. Provides global access to the team system,
 * defaulting to {@link SinglePlayerTeamAPIProvider}. Other implementations (e.g., FTB Teams) can be registered via {@link #register}.
 */
public class TeamsAPI {
	private static TeamsAPIProvider provider=new SinglePlayerTeamAPIProvider();
	private TeamsAPI() {
	}
	/**
	 * 获取当前注册的团队 API 提供者。
	 * <p>
	 * Gets the currently registered team API provider.
	 *
	 * @return 团队 API 提供者 / the team API provider
	 */
	public static TeamsAPIProvider getAPI() {
		return provider;
	}
	/**
	 * 注册一个新的团队 API 提供者，替换当前实现。
	 * <p>
	 * Registers a new team API provider, replacing the current implementation.
	 *
	 * @param prov 要注册的团队 API 提供者 / the team API provider to register
	 */
	public static void register(TeamsAPIProvider prov) {
		provider=prov;
	}
}
