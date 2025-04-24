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
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;

import com.teammoeg.chorda.client.AnimationUtil;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.widget.IconButton;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Waypoint extends AbstractWaypoint {
    /**
     * 路径点显示的图标
     */
    public IconButton.Icon icon = IconButton.Icon.BOX;
    /**
     * {@link #focus} 为 {@code true} 时使用的图标
     */
    public IconButton.Icon focusIcon = IconButton.Icon.BOX_ON;
    /**
     * 悬浮文本中最长文本的长度，用于显示背景
     */
    protected int maxTextWidth;

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
        if (focus) {
            pose.scale(1.5F, 1.5F, 1.5F);
            CGuiHelper.renderIcon(pose, focusIcon, -5, -5, color);
            //focus的动画效果
            float progress = AnimationUtil.fadeIn(750, "waypoints" + id, false);
            if (progress == 1 && AnimationUtil.progress(750, "waypoint2" + id, false) == 1) {
                AnimationUtil.remove("waypoints" + id);
                AnimationUtil.remove("waypoint2" + id);
            }
            int fadeColor = (int)((1-progress) * 255.0F) << 24 | color & 0x00FFFFFF;
            pose.scale(progress+0.25F, progress+0.25F, progress+0.25F);
            graphics.fill(-5, -5, 5, 5, fadeColor);
        } else {
            CGuiHelper.renderIcon(pose, icon, -5, -5, color);
        }
        pose.popPose();
    }

    @Override
    public void renderHoverInfo(GuiGraphics graphics) {
        boolean outScreen = maxTextWidth+20 + getScreenPos().x > ClientUtils.screenWidth();
        int totalHeight = infoLines.size() * 10;

        graphics.pose().pushPose();
        graphics.pose().translate(outScreen ? -maxTextWidth-15 : 15, -3.5F, 0);
        graphics.fill(outScreen ? maxTextWidth+3 : -3, -2, outScreen ? maxTextWidth+2 : -2, totalHeight, color);
        graphics.fill(-2, -2, maxTextWidth+2, totalHeight, 0x80000000);
        for (int i = 0; i < infoLines.size(); i++) {
            Object line = infoLines.get(i);
            if (line == null) continue;
            if (line instanceof Component) {
                graphics.drawString(ClientUtils.font(), (Component)line, 0, i*10, color, false);
            } else if (line instanceof FormattedCharSequence) {
                graphics.drawString(ClientUtils.font(), (FormattedCharSequence)line, 0, i*10, color, false);
            } else {
                graphics.drawString(ClientUtils.font(), line.toString(), 0, i*10, color, false);
            }
        }
        graphics.pose().popPose();
    }

    @Override
    public void updateInfos() {
        maxTextWidth = 0;

        //潜行时显示额外信息
        if (ClientWaypointManager.shouldShowExtra()) {
            addInfoLine(null, -1);
            addInfoLine(distanceTranslation(), -1);
            if (ClientUtils.getPlayer().isCreative()) {
                addInfoLine(posTranslation(), -1);
            }
        }

        List<FormattedCharSequence> lines;
        if (infoLines.isEmpty()) {
            //不显示额外信息时悬浮文本的最大宽度为96
            lines = ClientUtils.font().split(displayName, 96);
        } else {
            //最大宽度为窗口的一半-24
            lines = ClientUtils.font().split(displayName, Math.min(maxTextWidth, (int)(ClientUtils.screenWidth()*0.5F)-24));
        }
        addInfoLine(lines, 0);
    }

    @Override
    public void addInfoLine(Object line, int index) {
        if (line == null) {
            infoLines.add(null);

        } else if (line instanceof Collection<?> collection) {
            if (!collection.isEmpty()) {
                //防止添加时顺序乱掉
                List<Object> linesToAdd = new ArrayList<>(collection);
                Collections.reverse(linesToAdd);
                for (Object o : linesToAdd) {
                    addInfoLine(o, index);
                }
            }

        } else {
            if (index >= 0) {
                infoLines.add(Math.min(infoLines.size(), index), line);
            } else {
                infoLines.add(line);
            }

            if (line instanceof Component c) {
                maxTextWidth = Math.max(maxTextWidth, ClientUtils.font().width(c));
            } else if (line instanceof FormattedCharSequence f) {
                maxTextWidth = Math.max(maxTextWidth, ClientUtils.font().width(f));
            } else {
                maxTextWidth = Math.max(maxTextWidth, ClientUtils.font().width(line.toString()));
            }
        }
    }

    @Override
    public void onClientRemove() {

    }

    @Override
    public void onServerRemove() {

    }

    @Override
    public JsonElement serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        if (displayName instanceof TranslatableContents name) {
            json.addProperty("display_name", name.getKey());
        } else {
            json.addProperty("display_name", displayName.getString());
        }
        json.addProperty("dimension", dimension.toString());
        json.addProperty("x", target.x);
        json.addProperty("y", target.y);
        json.addProperty("z", target.z);
        json.addProperty("focus", focus);
        json.addProperty("enable", enable);
        json.addProperty("block_pos", blockPos);
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
        if (displayName instanceof TranslatableContents name) {
            nbt.putString("display_name", name.getKey());
        } else {
            nbt.putString("display_name", displayName.getString());
        }
        nbt.putString("dimension", dimension.toString());
        nbt.putDouble("x", target.x);
        nbt.putDouble("y", target.y);
        nbt.putDouble("z", target.z);
        nbt.putBoolean("focus", focus);
        nbt.putBoolean("enable", enable);
        nbt.putBoolean("block_pos", blockPos);
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
        this.focus = nbt.getBoolean("focus");
        this.enable = nbt.getBoolean("enable");
        this.blockPos = nbt.getBoolean("block_pos");
        this.color = nbt.getInt("color");
        this.valid = true;
    }
}
