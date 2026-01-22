package com.teammoeg.frostedheart.content.town;

import com.teammoeg.chorda.client.cui.TextField;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.frostedresearch.Lang;

public class AbstractTownWorkerBlockUILayer extends UILayer implements AbstractTownWorkerBlockScreen.ITabContent {
    AbstractTownWorkerBlockScreen panel;

    public AbstractTownWorkerBlockUILayer(AbstractTownWorkerBlockScreen panel) {
        super(panel);
        this.panel = panel;
    }

    @Override
    public void addUIElements() {
        TextField tf = new TextField(this);
        tf.setMaxWidth(71).setMaxLines(2).setPos(40, 15);
            tf.setText(Lang.translateGui("no_active_research"));
        add(tf);
    }

    @Override
    public void alignWidgets() {

    }

    @Override
    public void renderTabContent(UILayer layern) {

    }
}
