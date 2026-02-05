/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.waypoint.waypoints;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.AnimationUtil;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.content.archive.Alignment;
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class Waypoint extends AbstractWaypoint {
    /**
     * 路径点显示的图标
     */
    public FlatIcon icon = FlatIcon.BOX;
    /**
     * {@link #focused} 为 {@code true} 时使用的图标
     */
    public FlatIcon focusIcon = FlatIcon.BOX_ON;
    /**
     * 悬浮文本中最长文本的长度，用于显示背景
     */
    protected int tooltipWidth;
    protected List<FormattedCharSequence> cachedSplit = new ArrayList<>();

    public Waypoint(Vec3 target, String ID, int color) {
        super(target, ID, color);
    }

    public Waypoint(BlockPos target, String ID, int color) {
        super(target, ID, color);
    }

    public Waypoint(CompoundTag nbt) {
        super(nbt);
    }

    public Waypoint(FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void renderMain(GuiGraphics graphics) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.mulPose(new Quaternionf().rotateZ(Mth.PI/4));
        if (focused) {
            pose.scale(1.5F, 1.5F, 1.5F);
            focusIcon.render(pose, -5, -5, color);
            //focus的动画效果
            float progress = AnimationUtil.fadeIn(750, "waypoint_" + id, false);
            if (progress == 1 && AnimationUtil.progress(750, "waypoint_cd_" + id, false) == 1) {
                AnimationUtil.remove("waypoint_" + id);
                AnimationUtil.remove("waypoint_cd_" + id);
            }
            int fadeColor = (int)((1-progress) * 255.0F) << 24 | color & 0x00FFFFFF;
            pose.scale(progress+0.25F, progress+0.25F, progress+0.25F);
            graphics.fill(-5, -5, 5, 5, fadeColor);
        } else {
            icon.render(pose, -5, -5, color);
        }
        pose.popPose();
    }

    @Override
    public void renderTooltip(GuiGraphics graphics) {
        boolean outScreen = tooltipWidth +20 + getScreenPos().x > ClientUtils.screenWidth();
        float offsetX = outScreen ? -tooltipWidth -15 : 15;
        float offsetY = -3.5F;
        int height = cachedSplit.size() * 10;
        int backgroundColor = Colors.setAlpha(Colors.BLACK, 0.5F);

        graphics.pose().pushPose();
        graphics.pose().translate(offsetX, offsetY, 0);

        graphics.fill(outScreen ? tooltipWidth +3 : -3, -2, outScreen ? tooltipWidth +2 : -2, height, color);
        graphics.fill(-2, -2, tooltipWidth +2, height, backgroundColor);
        CGuiHelper.drawStringLinesInBound(graphics, ClientUtils.font(), cachedSplit, 0, 0, tooltipWidth, color,
                1, false, 0, Alignment.LEFT);

        graphics.pose().popPose();
    }

    @Override
    public void updateTooltip() {
        cachedSplit.clear();
        tooltipWidth = 0;

        addTooltipLine(this.displayName);
        // 潜行时添加额外信息
        if (ClientWaypointManager.shouldShowExtra()) {
            addAdvancedLines();
        }

        for (Component line : tooltip) {
            cachedSplit.addAll(ClientUtils.font().split(line, tooltipWidth));
        }
    }

    public void addAdvancedLines() {
        addTooltipLine(Component.empty());
        addTooltipLine(distanceTranslation());
        if (ClientUtils.getPlayer().isCreative()) {
            addTooltipLine(posTranslation());
        }
    }

    @Override
    public void addTooltipLine(Component line, int index) {
        super.addTooltipLine(line, index);
        // 不显示详细信息时最大96，否则为窗口的一半-24
        tooltipWidth = Math.min(ClientUtils.font().width(line),
                ClientWaypointManager.shouldShowExtra() ? (int)(ClientUtils.screenWidth()*0.5F)-24 : 96);
    }

    @Override
    public void onClientRemove() {
        AnimationUtil.remove("waypoint_" + id);
        AnimationUtil.remove("waypoint_cd_" + id);
    }

    @Override
    public void onServerRemove() {

    }

    @Override
    public JsonElement serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("display_name", Components.getKeyOrElseStr(displayName));
        json.addProperty("dimension", dimension.toString());
        json.addProperty("x", target.x);
        json.addProperty("y", target.y);
        json.addProperty("z", target.z);
        json.addProperty("focus", focused);
        json.addProperty("enable", enabled);
        json.addProperty("color", color);
        return json;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeNbt(serializeNBT());
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", id);
        nbt.putString("display_name", Components.getKeyOrElseStr(displayName));
        nbt.putString("dimension", dimension.toString());
        nbt.putDouble("x", target.x);
        nbt.putDouble("y", target.y);
        nbt.putDouble("z", target.z);
        nbt.putBoolean("focus", focused);
        nbt.putBoolean("enable", enabled);
        nbt.putInt("color", color);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.id = nbt.getString("id");
        String displayName = nbt.getString("display_name");
        this.displayName = Component.translatable(displayName);
        this.dimension = Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, nbt.get("dimension")).resultOrPartial(LOGGER::error).orElse(Level.OVERWORLD).location();
        this.target = new Vec3(nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"));
        this.focused = nbt.getBoolean("focus");
        this.enabled = nbt.getBoolean("enable");
        this.color = nbt.getInt("color");
        this.valid = true;
    }
}
