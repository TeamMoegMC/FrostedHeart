package com.teammoeg.frostedheart.content.tips.client.waypoint;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.content.tips.client.util.GuiUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public class Waypoint {
    protected static final Minecraft MC = Minecraft.getInstance();
    public String id;
    public Vector3f target;
    public boolean focus;
    public boolean visible;
    public int color;

    public Waypoint(Vector3f target, String ID, int color) {
        this.id = ID;
        this.target = target;
        this.color = color;
        this.focus = false;
        this.visible = true;
    }

    public void render(MatrixStack ms) {
        if (!this.visible) return;

        Vector2f screenPos = GuiUtil.worldPosToScreenPos(this.target);
        double x = MathHelper.clamp(screenPos.x, 10, MC.getMainWindow().getScaledWidth()-10);
        double y = MathHelper.clamp(screenPos.y, 10, MC.getMainWindow().getScaledHeight()-10);
        double xP = x / MC.getMainWindow().getScaledWidth();
        double yP = y / MC.getMainWindow().getScaledHeight();

        ms.push();
        ms.translate(x, y, 0);
        if (xP >= 0.45 && xP <= 0.55 && yP >= 0.45 && yP <= 0.55) {
            renderText(ms);
        }
        renderMain(ms);
        ms.pop();
    }

    public void renderMain(MatrixStack ms) {
        if (this.focus) {
            GuiUtil.renderIcon(ms, IconButton.ICON_BOX_ON, -5, -5, this.color);
        } else {
            GuiUtil.renderIcon(ms, IconButton.ICON_BOX, -5, -5, this.color);
        }
    }

    public void renderText(MatrixStack ms) {
        float textWidth = MC.fontRenderer.getStringWidth(this.id)*0.5F;
        AbstractGui.fill(ms, (int)(-textWidth)-2, -19, (int)(textWidth)+2, -7, 0x80000000);
        MC.fontRenderer.drawString(ms, this.id, -textWidth, -17, this.color);

        if (MC.player.isSneaking()) {
            Vector3d v = new Vector3d(this.target);
            String distance = "Distance: " + (int)v.distanceTo(MC.player.getPositionVec()) + " blocks";
            float textWidth1 = MC.fontRenderer.getStringWidth(distance)*0.5F;
            AbstractGui.fill(ms, (int)(-textWidth1)-2, 8, (int)(textWidth1)+2, 20, 0x80000000);
            MC.fontRenderer.drawString(ms, distance, -textWidth1, 10, this.color);

            String pos = "[X: %.2f, Y: %.2f, Z: %.2f]";
            pos = String.format(pos, this.target.getX(), this.target.getY(), this.target.getZ());
            float textWidth2 = MC.fontRenderer.getStringWidth(pos)*0.5F;
            AbstractGui.fill(ms, (int)(-textWidth2)-2, 20, (int)(textWidth2)+2, 32, 0x80000000);
            MC.fontRenderer.drawString(ms, pos, -textWidth2, 22, this.color);
        }
    }

    @Override
    public String toString() {
        return "ID: '" + id + "' [" + target.getX() + ", " + target.getY() + ", " + target.getZ() + "]";
    }
}
