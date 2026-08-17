/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/** Mayor's Seal tab for residential priority and guaranteed meals. */
public final class TownHousingTab extends TownManagerTab {
    public TownHousingTab(TownManagerScreen screen) { super(screen); }

    @Override
    public CIcons.CIcon getContentIcon() { return CIcons.getIcon(Items.RED_BED); }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.frostedheart.town_manager.housing_care");
    }

    @Override
    public void build(UILayer layer) {
        layer.add(new TownHousingPanel(layer, TownManagerScreen.CONTENT_X,
                TownManagerScreen.CONTENT_Y, screen::getTown));
    }
}
