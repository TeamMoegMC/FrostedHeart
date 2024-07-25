package com.teammoeg.frostedheart.content.waypoint.waypoints;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.content.waypoint.WaypointRenderer;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.RawMouseHelper;
import com.teammoeg.frostedheart.util.client.RenderHelper;
import com.teammoeg.frostedheart.util.io.Writeable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractWaypoint implements Writeable, INBTSerializable<CompoundNBT> {
    /**
     * 路径点的 ID
     */
    protected String id;
    /**
     * 路径点的显示名称
     */
    public ITextComponent displayName;
    /**
     * 路径点的目标坐标
     */
    public Vector3f target;
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
    public ResourceLocation dimension;

    /**
     * 路径点在屏幕中的坐标
     */
    protected Vector2f screenPos;

    /**
     * 路径点的悬停信息
     */
    protected List<Object> infoLines = new ArrayList<>();

    AbstractWaypoint(Vector3f target, String ID, int color) {
        this.id = ID;
        this.color = color;
        this.target = target;
        this.displayName = new StringTextComponent(ID);
        this.focus = false;
        this.enable = true;
        this.valid = true;

        if (ClientUtils.getWorld() != null) {
            this.dimension = ClientUtils.getWorld().getDimensionKey().getLocation();
        } else {
            this.dimension = DimensionType.OVERWORLD_ID;
        }
    }

    AbstractWaypoint(BlockPos target, String ID, int color) {
        this(new Vector3f(target.getX(), target.getY(), target.getZ()), ID, color);
        this.blockPos = true;
    }

    AbstractWaypoint(CompoundNBT nbt) {
        deserializeNBT(nbt);
    }

    AbstractWaypoint(PacketBuffer buffer) {
        this(buffer.readCompoundTag());
    }

    public void render(MatrixStack ms) {
        screenPos = getScreenPos();

        ms.push();
        ms.translate(screenPos.x, screenPos.y, 1);
        renderMain(ms);
        if (isHovered()) {
            if (WaypointRenderer.shouldUpdate()) {
                infoLines.clear();
                updateInfos();
            }
            if (!infoLines.isEmpty()) {
                renderHoverInfo(ms);
            }
        } else {
            infoLines.clear();
        }
        ms.pop();
    }

    /**
     * 渲染路径点的主体部分
     */
    public abstract void renderMain(MatrixStack ms);

    /**
     * 渲染悬停信息
     */
    public abstract void renderHoverInfo(MatrixStack ms);

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
        if (!ClientUtils.mc().mouseHelper.isMouseGrabbed()) {
            hovered = RawMouseHelper.isMouseIn(RawMouseHelper.getScaledMouseX(), RawMouseHelper.getScaledMouseY(), (int)(screenPos.x-5), (int)(screenPos.y-5), 10, 10);
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
    public Vector2f getScreenPos() {
        if (target == null) {
            return Vector2f.ZERO;
        }
        Vector2f pos = RenderHelper.worldPosToScreenPos(target);
        //将坐标限制在屏幕中心区域里
        float x = MathHelper.clamp(pos.x, 10, ClientUtils.screenWidth() -10);
        float y = MathHelper.clamp(pos.y, 25, ClientUtils.screenHeight()-25);
        return new Vector2f(x, y);
    }

    /**
     * 获取玩家距离路径点的距离
     */
    public double getDistance() {
        if (target == null) {
            return -1;
        }
        return new Vector3d(target).distanceTo(ClientUtils.getPlayer().getPositionVec());
    }

    public Vector3f getTarget() {
        if (blockPos) {
            Vector3f blockTarget = target.copy();
            blockTarget.add(0.5F, 0.5F, 0.5F);
            return blockTarget;
        }
        return target;
    }

    public String getID() {
        return id;
    }

    protected TranslationTextComponent distanceTranslation() {
        return TranslateUtils.translateWaypoint("distance", (int)getDistance());
    }

    protected TranslationTextComponent posTranslation() {
        return TranslateUtils.translateWaypoint("position", String.format("%.2f", target.getX()), String.format("%.2f", target.getY()), String.format("%.2f", target.getZ()));
    }

    @Override
    public abstract JsonElement serialize();

    @Override
    public abstract void write(PacketBuffer buffer);

    @Override
    public abstract CompoundNBT serializeNBT();

    @Override
    public abstract void deserializeNBT(CompoundNBT nbt);

    @Override
    public String toString() {
        return "ID: '" + id + "' [" + target.getX() + ", " + target.getY() + ", " + target.getZ() + "]";
    }
}
