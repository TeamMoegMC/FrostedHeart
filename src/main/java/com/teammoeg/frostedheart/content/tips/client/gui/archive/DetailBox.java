package com.teammoeg.frostedheart.content.tips.client.gui.archive;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.StringTextComponentParser;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.ScrollBar;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.ui.UV;
import com.teammoeg.frostedheart.FrostedHud;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.common.util.Size2i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DetailBox extends Layer {
    public final ScrollBar scrollBar;

    public DetailBox(UIWidget panel) {
        super(panel);
        this.scrollBar = new LayerScrollBar(parent, true, this);
        setSmoothScrollEnabled(true);
        resize();
    }

    public void fillContent(Line... lines) {
        this.fillContent(Arrays.asList(lines));
    }

    public void fillContent(Collection<Line> lines) {
        clearElement();
        for (Line line : lines) {
            add(line);
        }
        refresh();
    }

    @Override
    public void refresh() {
        resize();
        recalcContentSize();
        addUIElements();
        for (UIWidget element : elements) {
            element.refresh();
        }
        alignWidgets();
        scrollBar.setValue(0);
    }

    private void resize() {
        int h = (int)(ClientUtils.screenHeight() * 0.8F);
        int w = (int)(h * 1.3333F); // 4:3
        setPosAndSize(120, 0, w, h);
        scrollBar.setPosAndSize(getX() + w+8, -7, 6, h+15);
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
        align(4, false);
    }

    public TextLine text(String text) {
        return new TextLine(Alignment.LEFT, Component.literal(text));
    }

    public TextLine text(Alignment alignment, String text) {
        return new TextLine(alignment, Component.literal(text));
    }

    public TextLine text(Component text) {
        return new TextLine(Alignment.LEFT, text);
    }

    public TextLine text(Alignment alignment, Component text) {
        return new TextLine(alignment, text);
    }

    public ImageLine image(String imageLocation) {
        return new ImageLine(Alignment.CENTER, ResourceLocation.tryParse(imageLocation));
    }

    public ImageLine image(Alignment alignment, String imageLocation) {
        return new ImageLine(alignment, ResourceLocation.tryParse(imageLocation));
    }

    public ImageLine image(ResourceLocation imageLocation) {
        return new ImageLine(Alignment.CENTER, imageLocation);
    }

    public ImageLine image(Alignment alignment, ResourceLocation imageLocation) {
        return new ImageLine(alignment, imageLocation);
    }

    public EmptyLine emptyLine() {
        return new EmptyLine();
    }

    public BreakLine br() {
        return new BreakLine();
    }

    public BreakLine br(int color) {
        return (BreakLine) new BreakLine().setBaseColor(color);
    }


    public class TextLine extends Line {
        @Getter
        protected Component text;
        protected final List<FormattedCharSequence> splitText = new ArrayList<>();
        protected int backgroundColor = 0;
        protected int trimmingColor = 0;
        protected boolean isTitle = false;
        protected boolean isQuote = false;
        protected boolean shadow = false;
        protected int scale = 1;

        private TextLine(Alignment alignment, Component text) {
            super(alignment);
            this.text = text;
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h) {
            super.render(graphics, x, y, w, h);
            var pose = graphics.pose();
            pose.pushPose();

            if (scale > 1) {
                pose.translate(-x, -y, 0);
                pose.scale(scale, scale, 1);
                w = Math.round((float) w / scale);
                h = Math.round((float) h / scale);
            }

            int textW = w;
            if (isTitle) {
                graphics.fill(x, y, x+w, y+h, -1, trimmingColor);
                int offset = switch (alignment) {
                    case LEFT -> 2;
                    case CENTER -> 0;
                    case RIGHT -> -2;
                };
                textW -= offset;
                pose.translate(offset, 0, 0);

            } else if (isQuote) {
                graphics.fill(x, y, x+4, y+h, -1, trimmingColor);
                int offset = switch (alignment) {
                    case LEFT -> 8;
                    case CENTER -> 0;
                    case RIGHT -> -2;
                };
                textW -= offset;
                pose.translate(offset, 0, 0);
            }

            pose.translate(0, 1.5F, 0);

            CGuiHelper.drawStringLinesInBound(graphics, getFont(), splitText, x, y, textW, baseColor, 3,
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

        public TextLine setTitle(int trimmingColor, int scale) {
            this.isTitle = true;
            this.trimmingColor = trimmingColor;
            setScale(scale);
            return this;
        }

        public TextLine setQuote(int trimmingColor) {
            this.isQuote = true;
            this.baseColor = trimmingColor;
            this.trimmingColor = trimmingColor;
            return this;
        }

        public TextLine addShadow() {
            this.shadow = true;
            return this;
        }

        @Override
        public void refresh() {
            super.refresh();
            var pre = text.getString().split("\\n");
            splitText.clear();
            for (String s : pre) {
                splitText.addAll(getFont().split(StringTextComponentParser.parse(s), (int)(getWidth() * (1.0F/scale) - ((isTitle||isQuote) ? 8 : 0))));
            }

            setHeight(splitText.size() * DEF_LINE_HEIGHT * scale);
        }
    }

    public class ImageLine extends Line {
        protected ResourceLocation imgLocation;
        protected Size2i imgSize;
        protected UV imgUV;
        protected int backgroundColor = 0;

        private ImageLine(Alignment alignment, ResourceLocation imageLocation) {
            super(alignment);
            setImage(imageLocation);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h) {
            super.render(graphics, x, y, w, h);
            if (!isImgValid()) {
                graphics.drawCenteredString(getFont(), Component.literal(imgLocation.toString()).withStyle(ChatFormatting.RED),
                        x+w/2, y, baseColor);
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
            imgSize = ClientUtils.getImgSize(imageLocation);
            imgUV = new UV(0, 0, 0, 0);
            refresh();
            return this;
        }

        public ImageLine setBackgroundColor(int color) {
            this.backgroundColor = color;
            return this;
        }

        @Override
        public void refresh() {
            super.refresh();
            if (isImgValid()) {
                int w = imgSize.width;
                int h = imgSize.height;
                if (w > 32) {
                    w /= 2;
                    h /= 2;
                }
                if (w > getWidth()) {
                    w = getWidth();
                    h = (int)(h * (w / (imgSize.width * 0.5F)));
                }
                setHeight(h+6);
                imgUV = new UV(0, 0, w, h, w, h);
            }
        }
    }

    public class BreakLine extends Line {
        private BreakLine() {
            setBaseColor(ColorHelper.L_BG_GRAY);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h) {
            super.render(graphics, x, y, w, h);
            CGuiHelper.fillGradient(graphics.pose(), x, y+h/2, x+w/2, y+h/2+1, ColorHelper.setAlpha(baseColor, 0), baseColor);
            CGuiHelper.fillGradient(graphics.pose(),x+w/2, y+h/2, x+w, y+h/2+1, baseColor, ColorHelper.setAlpha(baseColor, 0));
        }

        @Override
        public void refresh() {
            super.refresh();
            setHeight((int) (DEF_LINE_HEIGHT * 1.5F));
        }
    }

    public class EmptyLine extends Line {

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h) {
            super.render(graphics, x, y, w, h);
        }
    }

    public abstract class Line extends UIWidget {
        public static final int DEF_LINE_HEIGHT = 12;
        protected Alignment alignment;
        protected int baseColor;

        public Line() {
            this(Alignment.LEFT);
        }

        public Line(Alignment alignment) {
            this(alignment, ColorHelper.WHITE);
        }

        public Line(Alignment alignment, int color) {
            super(DetailBox.this);
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
        public void refresh() {
            setSize(parent.getWidth(), DEF_LINE_HEIGHT);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h) {
            if (FrostedHud.renderDebugOverlay && (isMouseOver() || CInputHelper.isShiftKeyDown())) {
                graphics.fill(x, y, x+w, y+h, ColorHelper.setAlpha(ColorHelper.CYAN, 0.2F));
            }
        }
    }
}
