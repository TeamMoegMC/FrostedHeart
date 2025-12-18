package com.teammoeg.frostedheart.item.townitem;

import com.teammoeg.frostedheart.content.town.TeamTown;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MayorScreen extends Screen {
    TeamTown town;
    int activeTab = 0;
    public MayorScreen(Component pTitle) {
        super(pTitle);
        this.town = TeamTown.fromLocal();
    }
}
