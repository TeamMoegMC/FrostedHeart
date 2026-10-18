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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/** Mayor's Seal tab for mutually exclusive town-law domains. */
public final class TownPoliciesTab extends TownManagerTab {
    public TownPoliciesTab(TownManagerScreen screen) { super(screen); }

    @Override
    public CIcons.CIcon getContentIcon() { return CIcons.getIcon(Items.WRITABLE_BOOK); }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.frostedheart.town_manager.policies");
    }

    @Override
    public void build(UILayer layer) {
        layer.add(new TownPoliciesPanel(layer, TownManagerScreen.CONTENT_X,
                TownManagerScreen.CONTENT_Y, screen::getTown));
    }
}
