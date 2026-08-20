/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/** Mayor's Seal tab for town-owned virtual resources and service capacity. */
public final class TownVirtualResourcesTab extends TownManagerTab {
    public TownVirtualResourcesTab(TownManagerScreen screen) {
        super(screen);
    }

    @Override
    public CIcons.CIcon getContentIcon() {
        return CIcons.getIcon(Items.ENDER_CHEST);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.frostedheart.town_manager.virtual_resources");
    }

    @Override
    public void build(UILayer layer) {
        layer.add(new TownVirtualResourcesPanel(
                layer,
                TownManagerScreen.CONTENT_X,
                TownManagerScreen.CONTENT_Y,
                screen::getTown));
    }
}
