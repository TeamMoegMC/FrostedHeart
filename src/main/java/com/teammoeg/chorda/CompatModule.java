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

import net.minecraftforge.fml.ModList;
/**
 * 兼容模组注册表。集中管理所有兼容模组的加载状态检测，
 * 避免在代码各处硬编码模组 ID。
 * <p>
 * Compatibility module registry. Centrally manages load-status detection
 * for all compatible mods, preventing hardcoded mod IDs throughout the codebase.
 */
public class CompatModule {
	/** @return Curios 模组是否已加载 / whether the Curios mod is loaded */
	public static boolean isCuriosLoaded() {
		return curiosLoaded;
	}
	/** @return Tetra 模组是否已加载 / whether the Tetra mod is loaded */
	public static boolean isTetraLoaded() {
		return tetraLoaded;
	}
	/** @return Create 模组是否已加载 / whether the Create mod is loaded */
	public static boolean isCreateLoaded() {
		return createLoaded;
	}
	/** @return Charcoal Pit 模组是否已加载 / whether the Charcoal Pit mod is loaded */
	public static boolean isCharcoalPitLoaded() {
		return charcoalPitLoaded;
	}
	/** @return FTB Quests 模组是否已加载 / whether the FTB Quests mod is loaded */
	public static boolean isFTBQLoaded() {
		return FTBQLoaded;
	}
	/** @return FTB Teams 模组是否已加载 / whether the FTB Teams mod is loaded */
	public static boolean isFTBTLoaded() {
		return FTBTLoaded;
	}
	/** @return LdLib 模组是否已加载 / whether the LdLib mod is loaded */
	public static boolean isLdLibLoaded() {
		return LdLibLoaded;
	}
	/** @return Immersive Engineering 模组是否已加载 / whether the IE mod is loaded */
	public static boolean isIELoaded() {
		return IELoaded;
	}
	/** @return Caupona 模组是否已加载 / whether the Caupona mod is loaded */
	public static boolean isCauponaLoaded() {
		return cauponaLoaded;
	}
	/** @return Stone Age 模组是否已加载 / whether the Stone Age mod is loaded */
	public static boolean isStoneAgeLoaded() {
		return stoneAgeLoaded;
	}
	private static boolean curiosLoaded;
	private static boolean tetraLoaded;
	private static boolean createLoaded;
	private static boolean charcoalPitLoaded;
	private static boolean FTBQLoaded;
	private static boolean FTBTLoaded;
	private static boolean LdLibLoaded;
	private static boolean IELoaded;
	private static boolean cauponaLoaded;
	private static boolean stoneAgeLoaded;
	private static boolean isModlistLoaded;
	private static boolean jeiLoaded;

	/**
	 * 启用兼容模块。在模组初始化时调用，检测所有兼容模组的加载状态。
	 * 使用双重检查锁定确保线程安全且只执行一次。
	 * <p>
	 * Enables the compat module. Call at mod initialization to detect
	 * the load status of all compatible mods. Uses double-checked locking
	 * to ensure thread safety and single execution.
	 */
	public static void enableCompatModule() {
		if(!isModlistLoaded) {
			synchronized(CompatModule.class) {
				if(!isModlistLoaded) {
					refreshLoadedStatus();
					isModlistLoaded=true;
				}
			}
		}
	}
	/**
	 * 刷新所有兼容模组的加载状态。
	 * <p>
	 * Refreshes the loaded status of all compatible mods.
	 */
	private static void refreshLoadedStatus() {
		curiosLoaded=ModList.get().isLoaded("curios");
		tetraLoaded=ModList.get().isLoaded("tetra");
		createLoaded=ModList.get().isLoaded("create");
		charcoalPitLoaded=ModList.get().isLoaded("charcoal_pit");
		FTBQLoaded=ModList.get().isLoaded("ftbquests");
		LdLibLoaded=ModList.get().isLoaded("ldlib");
		IELoaded=ModList.get().isLoaded("immersiveengineering");
		FTBTLoaded=ModList.get().isLoaded("ftbteams");
		cauponaLoaded=ModList.get().isLoaded("caupona");
		stoneAgeLoaded=ModList.get().isLoaded("stone_age");
		jeiLoaded=ModList.get().isLoaded("jei");
	}
	public static boolean isJeiLoaded() {
		return jeiLoaded;
	}

}
