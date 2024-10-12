package com.teammoeg.frostedheart.content.waypoint.waypoints;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.teammoeg.frostedheart.content.waypoint.WaypointRenderer;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.RawMouseHelper;
import com.teammoeg.frostedheart.util.client.RenderHelper;
import com.teammoeg.frostedheart.util.io.Writeable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
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

public abstract class AbstractWaypoint implements Writeable, INBTSerializable<CompoundTag> {
    protected static final Logger LOGGER = LogUtils.getLogger();
    /**
     * 路径点的 ID
     */
    protected String id;
    /**
     * 路径点的显示名称
     */
    public Component displayName;
    /**
     * 路径点的目标坐标
     */
    public Vec3 target;
    /**
     * 路径点记录的是否为方块坐标
     */
    protected boolean blockPos;
    /**
     * 路径点被聚焦/锁定
     */
    public boolean focus;
    /**
     * 路径点是否启用，未启用的不会被渲染
     */
    public boolean enable;
    /**
     * 路径点是否有效，如果为 false 会在下一次渲染前删除
     */
    public boolean valid;
    /**
     * 路径点的渲染颜色
     */
    public int color;
    /**
     * 路径点所处的维度
     */
    public ResourceKey<Level> dimension;

    /**
     * 路径点在屏幕中的坐标
     */
    protected Vec2 screenPos;

    /**
     * 路径点的悬停信息
     */
    protected List<Object> infoLines = new ArrayList<>();

    AbstractWaypoint(Vec3 target, String ID, int color) {
        this.id = ID;
        this.color = color;
        this.target = target;
        this.displayName = TranslateUtils.str(ID);
        this.focus = false;
        this.enable = true;
        this.valid = true;

        if (ClientUtils.getWorld() != null) {
            this.dimension = ClientUtils.getWorld().dimension();
        } else {
            this.dimension = Level.OVERWORLD;
        }
    }

    AbstractWaypoint(BlockPos target, String ID, int color) {
        this(new Vec3(target.getX(), target.getY(), target.getZ()), ID, color);
        this.blockPos = true;
    }

    AbstractWaypoint(CompoundTag nbt) {
        deserializeNBT(nbt);
    }

    AbstractWaypoint(FriendlyByteBuf buffer) {
        this(buffer.readNbt());
    }

    public void render(GuiGraphics graphics) {
        screenPos = getScreenPos();

        graphics.pose().pushPose();
        graphics.pose().translate(screenPos.x, screenPos.y, 1);
        renderMain(graphics);
        if (isHovered()) {
            if (WaypointRenderer.shouldUpdate()) {
                infoLines.clear();
                updateInfos();
            }
            if (!infoLines.isEmpty()) {
                renderHoverInfo(graphics);
            }
        } else {
            infoLines.clear();
        }
        graphics.pose().popPose();
    }

    /**
     * 渲染路径点的主体部分
     */
    public abstract void renderMain(GuiGraphics graphics);

    /**
     * 渲染悬停信息
     */
    public abstract void renderHoverInfo(GuiGraphics graphics);

    /**
     * 更新悬停信息，每 50 毫秒更新一次
     */
    protected abstract void updateInfos();

    /**
     * 向悬停信息添加文本行
     * @param line 如果是 {@link Collection} 则添加其中的全部元素
     * @param index 行数索引，如果是 -1 添加到结尾
     */
    protected void addInfoLine(Object line, int index) {
        if (line == null) {
            this.infoLines.add(null);

        } else if (line instanceof Collection) {
            Collection<?> collection = (Collection<?>) line;
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
                this.infoLines.add(Math.min(infoLines.size(), index), line);
            } else {
                this.infoLines.add(line);
            }
        }
    }

    /**
     * 玩家是否注视或鼠标是否悬停在路径点上
     */
    public boolean isHovered() {
        boolean hovered = false;
        if (!ClientUtils.mc().mouseHandler.isMouseGrabbed()) {
            hovered = RawMouseHelper.isMouseIn(RawMouseHelper.getScaledX(), RawMouseHelper.getScaledY(), (int)(screenPos.x-5), (int)(screenPos.y-5), 10, 10);
        }
        if (!hovered) {
            double xP = screenPos.x / ClientUtils.screenWidth();
            double yP = screenPos.y / ClientUtils.screenHeight();
            hovered = xP >= 0.45 && xP <= 0.55 && yP >= 0.45 && yP <= 0.55;
        }
        return hovered;
    }

    /**
     * 当路径点在客户端被删除时调用
     */
    public abstract void onClientRemove();

    /**
     * 当路径点在服务端被删除时调用
     */
    public abstract void onServerRemove();

    /**
     * 获取路径点的屏幕坐标
     */
    public Vec2 getScreenPos() {
        if (target == null) {
            return Vec2.ZERO;
        }
        Vec2 pos = RenderHelper.worldPosToScreenPos(target);
        //将坐标限制在屏幕中心区域里
        float x = Mth.clamp(pos.x, 10, ClientUtils.screenWidth() -10);
        float y = Mth.clamp(pos.y, 25, ClientUtils.screenHeight()-25);
        return new Vec2(x, y);
    }

    /**
     * 获取玩家距离路径点的距离
     */
    public double getDistance() {
        if (target == null) {
            return -1;
        }
        return target.distanceTo(ClientUtils.getPlayer().getLookAngle());
    }

    public Vec3 getTarget() {
        if (blockPos) {
            return target.add(0.5F, 0.5F, 0.5F);
        }
        return target;
    }

    public String getID() {
        return id;
    }

    protected MutableComponent distanceTranslation() {
        return TranslateUtils.translateWaypoint("distance", (int)getDistance());
    }

    protected MutableComponent posTranslation() {
        return TranslateUtils.translateWaypoint("position", String.format("%.2f", target.x), String.format("%.2f", target.y), String.format("%.2f", target.z));
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
