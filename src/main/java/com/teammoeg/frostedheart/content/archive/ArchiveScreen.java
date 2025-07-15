package com.teammoeg.frostedheart.content.archive;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.CUIScreen;
import com.teammoeg.chorda.client.cui.PrimaryLayer;

public final class ArchiveScreen extends PrimaryLayer {
    public static String path;
    public final DetailBox detailBox;
    public final CategoryBox categoryBox;

    public ArchiveScreen() {
        this.detailBox = new DetailBox(this);
        this.categoryBox = new CategoryBox(this, detailBox);
    }

    @Override
    public void addUIElements() {
        add(detailBox);
        add(categoryBox);
        add(detailBox.scrollBar);
        add(categoryBox.scrollBar);
    }

    public static void open() {
        var config = new CUIScreen(new ArchiveScreen());
        ClientUtils.getMc().setScreen(config);
    }
}
