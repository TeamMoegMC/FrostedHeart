package com.teammoeg.frostedheart.content.tips;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import com.teammoeg.frostedheart.util.lang.Lang;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.Size2i;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class Tip {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Component ERROR_DESC = Component.translatable("tips.frostedheart.error.desc");
    public static final Tip EMPTY = Tip.builder("empty").error(ErrorType.OTHER, Lang.tips("error.not_exists").component(), ERROR_DESC).build();

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
     * 显示的图像
     */
    @Nullable
    private final ResourceLocation image;
    /**
     * 图像的宽高
     */
    @Nullable
    private final Size2i imageSize;
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
        this.imageSize = builder.imageSize;
        this.alwaysVisible = builder.alwaysVisible;
        this.onceOnly = builder.onceOnly;
        this.hide = builder.hide;
        this.pin = builder.pin;
        this.temporary = builder.temporary;
        this.displayTime = builder.displayTime;
        this.fontColor = builder.fontColor;
        this.backgroundColor = builder.BGColor;
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

    public boolean saveAsFile() {
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
        nbt.putBoolean("alwaysVisible", alwaysVisible);
        nbt.putBoolean("onceOnly", onceOnly);
        nbt.putBoolean("hide", hide);
        nbt.putBoolean("pin", pin);
        nbt.putBoolean("temporary", temporary);
        nbt.putInt("displayTime", displayTime);
        nbt.putInt("fontColor", fontColor);
        nbt.putInt("backgroundColor", backgroundColor);
        var toAddContents = new ListTag();
        this.contents.stream().map(content -> StringTag.valueOf(Lang.getKeyOrElseStr(content))).forEach(toAddContents::add);
        nbt.put("contents", toAddContents);
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
        json.addProperty("displayTime", displayTime);
        json.addProperty("fontColor", Integer.toHexString(fontColor).toUpperCase());
        json.addProperty("backgroundColor", Integer.toHexString(backgroundColor).toUpperCase());
        var toAddContents = new JsonArray();
        this.contents.stream().map(Lang::getKeyOrElseStr).forEach(toAddContents::add);
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
            builder.error(ErrorType.LOAD, Lang.str(filePath.toString()), Lang.tips("error.file_not_exists", ERROR_DESC).component());
        } else {
            try {
                String content = new String(Files.readAllBytes(Paths.get(String.valueOf(filePath))));
                builder.fromJson(GSON.fromJson(content, JsonElement.class).getAsJsonObject());
                return new Tip(builder);

            } catch (JsonSyntaxException e) {
                LOGGER.error("Invalid JSON format '{}'", filePath, e);
                builder.error(ErrorType.LOAD, e, Lang.str(builder.id), Lang.tips("error.invalid_json", ERROR_DESC).component());

            } catch (Exception e) {
                LOGGER.error("Unable to load file '{}'", filePath, e);
                builder.error(ErrorType.LOAD, e, Lang.str(builder.id), Lang.tips("error.other").component(), ERROR_DESC);
            }
        }
        return builder.build();
    }

    public static class Builder {
        private final List<Component> contents = new ArrayList<>();
        private String id;
        private String category = "";
        private String nextTip = "";
        private ResourceLocation image;
        private Size2i imageSize;
        private boolean alwaysVisible;
        private boolean onceOnly;
        private boolean hide;
        private boolean pin;
        private boolean temporary;
        private int displayTime = 30000;
        private int fontColor = FHColorHelper.CYAN;
        private int BGColor = FHColorHelper.BLACK;

        private boolean editable = true;

        public Builder(String id) {
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
            imageSize(image);
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

        public Builder copy(Tip source) {
            if (!editable) return this;

            this.contents.addAll(source.contents);
            this.category = source.category;
            this.nextTip = source.nextTip;
            this.image = source.image;
            this.imageSize = source.imageSize;
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
                alwaysVisible(nbt.getBoolean("alwaysVisible"));
                onceOnly(nbt.getBoolean("onceOnly"));
                hide(nbt.getBoolean("hide"));
                pin(nbt.getBoolean("pin"));
                displayTime(nbt.getInt("displayTime"));
                color(nbt.getInt("fontColor"), nbt.getInt("backgroundColor"));

                String location = nbt.getString("image");
                if (!location.isBlank()) image(ResourceLocation.tryParse(location));

                var contents = nbt.getList("contents", Tag.TAG_STRING);
                var list = contents.stream().map(tag -> Lang.translateOrElseStr(tag.getAsString())).toList();
                this.contents.addAll(list);
            }
            if (id.isBlank()) {
                error(ErrorType.OTHER, Lang.str("NBT does not contain tip"));
            }
            return this;
        }

        public Builder fromJson(JsonObject json) {
            if (!editable || json == null) return this;

            if (json.has("id")) {
                String s = json.get("id").getAsString();
                if (s.isBlank()) {
                    error(ErrorType.LOAD, Lang.tips("error.load.no_id").component());
                    return this;
                } else if (TipManager.INSTANCE.hasTip(s)) {
                    error(ErrorType.LOAD, Lang.str("ID: " + s), Lang.tips("error.load.duplicate_id").component());
                    return this;
                }
                id = s;
            } else {
                error(ErrorType.LOAD, Lang.tips("error.load.no_id").component());
                return this;
            }

            if (json.has("contents")) {
                JsonArray jsonContents = json.getAsJsonArray("contents");
                if (jsonContents != null) {
                    for (int i = 0; i < jsonContents.size(); i++) {
                        String s = jsonContents.get(i).getAsString();
                        line(Lang.translateOrElseStr(s));
                    }
                }
            }
            if (this.contents.isEmpty()) {
                error(ErrorType.LOAD, Lang.str("ID: " + id), Lang.tips("error.load.empty").component());
                return this;
            }

            if (json.has("image")) {
                String location = json.get("image").getAsString();
                if (!location.isBlank()) {
                    ResourceLocation image = ResourceLocation.tryParse(location);
                    if (image != null) {
                        image(image);
                    } else {
                        error(ErrorType.LOAD, Lang.str("ID: " + id), Lang.tips("error.load.invalid_image", location).component());
                        return this;
                    }
                }
            }

            if (json.has("category"       )) category     (json.get("category").getAsString());
            if (json.has("nextTip"        )) nextTip      (json.get("nextTip").getAsString());
            if (json.has("alwaysVisible"  )) alwaysVisible(json.get("alwaysVisible").getAsBoolean());
            if (json.has("onceOnly"       )) onceOnly     (json.get("onceOnly").getAsBoolean());
            if (json.has("hide"           )) hide         (json.get("hide").getAsBoolean());
            if (json.has("pin"            )) pin          (json.get("pin").getAsBoolean());
            if (json.has("displayTime"    )) displayTime  (Math.max(json.get("displayTime").getAsInt(), 0));
            if (json.has("fontColor"      )) fontColor    (getColorOrElse(json, "fontColor", FHColorHelper.CYAN));
            if (json.has("backgroundColor")) BGColor      (getColorOrElse(json, "backgroundColor", FHColorHelper.BLACK));

            temporary = false;
            return this;
        }

        public Builder error(ErrorType type, Collection<Component> descriptions) {
            clearContents()
                    .line(Lang.tips("error." + type.key).component())
                    .lines(descriptions)
                    .color(FHColorHelper.RED, FHColorHelper.BLACK)
                    .alwaysVisible(true)
                    .setTemporary()
                    .pin(true);
            this.editable = false;
            return this;
        }

        public Builder error(ErrorType type, Component... descriptions) {
            return error(type, List.of(descriptions));
        }

        public Builder error(ErrorType type, Exception exception, Component... descriptions) {
            var desc = new ArrayList<>(List.of(descriptions));
            desc.add(Lang.str(exception.getMessage()));
            return error(type, desc);
        }

        private void imageSize(ResourceLocation location) {
            var resource = ClientUtils.mc().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().open()) {
                    BufferedImage image= ImageIO.read(stream);
                    Size2i size = new Size2i(image.getWidth(), image.getHeight());
                    if (size.width != 0 || size.height != 0) {
                        this.imageSize = size;
                        return;
                    }
                } catch (IOException e) {
                    LOGGER.error("Invalid texture resource location {}", location, e);
                    error(ErrorType.LOAD, e, Lang.tips("error.load.invalid_image", location).component());
                }
            }
            this.image = null;
            error(ErrorType.LOAD, Lang.tips("error.load.invalid_image", location).component());
        }

        private int getColorOrElse(JsonObject json, String name, int defColor) {
            try {
                return Integer.parseUnsignedInt(json.get(name).getAsString(), 16);
            } catch (NumberFormatException e) {
                error(ErrorType.LOAD, Lang.tips("error.load.invalid_digit", name).component());
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
