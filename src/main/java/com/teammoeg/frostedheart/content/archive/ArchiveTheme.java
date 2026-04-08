package com.teammoeg.frostedheart.content.archive;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.TesselateHelper;
import com.teammoeg.chorda.client.cui.theme.Theme;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.math.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 档案系统专用主题，为档案/百科内容面板提供深色风格的UI渲染。
 * 定义了按钮、滑块、文本框、面板、工具提示等组件的绘制方式和配色方案。
 * <p>
 * Archive system theme providing dark-styled UI rendering for archive/encyclopedia
 * content panels. Defines drawing methods and color schemes for buttons, sliders,
 * text boxes, panels, tooltips, and other components.
 */
public class ArchiveTheme implements Theme {
    public static final ArchiveTheme INSTANCE = new ArchiveTheme();
    private ArchiveTheme() {}

    @Override
    public void drawButton(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight, boolean enabled) {
        graphics.fill(x, y, x+w, y+h, UIBGBorderColor());
        if (isHighlight && enabled) {
        	TesselateHelper.getShapeTesslator()
        	.drawRectWH(graphics.pose().last().pose(), x, y, w, h, Colors.themeColor(), true)
        	.close();
        }
    }

    @Override
    public void drawSliderBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight) {
        int x2 = x + w;
        int y2 = y + h;
        graphics.fill(x+1, y+1, x2-1, y2-1, 0xFF282A31);
        graphics.fill(x, y, x2, y + 1, UIBGBorderColor()); // top
        graphics.fill(x, y2 - 1, x2, y2, UIBGBorderColor()); // bottom
        graphics.fill(x2 - 1, y, x2, y2, UIBGBorderColor()); // right
    }

    @Override
    public void drawTextboxBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean focused) {
        drawButton(graphics, x, y, w, h, focused, focused);
    }

    @Override
    public void drawSliderBar(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight) {
        graphics.fill(x, y, x + w, y + h, UIAltTextColor());
    }

    @Override
    public void drawPanel(GuiGraphics graphics, int x, int y, int w, int h) {

    }

    @Override
    public void drawSlot(GuiGraphics graphics, int x, int y, int w, int h) {

    }

    @Override
    public void drawUIBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        TesselateHelper.getShapeTesslator()
        .fillRectWH(graphics.pose().last().pose(), x, y, w, h, UIBGColor())
    	.drawRectWH(graphics.pose().last().pose(), x, y, w, h, UIBGBorderColor(), true)
    	.close();
    }

    @Override
    public void drawTooltip(GuiGraphics graphics, List<Component> tooltipLines, int mouseX, int mouseY, int zOffset) {
        var font = ClientUtils.font();
        var context = CGuiHelper.split(tooltipLines, font, mouseX, graphics.guiWidth());
        int w = context.maxWidth();
        int h = context.lineSize() * font.lineHeight;
        var pos = DefaultTooltipPositioner.INSTANCE.positionTooltip(ClientUtils.screenWidth(), ClientUtils.screenHeight(), mouseX, mouseY+6, w, h);
        int x = pos.x();
        int y = pos.y();
        int border = 4;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, zOffset+400);
        graphics.fill(x-border, y-border, x+w+border, y+h+border, 0xFF282A31);
        CGuiHelper.drawStringLines(graphics, font, context.lines(), x, y, UITextColor(), 0, true, false);
        graphics.pose().popPose();
    }

    @Override
    public int UITextColor() {
        return Colors.WHITE;
    }

    @Override
    public int UIAltTextColor() {
        return 0xFF9294A3;
    }

    public int UIBGColor() {
        return 0xFF444651;
    }

    public int UIBGBorderColor() {
        return 0xFF585966;
    }

    @Override
    public int buttonTextColor() {
        return UITextColor();
    }

    @Override
    public int buttonTextOverColor() {
        return UITextColor();
    }

    @Override
    public int buttonTextDisabledColor() {
        return UIAltTextColor();
    }

    @Override
    public int errorColor() {
        return 0xFFFF5340;
    }

    @Override
    public int successColor() {
        return 0xFFC1E52F;
    }

    @Override
    public boolean isUITextShadow() {
        return false;
    }

    @Override
    public boolean isButtonTextShadow() {
        return true;
    }
}
