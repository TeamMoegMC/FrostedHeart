package com.teammoeg.frostedheart.content.tips.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.client.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class IconButton extends Button {
    public static final ResourceLocation ICON_LOCATION = new ResourceLocation(FHMain.MODID, "textures/gui/hud_icon.png");
    public static final Point ICON_MOUSE_LEFT    = new Point(0 ,0 );
    public static final Point ICON_MOUSE_RIGHT   = new Point(10,0 );
    public static final Point ICON_MOUSE_MIDDLE  = new Point(20,0 );
    public static final Point ICON_SIGHT         = new Point(30,0 );
    public static final Point ICON_QUESTION_MARK = new Point(0 ,10);
    public static final Point ICON_LOCK          = new Point(10,10);
    public static final Point ICON_CONTINUE      = new Point(20,10);
    public static final Point ICON_FORBID        = new Point(30,10);
    public static final Point ICON_RIGHT         = new Point(40,10);
    public static final Point ICON_DOWN          = new Point(50,10);
    public static final Point ICON_LEFT          = new Point(60,10);
    public static final Point ICON_TOP           = new Point(70,10);
    public static final Point ICON_TRADE         = new Point(0 ,20);
    public static final Point ICON_GIVE          = new Point(10,20);
    public static final Point ICON_GAIN          = new Point(20,20);
    public static final Point ICON_LEAVE         = new Point(30,20);
    public static final Point ICON_BOX           = new Point(0 ,30);
    public static final Point ICON_BOX_ON        = new Point(10,30);
    public static final Point ICON_CROSS         = new Point(20,30);
    public static final Point ICON_HISTORY       = new Point(30,30);
    public static final Point ICON_LIST          = new Point(40,30);
    public static final Point ICON_TRASH_CAN     = new Point(50,30);
    public static final Point ICON_CHECK         = new Point(60,30);
    public static final Point ICON_FOLDER        = new Point(70,30);
    public static final Point ICON_LEFT_SLIDE    = new Point(0 ,40);
    public static final Point ICON_RIGHT_SLIDE   = new Point(0 ,50);
    public static final Point ICON_WRENCH        = new Point(0 ,70);

    public final Point currentIcon;
    public final int color;

    /**
     * @param icon 按钮的图标，例如 {@link #ICON_LOCK}
     */
    public IconButton(int x, int y, Point icon, int color, ITextComponent title, IPressable pressedAction) {
        super(x, y, 10, 10, title, pressedAction);
        this.color = color;
        this.currentIcon = icon;
    }

    public void setXY(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (isHovered()) {
            fill(matrixStack, x, y, x+width, y+height, 50 << 24 | color & 0x00FFFFFF);
            renderToolTip(matrixStack, mouseX, mouseY);
        }

        float r = (color >> 16 & 0xFF) / 255F;
        float g = (color >> 8 & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(r, g, b, alpha);
        Minecraft.getInstance().getTextureManager().bindTexture(ICON_LOCATION);
        blit(matrixStack, x, y, currentIcon.getX(), currentIcon.getY(), 10, 10, 80, 80);
        RenderSystem.disableBlend();
    }

    @Override
    public void renderToolTip(MatrixStack matrixStack, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        String text = getMessage().getString();
        if (!text.isEmpty()) {
            int textWidth = mc.fontRenderer.getStringWidth(text);
            int renderX = x-textWidth+8;
            if (renderX < 0) {
                fill(matrixStack, x, y - 12, x+2 + textWidth, y, 50 << 24 | color & 0x00FFFFFF);
                mc.fontRenderer.drawString(matrixStack, text, x+2, y-10, color);
            } else {
                fill(matrixStack, x+8 - textWidth, y - 12, x + 10, y, 50 << 24 | color & 0x00FFFFFF);
                mc.fontRenderer.drawString(matrixStack, text, x-textWidth+width, y-10, color);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 258) {
            return false;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}
