package com.teammoeg.frostedheart.content.archive;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.CUIScreen;
import com.teammoeg.chorda.client.cui.PrimaryLayer;
import com.teammoeg.chorda.client.cui.contentpanel.ContentPanel;

import javax.annotation.Nullable;

public final class ArchiveScreen extends PrimaryLayer {
    public static String path;
    public final ContentPanel contentPanel;
    public final ArchiveCategory category;

    public ArchiveScreen() {
        this.contentPanel = new ContentPanel(this) {
            @Override
            public void resize() {
                int h = (int)(ClientUtils.screenHeight() * 0.8F);
                int w = (int)(h * 1.3333F); // 4:3
                setPosAndSize(120, 0, w, h);
                super.resize();
            }
        };
        this.category = new ArchiveCategory(this, contentPanel);
    }

    public ArchiveScreen(String path) {
        this();
        if (path != null) {
            ArchiveCategory.currentPath = path;
        }
    }

    @Override
    public void addUIElements() {
        add(contentPanel);
        add(category);
        add(contentPanel.scrollBar);
        add(category.scrollBar);
    }

    public static void open(@Nullable String path) {
        var layer = new ArchiveScreen(path);
        ClientUtils.getMc().setScreen(new CUIScreen(layer));
    }
}
