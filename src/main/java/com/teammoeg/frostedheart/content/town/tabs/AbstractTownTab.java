package com.teammoeg.frostedheart.content.town.tabs;

import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockEntity;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen;

public abstract class AbstractTownTab<C extends CBlockEntityMenu<? extends AbstractTownWorkerBlockEntity>> {
    protected final AbstractTownWorkerBlockScreen<C> screen;

    public AbstractTownTab(AbstractTownWorkerBlockScreen<C> screen) {
        this.screen = screen;
    }

    public abstract CIcons.CIcon getIcon();

    public abstract CIcons.CIcon getActiveIcon();

    public abstract void build(UILayer layer);

    protected C getMenu() {
        return screen.getCBEMenu();
    }

}
