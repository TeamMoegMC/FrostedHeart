package com.teammoeg.frostedheart.base.client.gui.widget;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import com.teammoeg.frostedheart.util.client.FHGuiHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.common.util.Size2i;
import org.jetbrains.annotations.NotNull;

public class IconButton extends Button {
    public static final ResourceLocation ICON_LOCATION = FHMain.rl("textures/gui/hud/flat_icon.png");
    public static final int TEXTURE_HEIGHT = 80;
    public static final int TEXTURE_WIDTH = 80;

    public final Icon icon;
    public int color;
    // 为什么是int? 混素达咩(
    private int scale;

    /**
     * @param icon 按钮的图标 {@link Icon}
     */
    public IconButton(int x, int y, Icon icon, int color, Component title, OnPress pressedAction) {
        this(x, y, icon, color, 1, title, pressedAction);
    }

    public IconButton(int x, int y, Icon icon, int color, int scale, Component title, OnPress pressedAction) {
        super(x, y, icon.size.width, icon.size.height, title, pressedAction, Button.DEFAULT_NARRATION);
        this.scale = Mth.clamp(scale, 1, Integer.MAX_VALUE);
        width *= scale;
        height *= scale;
        this.color = color;
        this.icon = icon;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (isHoveredOrFocused()) {
            graphics.fill(getX(), getY(), getX()+getWidth(), getY()+getHeight(), FHColorHelper.setAlpha(color, 50));
            if (!getMessage().getString().isBlank() && isHovered()) {
                int textWidth = ClientUtils.font().width(getMessage());
                int renderX = getX()-textWidth+8;
                if (renderX < 0) {
                    graphics.fill(getX(),
                            getY()-12,
                            getX()+2 + textWidth,
                            getY(),
                            FHColorHelper.setAlpha(color, 50));
                    graphics.drawString(ClientUtils.font(), getMessage(), getX()+2, getY()-10, color);
                } else {
                    graphics.fill(getX()-textWidth+getWidth()-1,
                            getY()-12,
                            getX()+getWidth(),
                            getY(),
                            FHColorHelper.setAlpha(color, 50));
                    graphics.drawString(ClientUtils.font(), getMessage(), getX()-textWidth+getWidth(), getY()-10, color);
                }
            }
        }

        FHGuiHelper.bindTexture(ICON_LOCATION);
        FHGuiHelper.blitColored(graphics.pose(), getX(), getY(), getWidth(), getHeight(), icon.x*scale, icon.y*scale, getWidth(), getHeight(), TEXTURE_WIDTH*scale, TEXTURE_HEIGHT*scale, color, alpha);
    }

    public void setScale(int scale) {
        int w = this.width / this.scale;
        int h = this.height / this.scale;
        this.scale = scale;
        this.width = w * scale;
        this.height = h * scale;
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

        public final int x;
        public final int y;
        public final Size2i size;

        Icon(int x, int y, Size2i size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }

        Icon(int x, int y) {
            this.x = x;
            this.y = y;
            this.size = new Size2i(10, 10);
        }
    }
}
