package com.teammoeg.frostedheart.compat;

import lombok.Getter;
import net.minecraftforge.fml.ModList;
/**
 * Registry for all mods we have a compat with, prevents writing modids everywhere
 * */
public class CompatModule {
	@Getter
	private static boolean curiosLoaded;
	@Getter
	private static boolean tetraLoaded;
	@Getter
	private static boolean createLoaded;
	@Getter
	private static boolean charcoalPitLoaded;
	@Getter
	private static boolean FTBQLoaded;
	@Getter
	private static boolean FTBTLoaded;
	@Getter
	private static boolean LdLibLoaded;
	@Getter
	private static boolean IELoaded;
	@Getter
	private static boolean cauponaLoaded;
	public static void refreshLoadedStatus() {
		curiosLoaded=ModList.get().isLoaded("curios");
		tetraLoaded=ModList.get().isLoaded("tetra");
		createLoaded=ModList.get().isLoaded("create");
		charcoalPitLoaded=ModList.get().isLoaded("charcoal_pit");
		FTBQLoaded=ModList.get().isLoaded("ftbquests");
		LdLibLoaded=ModList.get().isLoaded("ldlib");
		IELoaded=ModList.get().isLoaded("immersiveengineering");
		FTBTLoaded=ModList.get().isLoaded("ftbteams");
		cauponaLoaded=ModList.get().isLoaded("caupona");
	}
}
