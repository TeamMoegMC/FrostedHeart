package com.teammoeg.chorda.client.cui.contentpanel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.ScrollBar;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.Colors;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContentPanel extends Layer {
    public final ScrollBar scrollBar;
    protected List<Line<?>> lines = new ArrayList<>();

    public ContentPanel(UIWidget parent) {
        super(parent);
        this.scrollBar = new LayerScrollBar(parent, true, this);
        resize();
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
        graphics.fill(x-border, y-border, x+w+border*2, y+h+border, -2, 0xFF444651);
        CGuiHelper.drawBox(graphics, x-border, y-border, w+border*3, h+border*2, Colors.L_BG_GRAY, true);
    }

    public void fillContent(Collection<Line<?>> lines) {
        clearElement();
        lines.forEach(this::add);
        refresh();
    }

    public void addLine(Line<?> line) {
        this.lines.add(line);
        add(line);
        refresh();
    }

    public void addLines(Collection<? extends Line<?>> lines) {
        this.lines.addAll(lines);
        lines.forEach(this::add);
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
        scrollBar.setPosAndSize(getX() + w+9, -8, 6, h+15);
    }

    @Override
    public void alignWidgets() {
        align(4, false);
    }

    @Override
    public void addUIElements() {}
}
