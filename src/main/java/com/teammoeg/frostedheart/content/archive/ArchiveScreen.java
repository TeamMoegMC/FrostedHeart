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

package com.teammoeg.frostedheart.content.archive;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.contentpanel.ContentPanel;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreenWrapper;

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
        ClientUtils.getMc().setScreen(new CUIScreenWrapper(layer));
    }
}
