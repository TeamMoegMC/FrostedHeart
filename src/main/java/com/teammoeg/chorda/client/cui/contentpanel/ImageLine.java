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

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.CUIDebugHelper;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.UV;
import com.teammoeg.chorda.math.Colors;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.Size2i;

/**
 * 图片行，用于在内容面板中显示纹理图片。
 * 自动从资源位置加载图片尺寸，支持自适应缩放和UV覆盖。
 * 当图片加载失败时显示损坏图标。
 * <p>
 * Image line for displaying texture images in a content panel.
 * Automatically loads image dimensions from resource location, supports adaptive
 * scaling and UV override. Shows a broken image icon when loading fails.
 */
@Getter
public class ImageLine extends Line<ImageLine> {
    protected ResourceLocation imgLocation;
    protected Size2i imgSize;
    protected UV imgUV;
    protected UV overrideUV;
    @Setter
    protected int blitColor = Colors.WHITE;
    protected int imgX, imgY;

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
    public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        super.render(graphics, x, y, w, h, hint);
        // 图片无效时显示错误图标
        if (!isImgValid()) {
            FlatIcon.FILE_IMG_BROKEN.render(graphics.pose(), x+w/2-5, y+1, hint.theme(this).errorColor());
            return;
        }
        // 计算图片位置
        imgX = switch (alignment) {
            case CENTER -> x + getWidth() / 2 - imgUV.getW() / 2;
            case RIGHT ->  x + getWidth() - imgUV.getW() - 2;
            default -> x+2;
        };
        imgY = y + h/2 - imgUV.getH()/2;
        // 渲染图片
        imgUV.blitColored(graphics, imgLocation, imgX, imgY, blitColor);
    }

    @Override
    public void renderDebug(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint, int depth) {
        super.renderDebug(graphics, x, y, w, h, hint, depth);
        graphics.drawString(getFont(), imgUV.toString(), imgX, imgY-8, -1);
        CGuiHelper.drawBox(graphics, imgX, imgY, imgUV.getW(), imgUV.getH(), CUIDebugHelper.getDepthColor(depth), true);
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        if (imgSize.width < getWidth()/3) {
            graphics.fill(x, y, x+w, y+h, hint.theme(this).UIBGBorderColor());
        }
    }

    public boolean isImgValid() {
        return (imgUV != null || overrideUV != null) && imgSize != null && imgSize.height > 0 && imgSize.width > 0;
    }

    public ImageLine setImage(ResourceLocation imageLocation) {
        this.imgLocation = imageLocation;
        imgSize = CGuiHelper.getImgSize(imageLocation);
        imgUV = new UV(0, 0, 0, 0);
        refresh();
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
