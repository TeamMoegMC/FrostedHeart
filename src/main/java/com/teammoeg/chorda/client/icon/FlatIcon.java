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

package com.teammoeg.chorda.client.icon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.math.Colors;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.Size2i;

public enum FlatIcon {
    MOUSE_LEFT      (0 , 0 , "\uF200"),
    MOUSE_RIGHT     (10, 0 , "\uF201"),
    MOUSE_MIDDLE    (20, 0 , "\uF202"),
    SIGHT           (30, 0 , "\uF203"),
    INFO            (40, 0 , "\uF204"),
    SORT            (50, 0 , "\uF205"),
    FILTER          (60, 0 , "\uF206"),
    PIN             (70, 0 , "\uF207"),

    QUESTION_MARK   (0 , 10, "\uF210"),
    LOCK            (10, 10, "\uF211"),
    CONTINUE        (20, 10, "\uF212"),
    FORBID          (30, 10, "\uF213"),
    RIGHT           (40, 10, "\uF214"),
    DOWN            (50, 10, "\uF215"),
    LEFT            (60, 10, "\uF216"),
    TOP             (70, 10, "\uF217"),

    TRADE           (0 , 20, "\uF220"),
    GIVE            (10, 20, "\uF221"),
    GAIN            (20, 20, "\uF222"),
    LEAVE           (30, 20, "\uF223"),
    JUMP            (40, 20, "\uF224"),
    JUMP_IN         (50, 20, "\uF225"),
    UNUSED_R3C7     (60, 20, "\uF226"),
    UNUSED_R3C8     (70, 20, "\uF227"),

    BOX             (0 , 30, "\uF230"),
    BOX_ON          (10, 30, "\uF231"),
    CROSS           (20, 30, "\uF232"),
    HISTORY         (30, 30, "\uF233"),
    LIST            (40, 30, "\uF234"),
    TRASH_CAN       (50, 30, "\uF235"),
    CHECK           (60, 30, "\uF236"),
    FOLDER          (70, 30, "\uF237"),

    LEFT_SLIDE      (0 , 40, "\uF240"),
    UNLOCK          (10, 40, "\uF241"),
    BLOCK           (20, 40, "\uF242"),
    LIQUID          (30, 40, "\uF243"),
    FILE            (40, 40, "\uF244"),
    FILE_TXT        (50, 40, "\uF245"),
    FILE_IMG        (60, 40, "\uF246"),
    FILE_IMG_BROKEN (70, 40, "\uF247"),

    RIGHT_SLIDE     (0 , 50, "\uF250"),
    UNUSED_R6C2     (10, 50, "\uF251"),
    UNUSED_R6C3     (20, 50, "\uF252"),
    UNUSED_R6C4     (30, 50, "\uF253"),
    UNUSED_R6C5     (40, 50, "\uF254"),
    UNUSED_R6C6     (50, 50, "\uF255"),
    UNUSED_R6C7     (60, 50, "\uF256"),
    UNUSED_R6C8     (70, 50, "\uF257"),

    UNUSED_R7C1     (0 , 60, "\uF260"),
    UNUSED_R7C2     (10, 60, "\uF261"),
    UNUSED_R7C3     (20, 60, "\uF262"),
    UNUSED_R7C4     (30, 60, "\uF263"),
    UNUSED_R7C5     (40, 60, "\uF264"),
    UNUSED_R7C6     (50, 60, "\uF265"),
    UNUSED_R7C7     (60, 60, "\uF266"),
    UNUSED_R7C8     (70, 60, "\uF267"),

    WRENCH          (0 , 70, "\uF270"),
    CONFIG          (10, 70, "\uF271"),
    UNUSED_R8C3     (20, 70, "\uF272"),
    UNUSED_R8C4     (30, 70, "\uF273"),
    UNUSED_R8C5     (40, 70, "\uF274"),
    UNUSED_R8C6     (50, 70, "\uF275"),
    UNUSED_R8C7     (60, 70, "\uF276"),
    UNUSED_R8C8     (70, 70, "\uF277");

    public static final ResourceLocation ICON_LOCATION = Chorda.rl("textures/gui/hud/flat_icon_10x.png");
    public static final ResourceLocation FONT_LOCATION = Chorda.rl("default");
    public static final int TEXTURE_HEIGHT = 80;
    public static final int TEXTURE_WIDTH = 80;

    public final int x;
    public final int y;
    public final String chr;
    public final Size2i size;

    FlatIcon(int x, int y, String chr, Size2i size) {
        this.x = x;
        this.y = y;
        this.chr = chr;
        this.size = size;
    }

    FlatIcon(int x, int y, String chr) {
        this.x = x;
        this.y = y;
        this.chr = chr;
        this.size = new Size2i(10, 10);
    }

    public void render(PoseStack pose, int x, int y, int color) {
        CGuiHelper.bindTexture(ICON_LOCATION);
        CGuiHelper.blitColored(pose, x, y, this.size.width, this.size.height, this.x, this.y, this.size.width, this.size.height, TEXTURE_WIDTH, TEXTURE_HEIGHT, color);
    }

    public static void render(FlatIcon icon, PoseStack pose, int x, int y, int color) {
        icon.render(pose, x, y, color);
    }

    public FlatIconWidget toWidget(UIElement parent) {
        var widget = new FlatIconWidget(parent);
        widget.setIcon(this);
        return widget;
    }

    public FlatIconWidget toWidget(UIElement parent, int color) {
        var widget = new FlatIconWidget(parent, color);
        widget.setIcon(this);
        return widget;
    }

    @Getter
    @Setter
    public static class FlatIconWidget extends UIElement {
        protected FlatIcon icon;
        protected int color;

        public FlatIconWidget(UIElement parent) {
            this(parent, Colors.WHITE);
        }

        public FlatIconWidget(UIElement parent, int color) {
            super(parent);
            this.color = color;
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h) {
            if (icon != null) {
                icon.render(graphics.pose(), x, y, color);
            }
        }

        @Override
        public boolean isVisible() {
            return hasIcon() && super.isVisible();
        }

        public boolean hasIcon() {
            return this.icon != null;
        }
    }

    private CIcons.CIcon cIconCache;
    public CIcons.CIcon toCIcon() {
        if (cIconCache == null)
            cIconCache = CIcons.getIcon(ICON_LOCATION, x, y, size.width, size.height, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        return cIconCache;
    }

    private Component cTextIconCache;
    public Component toCTextIcon(){
        if (cTextIconCache == null) {
            cTextIconCache = Component.literal(chr).withStyle(s -> s.withFont(FONT_LOCATION));
        }
        return cTextIconCache;
    }
}
