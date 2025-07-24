package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.UV;
import com.teammoeg.frostedheart.content.archive.Alignment;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.Size2i;

@Getter
public class ImageLine extends Line<ImageLine> {
    protected ResourceLocation imgLocation;
    protected Size2i imgSize;
    protected UV imgUV;
    protected int backgroundColor = 0;

    public ImageLine(UIWidget parent, ResourceLocation imageLocation, Alignment alignment) {
        super(parent, alignment);
        setImage(imageLocation);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        super.render(graphics, x, y, w, h);
        if (!isImgValid()) {
            graphics.drawCenteredString(getFont(), Component.literal(imgLocation.toString()).withStyle(ChatFormatting.RED),
                    x+w/2, y+2, color);
            return;
        }

        if (backgroundColor != 0) {
            graphics.fill(x, y, x+w, y+h, -1, backgroundColor);
        }

        int imgX = switch (alignment) {
            case CENTER -> x + getWidth() / 2 - imgUV.getW() / 2;
            case RIGHT ->  x + getWidth() - imgUV.getW() - 2;
            default -> x+2;
        };
        imgUV.blit(graphics, imgLocation, imgX, y + h/2 - imgUV.getH()/2);
    }

    public boolean isImgValid() {
        return imgUV != null && imgSize != null && imgSize.height + imgSize.width > 0;
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
        if (isImgValid()) {
            int w = imgSize.width;
            int h = imgSize.height;
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
            return;
        }
        setSize(parent.getWidth(), DEF_LINE_HEIGHT);
    }
}
