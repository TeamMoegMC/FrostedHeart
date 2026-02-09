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

package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.UV;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.frostedheart.content.archive.Alignment;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.Size2i;

@Getter
public class ImageLine extends Line<ImageLine> {
    protected ResourceLocation imgLocation;
    protected Size2i imgSize;
    protected UV imgUV;
    protected UV overrideUV;
    protected int backgroundColor = 0;

    public ImageLine(UIElement parent, ResourceLocation imageLocation, Alignment alignment) {
        super(parent, alignment);
        setImage(imageLocation);
    }

    public ImageLine uvOverride(UV uv) {
        imgUV = overrideUV = uv;
        imgSize = new Size2i(uv.getW(), uv.getH());
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        super.render(graphics, x, y, w, h);
        if (!isImgValid()) {
            FlatIcon.FILE_IMG_BROKEN.render(graphics.pose(), x+w/2-5, y+1, Colors.RED);
            return;
        }

        if (backgroundColor != 0) {
            graphics.fill(x, y, x+w, y+h, backgroundColor);
        }

        int imgX = switch (alignment) {
            case CENTER -> x + getWidth() / 2 - imgUV.getW() / 2;
            case RIGHT ->  x + getWidth() - imgUV.getW() - 2;
            default -> x+2;
        };
        imgUV.blit(graphics, imgLocation, imgX, y + h/2 - imgUV.getH()/2);
    }

    public boolean isImgValid() {
        return (imgUV != null || overrideUV != null) && imgSize != null && imgSize.height + imgSize.width > 0;
    }

    public ImageLine setImage(ResourceLocation imageLocation) {
        this.imgLocation = imageLocation;
        imgSize = CGuiHelper.getImgSize(imageLocation);
        imgUV = new UV(0, 0, 0, 0);
        refresh();
        return this;
    }

    public ImageLine bgColor(int color) {
        this.backgroundColor = color;
        return this;
    }

    @Override
    public void refresh() {
        setSize(parent.getWidth(), DEF_LINE_HEIGHT);
        if (isImgValid()) {
            int w = imgSize.width;
            int h = imgSize.height;
            if (overrideUV != null) {
                imgUV = overrideUV;
                setHeight(h+6);
                return;
            }
            if (w > 32 || h > 32) {
                w /= 2;
                h /= 2;
            }
            if (w > getWidth()) {
                w = getWidth();
                h = Math.round(h * (w / (imgSize.width * 0.5F)));
            }
            setHeight(h+6);
            imgUV = new UV(0, 0, w, h, w, h);
        }
    }

    @Override
    public void getTooltip(TooltipBuilder list) {
        super.getTooltip(list);
        list.accept(getTitle());
    }

    @Override
    public boolean hasTooltip() {
        return super.hasTooltip() && !isImgValid();
    }

    @Override
    public Component getTitle() {
        return Component.literal(imgLocation.toString());
    }
}
