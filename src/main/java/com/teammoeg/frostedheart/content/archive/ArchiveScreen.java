package com.teammoeg.frostedheart.content.archive;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.CUIScreen;
import com.teammoeg.chorda.client.cui.PrimaryLayer;

public final class ArchiveScreen extends PrimaryLayer {
    // Home / Category / SubCategory / Entry
    // category list1
    // -> sub category list1
    // -> sub category list2
    //    -> entry1
    //    -> entry2
    //    -> entry3
    // category list2

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
