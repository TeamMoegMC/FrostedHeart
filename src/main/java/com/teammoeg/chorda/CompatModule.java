package com.teammoeg.chorda;

import lombok.Getter;
import net.minecraftforge.fml.ModList;
/**
 * Registry for all mods we have a compat with, prevents writing modids everywhere
 * */
public class CompatModule {
	public static boolean isCuriosLoaded() {
		return curiosLoaded;
	}
	public static boolean isTetraLoaded() {
		return tetraLoaded;
	}
	public static boolean isCreateLoaded() {
		return createLoaded;
	}
	public static boolean isCharcoalPitLoaded() {
		return charcoalPitLoaded;
	}
	public static boolean isFTBQLoaded() {
		return FTBQLoaded;
	}
	public static boolean isFTBTLoaded() {
		return FTBTLoaded;
	}
	public static boolean isLdLibLoaded() {
		return LdLibLoaded;
	}
	public static boolean isIELoaded() {
		return IELoaded;
	}
	public static boolean isCauponaLoaded() {
		return cauponaLoaded;
	}
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

	/**
	 * Call this method at the start of your mod to enable compat module
	 * */
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
	}

}
