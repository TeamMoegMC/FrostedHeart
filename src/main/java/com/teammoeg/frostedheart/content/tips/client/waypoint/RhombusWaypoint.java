package com.teammoeg.frostedheart.content.tips.client.waypoint;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.content.tips.client.util.AnimationUtil;
import com.teammoeg.frostedheart.content.tips.client.util.GuiUtil;
import com.teammoeg.frostedheart.util.client.Point;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public class RhombusWaypoint extends Waypoint {
    private Point icon;
    private Point focusIcon;
    private int maxTextWidth;
    private int height;

    public RhombusWaypoint(Vector3f target, int color, String ID) {
        super(target, ID, color);
        this.icon = IconButton.ICON_BOX;
        this.focusIcon = IconButton.ICON_BOX_ON;
    }

    public RhombusWaypoint(Vector3f target, int color, String ID, Point icon, Point focusIcon) {
        super(target, ID, color);
        this.icon = icon;
        this.focusIcon = focusIcon;
    }

    public void setIcon(Point icon) {
        this.icon = icon;
    }

    public void setFocusIcon(Point focusIcon) {
        this.focusIcon = focusIcon;
    }

    @Override
    public void renderMain(MatrixStack ms) {
        if (focus) {
            ms.mulPose(Vector3f.ZN.rotationDegrees(45));
            ms.scale(1.5F, 1.5F, 1.5F);
            GuiUtil.renderIcon(ms, focusIcon, -5, -5, this.color);
            //focus的动画效果
            float progress = AnimationUtil.calcFadeIn(750, "waypoint" + this.id, false);
            if (progress == 1 && AnimationUtil.calcProgress(750, "waypoint2" + this.id, false) == 1) {
                AnimationUtil.removeAnimation("waypoint" + this.id);
                AnimationUtil.removeAnimation("waypoint2" + this.id);
            }
            int fadeColor = (int)((1-progress) * 255.0F) << 24 | this.color & 0x00FFFFFF;
            //让不透明度可以低于0.1
            ms.scale(progress+0.25F, progress+0.25F, progress+0.25F);
            RenderSystem.disableAlphaTest();
            AbstractGui.fill(ms, -5, -5, 5, 5, fadeColor);
            RenderSystem.enableAlphaTest();
            ms.scale(0.66667F, 0.66667F, 0.66667F);
            return;
        }

        ms.mulPose(Vector3f.ZN.rotationDegrees(45));
        //防止被MC的神必半透明材质覆盖
        ms.translate(0, 0, 1);
        GuiUtil.renderIcon(ms, icon, -5, -5, this.color);
    }


    @Override
    public void renderText(MatrixStack ms) {
        ms.translate(15, -3.5F, 0);
        AbstractGui.fill(ms, -3, -2, -2, height, this.color);
        AbstractGui.fill(ms, -2, -2, maxTextWidth+2, height, 0x40000000);

        maxTextWidth = MC.player.isShiftKeyDown() ? Math.max(96, maxTextWidth) : 96;
        height = 10 * GuiUtil.drawWrapString(this.id, ms, 0, 0, maxTextWidth, this.color, 10, false);
        
        if (MC.player.isShiftKeyDown()) {
            AbstractGui.fill(ms, -3, height, -2, height+30, this.color);
            AbstractGui.fill(ms, -2, height, maxTextWidth+2, height+30, 0x40000000);

            Vector3d v = new Vector3d(this.target);
            String distance = "Distance: " + (int)v.distanceTo(MC.player.position()) + " blocks";
            maxTextWidth = Math.max(MC.font.width(distance), maxTextWidth);
            MC.font.draw(ms, distance, 0, height+10, this.color);

            String pos = "[X: %.2f, Y: %.2f, Z: %.2f]";
            pos = String.format(pos, this.target.x(), this.target.y(), this.target.z());
            maxTextWidth = Math.max(MC.font.width(pos), maxTextWidth);
            MC.font.draw(ms, pos, 0, height+20, this.color);
        }
        ms.translate(-15, 3.5F, 0);
    }
}
