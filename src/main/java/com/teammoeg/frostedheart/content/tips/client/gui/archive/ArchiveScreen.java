package com.teammoeg.frostedheart.content.tips.client.gui.archive;

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
}
