package com.teammoeg.frostedheart.content.tips.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import com.teammoeg.frostedheart.util.client.FHGuiHelper;
import com.teammoeg.frostedheart.util.client.RawMouseHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class IconButton extends Button {
    public static final ResourceLocation ICON_LOCATION = new ResourceLocation(FHMain.MODID, "textures/gui/hud_icon.png");
    public static final int TEXTURE_HEIGHT = 80;
    public static final int TEXTURE_WIDTH = 80;
    public static final int ICON_SIZE = 10;

    public Icon currentIcon;
    public int color;

    /**
     * @param icon 按钮的图标 {@link Icon}
     */
    public IconButton(int x, int y, Icon icon, int color, Component title, OnPress pressedAction) {
        super(x, y, 10, 10, title, pressedAction, Button.DEFAULT_NARRATION);
        this.color = color;
        this.currentIcon = icon;
    }

    public void setXY(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (isHoveredOrFocused()) {
            graphics.fill(getX(), getY(), getX()+width, getY()+height, FHColorHelper.setAlpha(color, 50));
            if (!getMessage().getString().isEmpty() && isHovered()) {
                int textWidth = ClientUtils.font().width(getMessage());
                int renderX = getX()-textWidth+8;
                if (renderX < 0) {
                    graphics.fill(getX(), getY()-12, getX()+2 + textWidth, getY(), FHColorHelper.setAlpha(color, 50));
                    graphics.drawString(ClientUtils.font(), getMessage(), getX()+2, getY()-10, color);
                } else {
                    graphics.fill(getX()+8 - textWidth, getY()-12, getX()+10, getY(), FHColorHelper.setAlpha(color, 50));
                    graphics.drawString(ClientUtils.font(), getMessage(), getX()-textWidth+width, getY()-10, color);
                }
            }
        }

        FHGuiHelper.bindTexture(ICON_LOCATION);
        FHGuiHelper.blitColored(graphics.pose(), getX(), getY(), ICON_SIZE, ICON_SIZE, currentIcon.x, currentIcon.y, ICON_SIZE, ICON_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT, color, alpha);
    }

    /**
     * 渲染一个图标
     * @param icon {@link IconButton.Icon}
     * @param color 图标的颜色
     */
    public static void renderIcon(PoseStack pose, Icon icon, int x, int y, int color) {
        FHGuiHelper.bindTexture(ICON_LOCATION);
        FHGuiHelper.blitColored(pose, x, y, ICON_SIZE, ICON_SIZE, icon.x, icon.y, ICON_SIZE, ICON_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT, color);
    }

    /**
     * 直接在屏幕中渲染一个图标按钮
     * @param icon {@link IconButton.Icon}
     * @param color 图标的颜色
     * @param BGColor 未被选中时的背景颜色，为 0 时不显示
     * @return 是否被按下
     */
    public static boolean renderIconButton(GuiGraphics graphics, Icon icon, double mouseX, double mouseY, int x, int y, int color, int BGColor) {
        boolean mouseIn = RawMouseHelper.isMouseIn(mouseX, mouseY, x, y, ICON_SIZE, ICON_SIZE);
        if (color != 0 && mouseIn) {
            graphics.fill(x, y, x+ICON_SIZE, y+ICON_SIZE, 50 << 24 | color & 0x00FFFFFF);
        } else if (BGColor != 0) {
            graphics.fill(x, y, x+ICON_SIZE, y+ICON_SIZE, BGColor);
        }
        renderIcon(graphics.pose(), icon, x, y, color);
        return mouseIn && RawMouseHelper.isLeftClicked();
    }

    public enum Icon {
        MOUSE_LEFT    (0 ,0 ),
        MOUSE_RIGHT   (10,0 ),
        MOUSE_MIDDLE  (20,0 ),
        SIGHT         (30,0 ),
        QUESTION_MARK (0 ,10),
        LOCK          (10,10),
        CONTINUE      (20,10),
        FORBID        (30,10),
        RIGHT         (40,10),
        DOWN          (50,10),
        LEFT          (60,10),
        TOP           (70,10),
        TRADE         (0 ,20),
        GIVE          (10,20),
        GAIN          (20,20),
        LEAVE         (30,20),
        BOX           (0 ,30),
        BOX_ON        (10,30),
        CROSS         (20,30),
        HISTORY       (30,30),
        LIST          (40,30),
        TRASH_CAN     (50,30),
        CHECK         (60,30),
        FOLDER        (70,30),
        LEFT_SLIDE    (0 ,40),
        RIGHT_SLIDE   (0 ,50),
        WRENCH        (0 ,70);

        final int x;
        final int y;

        Icon(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
