package com.teammoeg.frostedheart.content.waypoint.waypoints;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.util.client.AnimationUtil;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHGuiHelper;
import com.teammoeg.frostedheart.util.client.Point;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class waypoint extends AbstractWaypoint {
    /**
     * 路径点显示的图标
     */
    public Point icon = IconButton.ICON_BOX;
    /**
     * {@link #focus} 为 {@code true} 时使用的图标
     */
    public Point focusIcon = IconButton.ICON_BOX_ON;
    /**
     * 悬浮文本中最长文本的长度，用于显示背景
     */
    protected int maxTextWidth;

    public waypoint(Vector3f target, String ID, int color) {
        super(target, ID, color);
    }

    public waypoint(BlockPos target, String ID, int color) {
        super(target, ID, color);
    }

    public waypoint(CompoundNBT nbt) {
        super(nbt);
    }

    public waypoint(PacketBuffer buffer) {
        super(buffer);
    }

    @Override
    public void renderMain(MatrixStack ms) {
        ms.push();
        ms.rotate(Vector3f.ZN.rotationDegrees(45));
        if (focus) {
            ms.scale(1.5F, 1.5F, 1.5F);
            FHGuiHelper.renderIcon(ms, focusIcon, -5, -5, color);
            //focus的动画效果
            float progress = AnimationUtil.fadeIn(750, "waypoints" + id, false);
            if (progress == 1 && AnimationUtil.progress(750, "waypoint2" + id, false) == 1) {
                AnimationUtil.removeAnimation("waypoints" + id);
                AnimationUtil.removeAnimation("waypoint2" + id);
            }
            int fadeColor = (int)((1-progress) * 255.0F) << 24 | color & 0x00FFFFFF;
            //让不透明度可以低于0.1
            ms.scale(progress+0.25F, progress+0.25F, progress+0.25F);
            RenderSystem.disableAlphaTest();
            AbstractGui.fill(ms, -5, -5, 5, 5, fadeColor);
            RenderSystem.enableAlphaTest();
        } else {
            FHGuiHelper.renderIcon(ms, icon, -5, -5, color);
        }
        ms.pop();
    }

    @Override
    public void renderHoverInfo(MatrixStack ms) {
        boolean outScreen = maxTextWidth+20 + getScreenPos().x > ClientUtils.screenWidth();
        int totalHeight = infoLines.size() * 10;

        ms.push();
        ms.translate(outScreen ? -maxTextWidth-15 : 15, -3.5F, 0);
        AbstractGui.fill(ms, outScreen ? maxTextWidth+3 : -3, -2, outScreen ? maxTextWidth+2 : -2, totalHeight, color);
        AbstractGui.fill(ms, -2, -2, maxTextWidth+2, totalHeight, 0x80000000);
        for (int i = 0; i < infoLines.size(); i++) {
            Object line = infoLines.get(i);
            if (line == null) continue;
            if (line instanceof ITextComponent) {
                ClientUtils.font().drawText(ms, (ITextComponent)line, 0, i*10, color);
            } else {
                ClientUtils.font().drawString(ms, line.toString(), 0, i*10, color);
            }
        }
        ms.pop();
    }

    @Override
    public void updateInfos() {
        maxTextWidth = 0;

        //潜行时显示额外信息
        if (ClientUtils.getPlayer().movementInput.sneaking) {
            addInfoLine(null, -1);
            addInfoLine(distanceTranslation(), -1);
            addInfoLine(posTranslation(), -1);
        }

        List<String> lines;
        if (infoLines.isEmpty()) {
            //不显示额外信息时悬浮文本的最大宽度为96
            lines = FHGuiHelper.wrapString(displayName.getString(), 96);
        } else {
            //最大宽度为窗口的一半-24
            lines = FHGuiHelper.wrapString(displayName.getString(), Math.min(maxTextWidth, (int)(ClientUtils.screenWidth()*0.5F)-24));
        }
        addInfoLine(lines, 0);
    }

    @Override
    public void addInfoLine(Object line, int index) {
        if (line == null) {
            infoLines.add(null);

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
                infoLines.add(Math.min(infoLines.size(), index), line);
            } else {
                infoLines.add(line);
            }

            if (line instanceof ITextComponent) {
                maxTextWidth = Math.max(maxTextWidth, ClientUtils.font().getStringPropertyWidth((ITextComponent)line));
            } else {
                maxTextWidth = Math.max(maxTextWidth, ClientUtils.font().getStringWidth(line.toString()));
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
        if (displayName instanceof TranslationTextComponent) {
            json.addProperty("display_name", ((TranslationTextComponent)displayName).getKey());
        } else {
            json.addProperty("display_name", displayName.getString());
        }
        json.addProperty("dimension", dimension.toString());
        json.addProperty("x", target.getX());
        json.addProperty("y", target.getY());
        json.addProperty("z", target.getZ());
        json.addProperty("focus", focus);
        json.addProperty("enable", enable);
        json.addProperty("block_pos", blockPos);
        json.addProperty("color", color);
        return json;
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeCompoundTag(serializeNBT());
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putString("id", id);
        if (displayName instanceof TranslationTextComponent) {
            nbt.putString("display_name", ((TranslationTextComponent)displayName).getKey());
        } else {
            nbt.putString("display_name", displayName.getString());
        }
        nbt.putString("dimension", dimension.toString());
        nbt.putFloat("x", target.getX());
        nbt.putFloat("y", target.getY());
        nbt.putFloat("z", target.getZ());
        nbt.putBoolean("focus", focus);
        nbt.putBoolean("enable", enable);
        nbt.putBoolean("block_pos", blockPos);
        nbt.putInt("color", color);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.id = nbt.getString("id");
        String displayName = nbt.getString("display_name");
        if (I18n.hasKey(displayName)) {
            this.displayName = new TranslationTextComponent(displayName);
        } else {
            this.displayName = new StringTextComponent(displayName);
        }
        this.dimension = RegistryKey.getOrCreateKey(Registry.DIMENSION_TYPE_KEY, new ResourceLocation(nbt.getString("dimension"))).getLocation();
        this.target = new Vector3f(nbt.getFloat("x"), nbt.getFloat("y"), nbt.getFloat("z"));
        this.focus = nbt.getBoolean("focus");
        this.enable = nbt.getBoolean("enable");
        this.blockPos = nbt.getBoolean("block_pos");
        this.color = nbt.getInt("color");
        this.valid = true;
    }
}
