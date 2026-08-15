/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/** Mayor's Seal tab for the player-authored work-building staffing queue. */
public final class TownStaffingTab extends TownManagerTab {
    public TownStaffingTab(TownManagerScreen screen) {
        super(screen);
    }

    @Override
    public CIcons.CIcon getContentIcon() {
        return CIcons.getIcon(Items.IRON_PICKAXE);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.frostedheart.town_manager.staffing");
    }

    @Override
    public void build(UILayer layer) {
        layer.add(new TownStaffingPanel(
                layer,
                TownManagerScreen.CONTENT_X,
                TownManagerScreen.CONTENT_Y,
                screen::getTown));
    }
}
