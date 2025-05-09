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

package com.teammoeg.frostedheart.content.tips;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.io.FileUtil;
import com.teammoeg.chorda.lang.Components;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class Tip {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Component ERROR_DESC = Component.translatable("tips.frostedheart.error.desc");
    public static final Tip EMPTY = Tip.builder("empty").error(ErrorType.OTHER,Component.translatable("tips.frostedheart.error.other"), ERROR_DESC).build();


    /**
     * Tip 的 ID
     */
    private final String id;
    /**
     * 显示内容
     */
    private final List<Component> contents;
    /**
     * 在条目列表中的分类
     */
    private final String category;
    /**
     * 关闭后显示的下一个 tip
     */
    private final String nextTip;
    /**
     * 显示的图像
     */
    @Nullable
    private final ResourceLocation image;
    /**
     * 是否始终显示
     */
    private final boolean alwaysVisible;
    /**
     * 是否只显示一次
     */
    private final boolean onceOnly;
    /**
     * 是否在列表中隐藏
     */
    private final boolean hide;
    /**
     * 是否在添加到显示队列时置顶
     */
    private final boolean pin;
    /**
     * 是否为临时 tip，临时 tip 关闭后不会储存任何状态
     */
    private final boolean temporary;
    /**
     * 显示时长，单位为毫秒
     */
    private final int displayTime;
    /**
     * 文本颜色
     */
    private final int fontColor;
    /**
     * 背景颜色
     */
    private final int backgroundColor;
    /**
     * 跳转按钮的操作
     */
    private final String clickAction;
    /**
     * 跳转按钮的内容
     */
    private final String clickActionContent;

    protected Tip(Tip.Builder builder) {
        this.contents = builder.contents;
        this.id = builder.id;
        this.category = builder.category;
        this.nextTip = builder.nextTip;
        this.image = builder.image;
        this.alwaysVisible = builder.alwaysVisible;
        this.onceOnly = builder.onceOnly;
        this.hide = builder.hide;
        this.pin = builder.pin;
        this.temporary = builder.temporary;
        this.displayTime = builder.displayTime;
        this.fontColor = builder.fontColor;
        this.backgroundColor = builder.BGColor;
        this.clickAction = builder.clickAction;
        this.clickActionContent = builder.clickActionContent;
    }

    public void display() {
        TipManager.INSTANCE.display().general(this);
    }

    public void forceDisplay() {
        TipManager.INSTANCE.display().force(this);
    }

    public boolean hasNext() {
        return TipManager.INSTANCE.hasTip(nextTip);
    }

    public boolean hasClickAction() {
        return !clickAction.isBlank() && !clickActionContent.isBlank();
    }

    public boolean saveAsFile() {
        if (this.id.isBlank()) {
            builder("exception").error(ErrorType.SAVE, Component.translatable("tips.frostedheart.error.load.no_id")).build().forceDisplay();
            return false;
        } else if (isTipIdInvalid(this.id)) {
            builder("exception").error(ErrorType.SAVE, Component.literal("ID: " + this.id), Component.translatable("tips.frostedheart.error.invalid_id")).build().forceDisplay();
            return false;
        } /*else if (TipManager.INSTANCE.hasTip(this.id)) {
            builder("exception").error(ErrorType.SAVE, Component.literal("ID: " + this.id), Component.translatable("tips.frostedheart.error.load.duplicate_id")).build().forceDisplay();
            return false;
        }*/

        File file = new File(TipManager.TIP_PATH, this.id + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            String json = GSON.toJson(toJson());
            writer.write(json);
            return true;
        } catch (IOException e) {
            LOGGER.error("Unable to save file: '{}'", file, e);
            Tip.builder("exception").error(ErrorType.SAVE, e).build().forceDisplay();
            return false;
        }
    }

    public FriendlyByteBuf write(FriendlyByteBuf buffer) {
        return buffer.writeNbt(toNBT());
    }

    public CompoundTag toNBT() {
        var nbt = new CompoundTag();
        nbt.putString("id", id);
        nbt.putString("category", category);
        nbt.putString("nextTip", nextTip);
        nbt.putString("image", image == null ? "" : image.toString());
        nbt.putString("clickAction", clickAction);
        nbt.putString("clickActionContent", clickActionContent);
        nbt.putBoolean("alwaysVisible", alwaysVisible);
        nbt.putBoolean("onceOnly", onceOnly);
        nbt.putBoolean("hide", hide);
        nbt.putBoolean("pin", pin);
        nbt.putBoolean("temporary", temporary);
        nbt.putInt("displayTime", displayTime);
        nbt.putInt("fontColor", fontColor);
        nbt.putInt("backgroundColor", backgroundColor);
        var toAddContents = new ListTag();
        this.contents.stream().map(content -> StringTag.valueOf(Components.getKeyOrElseStr(content))).forEach(toAddContents::add);
        nbt.put("contents", toAddContents);
        return nbt;
    }

    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("category", category);
        json.addProperty("nextTip", nextTip);
        json.addProperty("image", image == null ? "" : image.toString());
        json.addProperty("clickAction", clickAction);
        json.addProperty("clickActionContent", clickActionContent);
        json.addProperty("alwaysVisible", alwaysVisible);
        json.addProperty("onceOnly", onceOnly);
        json.addProperty("hide", hide);
        json.addProperty("pin", pin);
        json.addProperty("displayTime", displayTime);
        json.addProperty("fontColor", Integer.toHexString(fontColor).toUpperCase());
        json.addProperty("backgroundColor", Integer.toHexString(backgroundColor).toUpperCase());
        var toAddContents = new JsonArray();
        this.contents.stream().map(Components::getKeyOrElseStr).forEach(toAddContents::add);
        json.add("contents", toAddContents);
        return json;
    }

    public static Tip.Builder builder(String id) {
        return new Builder(id);
    }

    public static Tip fromJsonFile(File filePath) {
        LOGGER.debug("Loading tip '{}'", filePath.getName());
        Tip.Builder builder = builder("exception");
        if (!filePath.exists()) {
            LOGGER.error("File does not exists '{}'", filePath);
            builder.error(ErrorType.LOAD, Component.literal(filePath.toString()),Component.translatable("tips.frostedheart.error.load.file_not_exists", ERROR_DESC));
        } else {
            try {
                String content = FileUtil.readString(filePath);
                builder.fromJson(GSON.fromJson(content, JsonElement.class).getAsJsonObject());
                return new Tip(builder);

            } catch (JsonSyntaxException e) {
                LOGGER.error("Invalid JSON format '{}'", filePath, e);
                builder.error(ErrorType.LOAD, e, Component.literal(builder.id),Component.translatable("tips.frostedheart.error.load.invalid_json", ERROR_DESC));

            } catch (Exception e) {
                LOGGER.error("Unable to load file '{}'", filePath, e);
                builder.error(ErrorType.LOAD, e, Component.literal(builder.id),Component.translatable("tips.frostedheart.error.other"), ERROR_DESC);
            }
        }
        return builder.build();
    }

    public static boolean isTipIdInvalid(String id) {
        return id.matches(".*[<>:\"/\\\\|?*§].*");
    }

    public static class Builder {
        private final List<Component> contents = new ArrayList<>();
        private String id = "";
        private String category = "";
        private String nextTip = "";
        private String clickAction = "";
        private String clickActionContent = "";
        private ResourceLocation image;
        private boolean alwaysVisible;
        private boolean onceOnly;
        private boolean hide;
        private boolean pin;
        private boolean temporary;
        private int displayTime = 30000;
        private int fontColor = ColorHelper.CYAN;
        private int BGColor = ColorHelper.BLACK;

        private boolean editable = true;

        public Builder(String id) {
            if (isTipIdInvalid(id)) {
                error(ErrorType.LOAD, Component.literal("ID: " + id), Component.translatable("tips.frostedheart.error.invalid_id"));
            }
            this.id = id;
            setTemporary();
        }

        public Tip build() {
            return new Tip(this);
        }

        public Builder nextTip(String next) {
            if (!editable) return this;
            this.nextTip = next;
            return this;
        }

        public Builder category(String category) {
            if (!editable) return this;
            this.category = category;
            return this;
        }

        public Builder line(Component text) {
            if (!editable) return this;
            this.contents.add(text);
            return this;
        }

        public Builder lines(Collection<Component> texts) {
            if (!editable) return this;
            this.contents.addAll(texts);
            return this;
        }

        public Builder lines(Component... texts) {
            return lines(List.of(texts));
        }

        public Builder clearContents() {
            if (!editable) return this;
            this.contents.clear();
            return this;
        }

        public Builder image(ResourceLocation image) {
            if (!editable) return this;
            this.image = image;
            return this;
        }

        public Builder alwaysVisible(boolean alwaysVisible) {
            if (!editable) return this;
            this.alwaysVisible = alwaysVisible;
            return this;
        }

        public Builder onceOnly(boolean onceOnly) {
            if (!editable) return this;
            this.onceOnly = onceOnly;
            return this;
        }

        public Builder hide(boolean hide) {
            if (!editable) return this;
            this.hide = hide;
            return this;
        }

        public Builder pin(boolean pin) {
            if (!editable) return this;
            this.pin = pin;
            return this;
        }

        public Builder setTemporary() {
            if (!editable) return this;
            this.temporary = true;
            return this;
        }

        public Builder fontColor(int fontColor) {
            if (!editable) return this;
            this.fontColor = fontColor;
            return this;
        }

        public Builder BGColor(int BGColor) {
            if (!editable) return this;
            this.BGColor = BGColor;
            return this;
        }

        public Builder color(int fontColor, int BGColor) {
            if (!editable) return this;
            this.fontColor = fontColor;
            this.BGColor = BGColor;
            return this;
        }

        public Builder displayTime(int time) {
            if (!editable) return this;
            this.displayTime = time;
            return this;
        }

        public Builder clickAction(String name, String content) {
            if (!editable) return this;
            this.clickAction = name;
            this.clickActionContent = content;
            return this;
        }

        public Builder copy(Tip source) {
            if (!editable) return this;

            this.contents.addAll(source.contents);
            this.category = source.category;
            this.nextTip = source.nextTip;
            this.image = source.image;
            this.alwaysVisible = source.alwaysVisible;
            this.onceOnly = source.onceOnly;
            this.hide = source.hide;
            this.pin = source.pin;
            this.temporary = true;
            this.displayTime = source.displayTime;
            this.fontColor = source.fontColor;
            this.BGColor = source.backgroundColor;
            return this;
        }

        public Builder fromNBT(CompoundTag nbt) {
            if (!editable) return this;

            if (nbt != null) {
                setTemporary();
                this.id = nbt.getString("id");
                category(nbt.getString("category"));
                nextTip(nbt.getString("nextTip"));
                clickAction(nbt.getString("clickAction"), nbt.getString("clickActionContent"));
                alwaysVisible(nbt.getBoolean("alwaysVisible"));
                onceOnly(nbt.getBoolean("onceOnly"));
                hide(nbt.getBoolean("hide"));
                pin(nbt.getBoolean("pin"));
                displayTime(nbt.getInt("displayTime"));
                color(nbt.getInt("fontColor"), nbt.getInt("backgroundColor"));

                String location = nbt.getString("image");
                if (!location.isBlank()) image(ResourceLocation.tryParse(location));

                var contents = nbt.getList("contents", Tag.TAG_STRING);
                var list = contents.stream().map(tag -> Component.translatable(tag.getAsString())).toList();
                this.contents.addAll(list);
            }
            if (id.isBlank()) {
                error(ErrorType.OTHER, Component.literal("NBT does not contain a tip"));
            }
            return this;
        }

        public Builder fromJson(JsonObject json) {
            if (!editable || json == null) return this;

            if (json.has("id")) {
                String s = json.get("id").getAsString();
                if (s.isBlank()) {
                    error(ErrorType.LOAD,Component.translatable("tips.frostedheart.error.load.no_id"));
                } else if (isTipIdInvalid(s)) {
                    error(ErrorType.LOAD, Component.literal("ID: " + s), Component.translatable("tips.frostedheart.error.invalid_id"));
                }
                id = s;
            } else {
                error(ErrorType.LOAD,Component.translatable("tips.frostedheart.error.load.no_id"));
                return this;
            }

            if (json.has("contents")) {
                JsonArray jsonContents = json.getAsJsonArray("contents");
                if (jsonContents != null) {
                    for (int i = 0; i < jsonContents.size(); i++) {
                        String s = jsonContents.get(i).getAsString();
                        line(Component.translatable(s));
                    }
                }
            }
            if (this.contents.isEmpty()) {
                error(ErrorType.LOAD, Component.literal("ID: " + id), Component.translatable("tips.frostedheart.error.load.empty"));
                return this;
            }

            if (json.has("image")) {
                String location = json.get("image").getAsString();
                if (!location.isBlank()) {
                    ResourceLocation image = ResourceLocation.tryParse(location);
                    if (image != null) {
                        image(image);
                    } else {
                        error(ErrorType.LOAD, Component.literal("ID: " + id), Component.translatable("tips.frostedheart.error.load.invalid_image", location));
                        return this;
                    }
                }
            }

            if (json.has("category"       )) category     (json.get("category").getAsString());
            if (json.has("nextTip"        )) nextTip      (json.get("nextTip").getAsString());
            if (json.has("clickAction"    )) clickAction  (json.get("clickAction").getAsString(), json.get("clickActionContent").getAsString());
            if (json.has("alwaysVisible"  )) alwaysVisible(json.get("alwaysVisible").getAsBoolean());
            if (json.has("onceOnly"       )) onceOnly     (json.get("onceOnly").getAsBoolean());
            if (json.has("hide"           )) hide         (json.get("hide").getAsBoolean());
            if (json.has("pin"            )) pin          (json.get("pin").getAsBoolean());
            if (json.has("displayTime"    )) displayTime  (Math.max(json.get("displayTime").getAsInt(), 0));
            if (json.has("fontColor"      )) fontColor    (getColorOrElse(json, "fontColor", ColorHelper.CYAN));
            if (json.has("backgroundColor")) BGColor      (getColorOrElse(json, "backgroundColor", ColorHelper.BLACK));

            temporary = false;
            return this;
        }

        public Builder error(ErrorType type, Collection<Component> descriptions) {
            clearContents()
                    .line(Component.translatable("tips.frostedheart.error." + type.key))
                    .lines(descriptions)
                    .color(ColorHelper.RED, ColorHelper.BLACK)
                    .alwaysVisible(true)
                    .setTemporary()
                    .pin(true)
                    .clickAction("OpenURL", Component.translatable("gui.frostedheart.issue_tracker").getString());
            this.editable = false;
            return this;
        }

        public Builder error(ErrorType type, Component... descriptions) {
            return error(type, List.of(descriptions));
        }

        public Builder error(ErrorType type, Exception exception, Component... descriptions) {
            var desc = new ArrayList<>(List.of(descriptions));
            desc.add(Component.literal(exception.getMessage()));
            return error(type, desc);
        }

        private int getColorOrElse(JsonObject json, String name, int defColor) {
            try {
                return Integer.parseUnsignedInt(json.get(name).getAsString(), 16);
            } catch (NumberFormatException e) {
                error(ErrorType.LOAD,Component.translatable("tips.frostedheart.error.load.invalid_digit", name));
                return defColor;
            }
        }
    }

    public enum ErrorType {
        OTHER("other"),
        LOAD("load"),
        SAVE("save"),
        DISPLAY("display");

        final String key;

        ErrorType(String key) {
            this.key = key;
        }
    }
}
