package com.teammoeg.frostedheart.content.trade;

import java.util.Collection;

import com.teammoeg.frostedheart.content.steamenergy.EndPointData;
import com.teammoeg.frostedheart.content.steamenergy.HeatStatContainer;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import dev.ftb.mods.ftblibrary.ui.MenuScreenWrapper;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class ClientHeatHandler {
	public static void loadEndPoint(Collection<EndPointData> data) {
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
