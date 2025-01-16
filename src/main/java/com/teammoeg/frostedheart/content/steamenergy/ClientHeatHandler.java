package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.chorda.util.client.ClientUtils;
import dev.ftb.mods.ftblibrary.ui.MenuScreenWrapper;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Collection;

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
