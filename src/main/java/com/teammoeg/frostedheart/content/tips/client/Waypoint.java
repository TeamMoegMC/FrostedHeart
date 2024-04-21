package com.teammoeg.frostedheart.content.tips.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.content.tips.client.util.AnimationUtil;
import com.teammoeg.frostedheart.content.tips.client.util.GuiUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3f;

public class Waypoint {
    private final Minecraft MC = Minecraft.getInstance();
    public Vector3f target;
    public int color;
    public boolean focus;
    public boolean visible;
    public String id;

    public Waypoint(Vector3f target, int color, String ID) {
        this.target = target;
        this.color = color;
        this.id = ID;
        this.visible = true;
    }

    public void render(MatrixStack ms) {
        if (!visible || MC.player == null) return;

        Vector2f screenPos = GuiUtil.worldPosToScreenPos(this.target);
        double x = MathHelper.clamp(screenPos.x, 10, MC.getMainWindow().getScaledWidth()-10);
        double y = MathHelper.clamp(screenPos.y, 10, MC.getMainWindow().getScaledHeight()-10);
        double xP = x / MC.getMainWindow().getScaledWidth();
        double yP = y / MC.getMainWindow().getScaledHeight();

        ms.push();
        if (this.focus) {
            //放大导航点
            ms.scale(1.5F, 1.5F, 1.5F);
            x /= 1.5;
            y /= 1.5;
        }

        ms.translate(x, y, 0);
        if (xP >= 0.45 && xP <= 0.55 && yP >= 0.45 && yP <= 0.55) {
            //让文本保持原尺寸
            if (this.focus) ms.scale(0.666667F, 0.666667F, 0.666667F);
            if (MC.player.isSneaking()) {
                MC.fontRenderer.drawString(ms, this.toString(), 7, -3.51F, color);
            } else {
                MC.fontRenderer.drawString(ms, this.id, 7, -3.51F, color);
            }
            if (this.focus) ms.scale(1.5F, 1.5F, 1.5F);
        }

        ms.rotate(Vector3f.ZN.rotationDegrees(45));
        if (this.focus) {
            GuiUtil.renderIcon(ms, IconButton.ICON_BOX_ON, -5, -5, this.color);
            //focus的动画效果
            float progress = AnimationUtil.calcFadeIn(750, "waypoint" + this.id, false);
            if (progress == 1.0F && AnimationUtil.calcProgress(750, "waypoint2" + this.id, false) == 1.0F) {
                AnimationUtil.removeAnimation("waypoint" + this.id);
                AnimationUtil.removeAnimation("waypoint2" + this.id);
            }
            int fadeColor = (int)((1-progress) * 255.0F) << 24 | this.color & 0x00FFFFFF;
            ms.scale(progress+0.25F, progress+0.25F, progress+0.25F);
            //让不透明度可以低于0.1
            RenderSystem.disableAlphaTest();
            AbstractGui.fill(ms, -5, -5, 5, 5, fadeColor);
            RenderSystem.enableAlphaTest();

        } else {
            //防止被MC的神必半透明材质覆盖
            ms.translate(0, 0, 1);
            GuiUtil.renderIcon(ms, IconButton.ICON_BOX, -5, -5, this.color);
        }
        ms.pop();
    }

    @Override
    public String toString() {
        return "ID: '" + id + "' [" + target.getX() + ", " + target.getY() + ", " + target.getZ() + "]";
    }
}
