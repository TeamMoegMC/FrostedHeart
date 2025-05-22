package com.teammoeg.frostedheart.content.tips.client.gui.archive;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.StringTextComponentParser;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.ScrollBar;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.ui.UV;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.common.util.Size2i;

import java.util.ArrayList;
import java.util.List;

public class DetailBox extends Layer {
    public List<Line> lines = new ArrayList<>();
    public final ScrollBar scrollBar;

    public DetailBox(UIWidget panel) {
        super(panel);
        this.scrollBar = new LayerScrollBar(parent, true, this);
    }

    public void fillContent(Line... lines) {
        for (Line line : lines) {
            add(line);
        }
        refresh();
    }

    @Override
    public void refresh() {
        int h = (int)(ClientUtils.screenHeight() * 0.8F);
        int w = (int)(h * 1.3333F);
        setSize(w, h); // 4:3
        setPos(0, 0);

//        setScissorEnabled(false);

        recalcContentSize();
        addUIElements();
        for (UIWidget element : elements) {
            element.refresh();
        }
        alignWidgets();
        scrollBar.setPosAndSize(w+8, -7, 6, h+15);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
//        float a = AnimationUtil.fadeIn(3000, "test", true);
//        float px = x + (float) w / 2;
//        float py = y + (float) h / 2;
//        pose.rotateAround(new Quaternionf().rotateX(-0.2F), px, py, 0);
//        pose.rotateAround(new Quaternionf().rotateY(0.2F), px, py, 0);
//        pose.translate(0, 0, -a*10);
//        RenderSystem.enableBlend();
//        RenderSystem.setShaderColor(1, 1, 1, a);
        super.render(graphics, x, y, w, h);
//        RenderSystem.disableBlend();
        pose.popPose();
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        int border = 8;
        graphics.fill(x-border, y-border, x+w+border+6, y+h+border, -2, 0xFF444651);
        graphics.fill(x-border+1, y-border+1, x+w+border+1+6, y-border, -2, 0xFF585966);
        graphics.fill(x-border+1, y-border, x-border, y+h+border, -2, 0xFF585966);
        graphics.fill(x+w+border+6, y-border, x+w+border+1+6, y+h+border, -2, 0xFF585966);
        graphics.fill(x-border+1, y+h+border, x+w+border+1+6, y+h+border+1, -2, 0xFF585966);
    }

    @Override
    public void addUIElements() {
//        add(scrollBar);
    }

    @Override
    public void alignWidgets() {
        align(false);
    }


    public static class TextLine extends Line {
        @Getter
        protected Component text;
        protected final List<FormattedCharSequence> splitText = new ArrayList<>();
        protected int backgroundColor = 0;
        protected int trimmingColor = 0;
        protected boolean isTitle = false;
        protected boolean isQuote = false;
        protected boolean shadow = false;
        protected int scale = 1;

        public TextLine(UIWidget parent, Alignment alignment, Component text) {
            super(parent, alignment);
            this.text = text;
        }

        public static TextLine of(UIWidget parent, String text) {
            return new TextLine(parent, Alignment.LEFT, Component.literal(text));
        }

        public static TextLine of(UIWidget parent, Alignment alignment, String text) {
            return new TextLine(parent, alignment, Component.literal(text));
        }

        public static TextLine of(UIWidget parent, Component text) {
            return new TextLine(parent, Alignment.LEFT, text);
        }

        public static TextLine of(UIWidget parent, Alignment alignment, Component text) {
            return new TextLine(parent, alignment, text);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h) {
            var pose = graphics.pose();
            pose.pushPose();

            if (scale > 1) {
                pose.translate(-x, -y, 0);
                pose.scale(scale, scale, 1);
                w /= scale;
                h /= scale;
            }

            if (isTitle) {
                graphics.fill(x, y, x+w, y+h-2, -1, trimmingColor);
                pose.translate(2, 2, 0);

            } else if (isQuote) {
                graphics.fill(x, y, x+4, y+h-3, -1, trimmingColor);
                pose.translate(8, 2, 0);
            }

            CGuiHelper.drawStringLinesInBound(graphics, getFont(), splitText, x, y, w, baseColor, DEF_LINE_HEIGHT,
                    shadow, backgroundColor, alignment);

            pose.popPose();
        }

        public TextLine setText(Component text) {
            this.text = text;
            refresh();
            return this;
        }

        public TextLine setScale(int scale) {
            this.scale = Math.max(1, scale);
            refresh();
            return this;
        }

        public TextLine setBackgroundColor(int color) {
            this.backgroundColor = color;
            return this;
        }

        public TextLine setTitle(boolean isTitle, int trimmingColor, int scale) {
            this.isTitle = isTitle;
            this.trimmingColor = trimmingColor;
            setScale(scale);
            return this;
        }

        public TextLine setQuote(boolean isQuote, int trimmingColor) {
            this.isQuote = isQuote;
            this.baseColor = trimmingColor;
            this.trimmingColor = trimmingColor;
            return this;
        }

        public TextLine setShadow(boolean shadow) {
            this.shadow = shadow;
            return this;
        }

        @Override
        public void refresh() {
            setWidth(parent.getWidth());
            var pre = text.getString().split("\\n");
            splitText.clear();
            for (String s : pre) {
                splitText.addAll(getFont().split(StringTextComponentParser.parse(s), (int)(getWidth() * (1.0F/scale))));
            }

            setHeight(splitText.size() * DEF_LINE_HEIGHT * scale + 4);
        }
    }

    public static class ImageLine extends Line {
        protected ResourceLocation imgLocation;
        protected Size2i imgSize;
        protected UV imgUV;
        protected int backgroundColor = 0;

        public ImageLine(UIWidget parent, Alignment alignment, ResourceLocation imageLocation) {
            super(parent, alignment);
            setImage(imageLocation);
        }

        public static ImageLine of(UIWidget parent, String imageLocation) {
            return new ImageLine(parent, Alignment.CENTER, ResourceLocation.tryParse(imageLocation));
        }

        public static ImageLine of(UIWidget parent, Alignment alignment, String imageLocation) {
            return new ImageLine(parent, alignment, ResourceLocation.tryParse(imageLocation));
        }

        public static ImageLine of(UIWidget parent, ResourceLocation imageLocation) {
            return new ImageLine(parent, Alignment.CENTER, imageLocation);
        }

        public static ImageLine of(UIWidget parent, Alignment alignment, ResourceLocation imageLocation) {
            return new ImageLine(parent, alignment, imageLocation);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h) {
            y -= 2;
            if (backgroundColor != 0) {
                int bgX = switch (alignment) {
                    case LEFT -> x-2;
                    case CENTER -> x - w/2 + imgSize.width/2;
                    case RIGHT -> x - w + imgSize.width + 2;
                };
                graphics.fill(bgX, y+1, bgX+w, y+h-1, -1, backgroundColor);
            }

            if (!isImgLocValid()) {
                graphics.drawCenteredString(getFont(), Component.literal(imgLocation.toString()).withStyle(ChatFormatting.RED),
                        x+w/2, y, baseColor);
                return;
            }
            imgUV.blit(graphics, imgLocation, x, y + h/2 - imgSize.height/2);
        }

        public boolean isImgLocValid() {
            return imgSize.height + imgSize.width > 0 && imgUV != null;
        }

        public ImageLine setImage(ResourceLocation imageLocation) {
            this.imgLocation = imageLocation;
            imgSize = ClientUtils.getImgSize(imageLocation);
            imgUV = new UV(0, 0, imgSize.width, imgSize.height, imgSize.width, imgSize.height);
            refresh();
            return this;
        }

        public ImageLine setBackgroundColor(int color) {
            this.backgroundColor = color;
            return this;
        }

        @Override
        public void refresh() {
            if (isImgLocValid()) {
                setSize(parent.getWidth(), imgSize.height+6);
                setX(switch (alignment) {
                    case CENTER -> getWidth() / 2 - imgSize.width / 2;
                    case RIGHT -> getWidth() - imgSize.width - 2;
                    default -> 2;
                });
                return;
            }
            setHeight(DEF_LINE_HEIGHT);
        }
    }

    public static class EmptyLine extends Line {

        public EmptyLine(UIWidget parent) {
            super(parent);
            setHeight(DEF_LINE_HEIGHT);
        }

        public static EmptyLine create(UIWidget parent) {
            return new EmptyLine(parent);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h) {

        }
    }

    public static abstract class Line extends UIWidget {
        public static final int DEF_LINE_HEIGHT = 12;
        protected Alignment alignment;
        protected int baseColor;

        public Line(UIWidget parent) {
            this(parent, Alignment.LEFT);
        }

        public Line(UIWidget parent, Alignment alignment) {
            this(parent, alignment, ColorHelper.WHITE);
        }

        public Line(UIWidget parent, Alignment alignment, int color) {
            super(parent);
            this.alignment = alignment;
            this.baseColor = color;
        }

        public Line setAlignment(Alignment alignment) {
            this.alignment = alignment;
            refresh();
            return this;
        }

        public Line setBaseColor(int color) {
            this.baseColor = color;
            refresh();
            return this;
        }

        @Override
        public abstract void render(GuiGraphics graphics, int x, int y, int w, int h);
    }
}
