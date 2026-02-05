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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;
import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.chorda.client.CameraHelper;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.io.Writeable;
import com.teammoeg.chorda.text.Components;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.INBTSerializable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
public abstract class AbstractWaypoint implements Writeable, INBTSerializable<CompoundTag> {
    protected static final Logger LOGGER = LogUtils.getLogger();
    /**
     * 路径点的 ID
     */
    protected String id;
    /**
     * 路径点的显示名称
     */
    @Setter
    protected Component displayName;
    /**
     * 路径点的目标坐标
     */
    @Setter
    protected Vec3 target = Vec3.ZERO;
    /**
     * 路径点被聚焦/锁定
     */
    @Setter
    protected boolean focused;
    /**
     * 路径点是否启用，未启用的不会被渲染
     */
    @Setter
    protected boolean enabled;
    @Setter
    protected boolean hovered;
    /**
     * 路径点是否有效，如果为 false 会在下一次渲染前删除
     */
    protected boolean valid;
    /**
     * 路径点的渲染颜色
     */
    @Setter
    protected int color;
    /**
     * 路径点所处的维度
     */
    protected ResourceLocation dimension;
    protected Vec2 screenPos = Vec2.ZERO;
    protected final List<Component> tooltip = new ArrayList<>();

    AbstractWaypoint(Vec3 target, String ID, int color) {
        this.id = ID;
        this.color = color;
        this.target = target;
        this.displayName = Components.str(ID);
        this.focused = false;
        this.enabled = true;
        this.valid = true;

        if (ClientUtils.getWorld() != null) {
            this.dimension = ClientUtils.getDimLocation();
        } else {
            this.dimension = Level.OVERWORLD.location();
        }
    }

    AbstractWaypoint(BlockPos target, String ID, int color) {
        this(target.getCenter(), ID, color);
    }

    AbstractWaypoint(CompoundTag nbt) {
        deserializeNBT(nbt);
    }

    AbstractWaypoint(FriendlyByteBuf buffer) {
        this(buffer.readNbt());
    }

    public void render(GuiGraphics graphics) {
        updateScreenPos();
        PoseStack pose = graphics.pose();

        pose.pushPose();
        pose.translate(getScreenPos().x, getScreenPos().y, -1);
        if (ClientWaypointManager.getSelected().isPresent()) {
            var selected = ClientWaypointManager.getSelected().get();
            if (selected != this && this.hovered) {
                graphics.setColor(1, 1, 1, 0.1F);
            }
            renderMain(graphics);
            if (selected == this) {
                if (ClientWaypointManager.shouldUpdate()) {
                    tooltip.clear();
                    updateTooltip();
                }
                if (!tooltip.isEmpty()) {
                    // FIXME 半透明背景会剔除其他HUD元素
                    renderTooltip(graphics);
                }
            } else {
                tooltip.clear();
            }
        } else {
            renderMain(graphics);
        }
        pose.popPose();
        graphics.setColor(1, 1, 1, 1);
    }

    /**
     * 渲染路径点的主体部分
     */
    public abstract void renderMain(GuiGraphics graphics);

    /**
     * 渲染Tooltip
     */
    public abstract void renderTooltip(GuiGraphics graphics);

    /**
     * 更新Tooltip，每 tick 更新一次
     */
    protected abstract void updateTooltip();

    /**
     * 向Tooltip添加文本行
     * @param index 行数索引，如果是 -1 添加到结尾
     */
    protected void addTooltipLine(Component line, int index) {
        if (index >= 0) {
            this.tooltip.add(Math.min(tooltip.size(), index), line);
        } else {
            this.tooltip.add(line);
        }
    }

    protected void addTooltipLine(Component line) {
        addTooltipLine(line, -1);
    }

    protected void addTooltipLines(Collection<Component> lines, int index) {
        if (index < 0) {
            var reverse = new ArrayList<>(lines);
            Collections.reverse(reverse);
            reverse.forEach(line -> addTooltipLine(line, index));
            return;
        }
        lines.forEach(this::addTooltipLine);
    }

    /**
     * 当路径点在客户端被删除时调用
     */
    public abstract void onClientRemove();

    /**
     * 当路径点在服务端被删除时调用
     */
    public abstract void onServerRemove();

    protected void updateScreenPos() {
        Vec2 pos = CameraHelper.worldPosToScreenPos(getTarget());
        //限制区域避免覆盖其他HUD元素
        float x = Mth.clamp(pos.x, 10, ClientUtils.screenWidth() -10);
        float y = Mth.clamp(pos.y, 25, ClientUtils.screenHeight()-25);
        screenPos = new Vec2(x, y);
    }

    /**
     * 获取玩家距离路径点的距离
     */
    public double getDistance() {
        return getTarget().distanceTo(ClientUtils.getPlayer().getEyePosition(ClientUtils.partialTicks()));
    }
    
    public void setBlockTarget(BlockPos newTarget) {
        this.target = newTarget.getCenter();
    }

    public void invalidate() {
        valid = false;
    }

    protected MutableComponent distanceTranslation() {
        String distance = getDistance() < 0 ? "???" : String.valueOf(Math.round(getDistance()));
        return Lang.waypoint("distance", distance).component();
    }

    protected MutableComponent posTranslation() {
        return Lang.waypoint("position", String.format("%.2f", getTarget().x), String.format("%.2f", getTarget().y), String.format("%.2f", getTarget().z)).component();
    }

    @Override
    public abstract JsonElement serialize();

    @Override
    public abstract void write(FriendlyByteBuf buffer);

    @Override
    public abstract CompoundTag serializeNBT();

    @Override
    public abstract void deserializeNBT(CompoundTag nbt);

    @Override
    public String toString() {
        return "ID: '" + id + "' [" + target.x + ", " + target.y + ", " + target.z + "]";
    }
}
