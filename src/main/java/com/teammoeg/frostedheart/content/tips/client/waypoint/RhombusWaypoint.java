package com.teammoeg.frostedheart.content.tips.client.waypoint;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.content.tips.client.util.AnimationUtil;
import com.teammoeg.frostedheart.content.tips.client.util.GuiUtil;
import com.teammoeg.frostedheart.util.client.Point;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

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
    public void renderMain(GuiGraphics ms) {
        if (focus) {
            ms.pose().mulPose(new Quaternionf().rotateZ(Mth.PI/4));
            ms.pose().scale(1.5F, 1.5F, 1.5F);
            GuiUtil.renderIcon(ms, focusIcon, -5, -5, this.color);
            //focus的动画效果
            float progress = AnimationUtil.calcFadeIn(750, "waypoint" + this.id, false);
            if (progress == 1 && AnimationUtil.calcProgress(750, "waypoint2" + this.id, false) == 1) {
                AnimationUtil.removeAnimation("waypoint" + this.id);
                AnimationUtil.removeAnimation("waypoint2" + this.id);
            }
            int fadeColor = (int)((1-progress) * 255.0F) << 24 | this.color & 0x00FFFFFF;
            //让不透明度可以低于0.1
            ms.pose().scale(progress+0.25F, progress+0.25F, progress+0.25F);
            ms.fill(-5, -5, 5, 5, fadeColor);
            ms.pose().scale(0.66667F, 0.66667F, 0.66667F);
            return;
        }

        ms.pose().mulPose(new Quaternionf().rotateZ(Mth.PI/4));
        //防止被MC的神必半透明材质覆盖
        ms.pose().translate(0, 0, 1);
        GuiUtil.renderIcon(ms, icon, -5, -5, this.color);
    }


    @Override
    public void renderText(GuiGraphics ms) {
        ms.pose().translate(15, -3.5F, 0);
        ms.fill(-3, -2, -2, height, this.color);
        ms.fill(-2, -2, maxTextWidth+2, height, 0x40000000);

        maxTextWidth = MC.player.isShiftKeyDown() ? Math.max(96, maxTextWidth) : 96;
        height = 10 * GuiUtil.drawWrapString(this.id, ms, 0, 0, maxTextWidth, this.color, 10, false);
        
        if (MC.player.isShiftKeyDown()) {
            ms.fill(-3, height, -2, height+30, this.color);
            ms.fill(-2, height, maxTextWidth+2, height+30, 0x40000000);

            Vec3 v = new Vec3(this.target);
            String distance = "Distance: " + (int)v.distanceTo(MC.player.position()) + " blocks";
            maxTextWidth = Math.max(MC.font.width(distance), maxTextWidth);
            ms.drawString(MC.font, distance, 0, height+10, this.color);

            String pos = "[X: %.2f, Y: %.2f, Z: %.2f]";
            pos = String.format(pos, this.target.x(), this.target.y(), this.target.z());
            maxTextWidth = Math.max(MC.font.width(pos), maxTextWidth);
            ms.drawString(MC.font, pos, 0, height+20, this.color);
        }
        ms.pose().translate(-15, 3.5F, 0);
    }
}
