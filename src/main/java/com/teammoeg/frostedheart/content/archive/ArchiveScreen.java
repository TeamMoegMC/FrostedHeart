package com.teammoeg.frostedheart.content.archive;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.CUIScreen;
import com.teammoeg.chorda.client.cui.PrimaryLayer;
import com.teammoeg.chorda.client.cui.contentpanel.ContentPanel;

public final class ArchiveScreen extends PrimaryLayer {
    public static String path;
    public final ContentPanel contentPanel;
    public final CategoryBox categoryBox;

    public ArchiveScreen() {
        this.contentPanel = new ContentPanel(this);
        this.categoryBox = new CategoryBox(this, contentPanel);
    }

    @Override
    public void addUIElements() {
        add(contentPanel);
        add(categoryBox);
        add(contentPanel.scrollBar);
        add(categoryBox.scrollBar);
    }

    public static void open() {
        var config = new CUIScreen(new ArchiveScreen());
        ClientUtils.getMc().setScreen(config);
    }
}
