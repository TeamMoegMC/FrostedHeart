package com.teammoeg.frostedheart.content.tips;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import com.teammoeg.frostedheart.util.lang.Lang;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Getter
public class Tip {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Tip EMPTY = Tip.builder("empty").error(ErrorType.EMPTY).build();

    /**
     * 显示内容
     */
    private final List<Component> contents;
    private final String id;
    /**
     * 在条目列表中的分类
     */
    private final String category;
    /**
     * 关闭后显示的下一个 tip
     */
    private final String nextTip;
    /**
     * 图像
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
    }

    public void saveAsFile() {
        File file = new File(TipManager.TIP_PATH, this.id + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            String json = GSON.toJson(toJson());
            writer.write(json);
        } catch (IOException e) {
            LOGGER.error("Unable to save file: '{}'", file, e);
            Tip exception = Tip.builder("exception").error(ErrorType.SAVE, e).build();
            TipManager.INSTANCE.display().force(exception);
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
        nbt.putBoolean("alwaysVisible", alwaysVisible);
        nbt.putBoolean("onceOnly", onceOnly);
        nbt.putBoolean("hide", hide);
        nbt.putBoolean("pin", pin);
        nbt.putBoolean("temporary", temporary);
        nbt.putInt("displayTime", displayTime);
        nbt.putInt("fontColor", fontColor);
        nbt.putInt("backgroundColor", backgroundColor);
        ListTag contents = new ListTag();
        for (Component content : this.contents) {
            contents.add(StringTag.valueOf(content.getString()));
        }
        nbt.put("contents", contents);
        return nbt;
    }

    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("category", category);
        json.addProperty("nextTip", nextTip);
        json.addProperty("image", image == null ? "" : image.toString());
        json.addProperty("alwaysVisible", alwaysVisible);
        json.addProperty("onceOnly", onceOnly);
        json.addProperty("hide", hide);
        json.addProperty("pin", pin);
        json.addProperty("temporary", temporary);
        json.addProperty("displayTime", displayTime);
        json.addProperty("fontColor", Integer.toHexString(fontColor).toUpperCase());
        json.addProperty("backgroundColor", Integer.toHexString(backgroundColor).toUpperCase());
        var contents = new JsonArray();
        for (Component content : this.contents) {
            if (content instanceof MutableComponent component && (component.getContents() instanceof TranslatableContents translatable)) {
                contents.add(translatable.getKey());
            } else {
                contents.add(content.getString());
            }
        }
        json.add("contents", contents);
        return json;
    }

    public static Tip.Builder builder(String id) {
        return new Builder(id);
    }

    protected static Tip fromJsonFile(File filePath) {
        LOGGER.debug("Loading tip '{}'", filePath.getName());
        Tip.Builder builder = builder("temp");
        if (!filePath.exists()) {
            LOGGER.error("File does not exists '{}'", filePath);
            builder.error(ErrorType.NOT_EXISTS);
        } else {
            try {
                String content = new String(Files.readAllBytes(Paths.get(String.valueOf(filePath))));
                builder.fromJson(GSON.fromJson(content, JsonElement.class).getAsJsonObject());
                return new Tip(builder);

            } catch (JsonSyntaxException e) {
                LOGGER.error("Invalid JSON format '{}'", filePath, e);
                builder.error(ErrorType.INVALID, e);

            } catch (Exception e) {
                LOGGER.error("Unable to load file '{}'", filePath, e);
                builder.error(ErrorType.LOAD, e);
            }
        }
        return builder.build();
    }

    @Getter
    public static class Builder {
        private final List<Component> contents = new ArrayList<>();
        private String id;
        private String category = "";
        private String nextTip = "";
        private ResourceLocation image;
        private boolean alwaysVisible;
        private boolean onceOnly;
        private boolean hide;
        private boolean pin;
        private boolean temporary;
        private int displayTime = 30000;
        private int fontColor = FHColorHelper.CYAN;
        private int BGColor = FHColorHelper.BLACK;

        public Builder(String id) {
            this.id = id;
        }

        public Tip build() {
            return new Tip(this);
        }

        public Builder nextTip(String next) {
            this.nextTip = next;
            return this;
        }

        public Builder line(Component text) {
            this.contents.add(text);
            return this;
        }

        public Builder lines(Collection<Component> texts) {
            this.contents.addAll(texts);
            return this;
        }

        public Builder clearContents() {
            this.contents.clear();
            return this;
        }

        public Builder image(ResourceLocation image) {
            this.image = image;
            return this;
        }

        public Builder alwaysVisible(boolean alwaysVisible) {
            this.alwaysVisible = alwaysVisible;
            return this;
        }

        public Builder pin(boolean pin) {
            this.pin = pin;
            return this;
        }

        public Builder setTemporary() {
            this.temporary = true;
            return this;
        }

        public Builder color(int fontColor, int BGColor) {
            this.fontColor = fontColor;
            this.BGColor = BGColor;
            return this;
        }

        public Builder displayTime(int time) {
            this.displayTime = time;
            return this;
        }

        public Builder error(ErrorType type, Component... descriptions) {
            return clearContents()
                    .line(Lang.tips("error." + type.key).component())
                    .lines(Arrays.asList(descriptions))
                    .line(Lang.tips("error.desc").component())
                    .color(FHColorHelper.RED, FHColorHelper.BLACK)
                    .alwaysVisible(true)
                    .setTemporary()
                    .pin(true);
        }

        public Builder error(ErrorType type, Exception exception, Component... descriptions) {
            return error(type, descriptions)
                    .line(Lang.str(exception.getMessage()));
        }

        public Builder copy(Tip source) {
            this.contents.addAll(source.contents);
            this.category = source.category;
            this.nextTip = source.nextTip;
            this.image = source.image;
            this.alwaysVisible = source.alwaysVisible;
            this.onceOnly = source.onceOnly;
            this.hide = source.hide;
            this.pin = source.pin;
            this.temporary = source.temporary;
            this.displayTime = source.displayTime;
            this.fontColor = source.fontColor;
            this.BGColor = source.backgroundColor;
            return this;
        }

        public Builder fromJson(JsonObject json) {
            if (json.has("id"             )) this.id            = json.get("id").getAsString();
            if (json.has("category"       )) this.category      = json.get("category").getAsString();
            if (json.has("next"           )) this.nextTip       = json.get("next").getAsString();
            if (json.has("alwaysVisible"  )) this.alwaysVisible = json.get("alwaysVisible").getAsBoolean();
            if (json.has("onceOnly"       )) this.onceOnly      = json.get("onceOnly").getAsBoolean();
            if (json.has("hide"           )) this.hide          = json.get("hide").getAsBoolean();
            if (json.has("pin"            )) this.pin           = json.get("pin").getAsBoolean();
            if (json.has("visibleTime"    )) this.displayTime   = Math.max(json.get("visibleTime").getAsInt(), 0);
            if (json.has("fontColor"      )) this.fontColor     = Integer.parseUnsignedInt(json.get("fontColor").getAsString(), 16);
            if (json.has("backgroundColor")) this.BGColor       = Integer.parseUnsignedInt(json.get("backgroundColor").getAsString(), 16);
            if (json.has("image")) {
                Optional.ofNullable(ResourceLocation.tryParse(json.get("image").getAsString())).ifPresentOrElse(this::image, () -> error(ErrorType.INVALID_IMAGE));
                ResourceLocation image = ResourceLocation.tryParse(json.get("image").getAsString());
                if (image != null) {
                    this.image = image;
                } else {
                    error(ErrorType.INVALID_IMAGE);
                    return this;
                }
            }
            if (json.has("contents")) {
                JsonArray jsonContents = json.getAsJsonArray("contents");
                for (int i = 0; i < jsonContents.size(); i++) {
                    String s = jsonContents.get(i).getAsString();
                    this.contents.add(Lang.translateOrElseStr(s));
                }
                if (this.contents.isEmpty()) {
                    error(ErrorType.EMPTY);
                    return this;
                }
            }

            return this;
        }

        public Builder fromNBT(CompoundTag nbt) {
            if (nbt != null) {
                this.id = nbt.getString("id");
                this.category = nbt.getString("category");
                this.nextTip = nbt.getString("nextTip");
                this.image = ResourceLocation.tryParse(nbt.getString("image"));
                this.alwaysVisible = nbt.getBoolean("alwaysVisible");
                this.onceOnly = nbt.getBoolean("onceOnly");
                this.hide = nbt.getBoolean("hide");
                this.pin = nbt.getBoolean("pin");
                this.temporary = nbt.getBoolean("temporary");
                this.displayTime = nbt.getInt("displayTime");
                this.fontColor = nbt.getInt("fontColor");
                this.BGColor = nbt.getInt("backgroundColor");

                var contents = nbt.getList("contents", Tag.TAG_STRING);
                var list = contents.stream().map(tag -> Lang.translateOrElseStr(tag.getAsString())).toList();
                this.contents.addAll(list);
            }
            return this;
        }

    }

    public enum ErrorType {
        OTHER("other"),
        LOAD("load"),
        SAVE("save"),
        EMPTY("empty"),
        INVALID("invalid"),
        INVALID_IMAGE("invalid_image"),
        NOT_EXISTS("not_exists");

        final String key;

        ErrorType(String key) {
            this.key = key;
        }
    }
}
