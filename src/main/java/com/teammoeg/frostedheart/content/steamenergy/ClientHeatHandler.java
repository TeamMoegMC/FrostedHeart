package com.teammoeg.frostedheart.content.steamenergy;

import dev.ftb.mods.ftblibrary.ui.MenuScreenWrapper;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Collection;

import com.teammoeg.chorda.client.ClientUtils;

public class ClientHeatHandler {
	public static void loadEndPoint(Collection<HeatEndpoint> data) {
		AbstractContainerMenu c=ClientUtils.mc().player.containerMenu;
		if(c instanceof HeatStatContainer) {
			((HeatStatContainer)c).data=data;
			if(ClientUtils.mc().screen instanceof MenuScreenWrapper) {
				MenuScreenWrapper<?> msw=(MenuScreenWrapper<?>) ClientUtils.mc().screen;
				msw.getGui().refreshWidgets();
			}
		}
	}
}
