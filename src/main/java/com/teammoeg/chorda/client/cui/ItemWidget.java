package com.teammoeg.chorda.client.cui;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

@Setter
@Getter
public class ItemWidget extends UIWidget {
    public static final int ITEM_WIDTH = 16;
    public static final int ITEM_HEIGHT = 16;
    protected ItemStack item;
    protected float scale;
    protected boolean hoverOverlayEnabled = true;

    public ItemWidget(UIWidget parent, ItemStack item) {
        super(parent);
        this.item = item;
        setScale(1);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        if (scale != 1) {
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(x, y, 0);
            pose.scale(scale, scale, 0);
            graphics.renderItem(item, 0, 0);
            pose.popPose();
        } else {
            graphics.renderItem(item, x, y);
        }

        if (isHoverOverlayEnabled() && isMouseOver()) {
            graphics.fill(x, y, x+w, y+h, 151, Colors.setAlpha(Colors.WHITE, 0.25F));
        }
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver()) return false;

        switch (button) {
            case LEFT -> JEICompat.showJEIFor(item);
            case RIGHT -> JEICompat.showJEIUsageFor(item);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onKeyPressed(int keyCode, int scanCode, int modifier) {
        if (!isMouseOver()) return false;

        switch (keyCode) {
            case GLFW.GLFW_KEY_R -> {
                if (modifier == GLFW.GLFW_MOD_SHIFT) {
                    JEICompat.showJEIUsageFor(item);
                } else {
                    JEICompat.showJEIFor(item);
                }
            }
            case GLFW.GLFW_KEY_U -> JEICompat.showJEIUsageFor(item);
            default -> {
                return false;
            }
        }
        return true;
    }

    public void setScale(float scale) {
        this.scale = scale;
        setSize((int) (ITEM_WIDTH * scale), (int) (ITEM_HEIGHT * scale));
    }

    @Override
    public void getTooltip(Consumer<Component> tooltip) {
        item.getTooltipLines(ClientUtils.getPlayer(), TooltipFlag.NORMAL).forEach(tooltip);
    }
}
