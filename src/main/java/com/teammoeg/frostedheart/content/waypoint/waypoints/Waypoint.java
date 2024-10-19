package com.teammoeg.frostedheart.content.waypoint.waypoints;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.AnimationUtil;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHGuiHelper;
import com.teammoeg.frostedheart.util.client.Point;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
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
    public Point icon = IconButton.ICON_BOX;
    /**
     * {@link #focus} 为 {@code true} 时使用的图标
     */
    public Point focusIcon = IconButton.ICON_BOX_ON;
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
        graphics.pose().popPose();
        graphics.pose().mulPose(new Quaternionf().rotateZ(Mth.PI/4));
        if (focus) {
            graphics.pose().scale(1.5F, 1.5F, 1.5F);
            IconButton.renderIcon(graphics.pose(), focusIcon, -5, -5, color);
            //focus的动画效果
            float progress = AnimationUtil.fadeIn(750, "waypoints" + id, false);
            if (progress == 1 && AnimationUtil.progress(750, "waypoint2" + id, false) == 1) {
                AnimationUtil.remove("waypoints" + id);
                AnimationUtil.remove("waypoint2" + id);
            }
            int fadeColor = (int)((1-progress) * 255.0F) << 24 | color & 0x00FFFFFF;
            //让不透明度可以低于0.1
            graphics.pose().scale(progress+0.25F, progress+0.25F, progress+0.25F);
//            RenderSystem.disableAlphaTest();
            graphics.fill(-5, -5, 5, 5, fadeColor);
//            RenderSystem.enableAlphaTest();
        } else {
            IconButton.renderIcon(graphics.pose(), icon, -5, -5, color);
        }
        graphics.pose().popPose();
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
                graphics.drawString(ClientUtils.font(), (Component)line, 0, i*10, color);
            } else {
                graphics.drawString(ClientUtils.font(), line.toString(), 0, i*10, color);
            }
        }
        graphics.pose().popPose();
    }

    @Override
    public void updateInfos() {
        maxTextWidth = 0;

        //潜行时显示额外信息
        if (ClientUtils.getPlayer().isShiftKeyDown()) {
            addInfoLine(null, -1);
            addInfoLine(distanceTranslation(), -1);
            addInfoLine(posTranslation(), -1);
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

            if (line instanceof Component) {
                maxTextWidth = Math.max(maxTextWidth, ClientUtils.font().width((Component)line));
            } else if (line instanceof FormattedCharSequence) {
                maxTextWidth = Math.max(maxTextWidth, ClientUtils.font().width((FormattedCharSequence)line));
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
        if (displayName instanceof TranslatableContents) {
            json.addProperty("display_name", ((TranslatableContents)displayName).getKey());
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
        if (displayName instanceof TranslatableContents) {
            nbt.putString("display_name", ((TranslatableContents)displayName).getKey());
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
        if (I18n.exists(displayName)) {
            this.displayName = TranslateUtils.translate(displayName);
        } else {
            this.displayName = TranslateUtils.str(displayName);
        }
        this.dimension = Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, nbt.get("dimension")).resultOrPartial(LOGGER::error).orElse(Level.OVERWORLD);
        this.target = new Vec3(nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"));
        this.focus = nbt.getBoolean("focus");
        this.enable = nbt.getBoolean("enable");
        this.blockPos = nbt.getBoolean("block_pos");
        this.color = nbt.getInt("color");
        this.valid = true;
    }
}
