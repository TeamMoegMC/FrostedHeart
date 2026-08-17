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
