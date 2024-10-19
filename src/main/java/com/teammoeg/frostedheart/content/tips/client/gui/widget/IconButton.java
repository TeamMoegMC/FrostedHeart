package com.teammoeg.frostedheart.content.tips.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.client.FHGuiHelper;
import com.teammoeg.frostedheart.util.client.Point;
import com.teammoeg.frostedheart.util.client.RawMouseHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class IconButton extends Button {
    public static final ResourceLocation ICON_LOCATION = new ResourceLocation(FHMain.MODID, "textures/gui/hud_icon.png");
    public static final int TEXTURE_HEIGHT = 80;
    public static final int TEXTURE_WIDTH = 80;
    public static final int ICON_SIZE = 10;
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
    public IconButton(int x, int y, Point icon, int color, Component title, OnPress pressedAction) {
        super(builder(title, pressedAction).bounds(x, y, ICON_SIZE, ICON_SIZE).tooltip(Tooltip.create(title)));
        this.color = color;
        this.currentIcon = icon;
    }

    public void setXY(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (isHovered()) {
        	graphics.fill( getX(), getY(), getX()+width, getY()+height, 50 << 24 | color & 0x00FFFFFF);
        }

        FHGuiHelper.bindTexture(ICON_LOCATION);
        FHGuiHelper.blitColored(graphics.pose(), getX(), getY(), ICON_SIZE, ICON_SIZE, currentIcon.getX(), currentIcon.getY(), ICON_SIZE, ICON_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT, color, alpha);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 258) {
            return false;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    /**
     * 渲染一个图标
     * @param icon {@link IconButton}
     * @param color 图标的颜色
     */
    public static void renderIcon(PoseStack pose, Point icon, int x, int y, int color) {
        FHGuiHelper.bindTexture(ICON_LOCATION);
        FHGuiHelper.blitColored(pose, x, y, ICON_SIZE, ICON_SIZE, icon.getX(), icon.getY(), ICON_SIZE, ICON_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT, color);
    }

    /**
     * 直接在屏幕中渲染一个图标按钮
     * @param icon {@link IconButton}
     * @param color 图标的颜色
     * @param BGColor 未被选中时的背景颜色，为 0 时不显示
     * @return 是否被按下
     */
    public static boolean renderIconButton(GuiGraphics graphics, Point icon, int mouseX, int mouseY, int x, int y, int color, int BGColor) {
        if (color != 0 && RawMouseHelper.isMouseIn(mouseX, mouseY, x, y, ICON_SIZE, ICON_SIZE)) {
            graphics.fill(x, y, x+ICON_SIZE, y+ICON_SIZE, 50 << 24 | color & 0x00FFFFFF);
        } else if (BGColor != 0) {
            graphics.fill(x, y, x+ICON_SIZE, y+ICON_SIZE, BGColor);
        }
        renderIcon(graphics.pose(), icon, x, y, color);
        return RawMouseHelper.isMouseIn(mouseX, mouseY, x, y, TEXTURE_WIDTH, TEXTURE_HEIGHT) && RawMouseHelper.isLeftClicked();
    }
}
