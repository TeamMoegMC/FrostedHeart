/*
 * Copyright (c) 2026 TeamMoeg
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
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

// 如何添加新参数
// 1. 在下面加
// 2. 在 Tip 构造函数中添加
// 3. 在 Tip.Builder 中添加，添加对应方法
// 2. 在 CODEC 中添加
// 4. 在 TipHelper EDITOR 中添加
// 6. 注意顺序，以及 CODEC 最大只支持 16 个参数

public record Tip(
        String id,
        List<String> contents,
        String category,
        @Nullable ResourceLocation image,
        String nextTip,
        List<String> unlocks,
        List<String> children,
        ClickActions.ClickAction clickAction,
        Display display,
        boolean temporary
) {
    public static final Codec<Tip> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Tip::id),
            Codec.STRING.listOf().fieldOf("contents").forGetter(Tip::contents),
            Codec.STRING.optionalFieldOf("category", "").forGetter(Tip::category),
            ResourceLocation.CODEC.optionalFieldOf("image").forGetter(t -> t.image == null ? Optional.empty() : Optional.of(t.image())),
            Codec.STRING.optionalFieldOf("nextTip", "").forGetter(Tip::nextTip),
            Codec.STRING.listOf().optionalFieldOf("unlock", List.of()).forGetter(Tip::unlocks),
            Codec.STRING.listOf().optionalFieldOf("children", List.of()).forGetter(Tip::children),
            ClickActions.CODEC.optionalFieldOf("clickAction", ClickActions.NO_ACTION).forGetter(Tip::clickAction),
            Display.CODEC.optionalFieldOf("display", Display.DEFAULT).forGetter(Tip::display)
    ).apply(instance, Tip::new));

    public record Display(List<ItemStack> displayItems, int displayTime, int fontColor, int backgroundColor, boolean alwaysVisible, boolean onceOnly, boolean hide, boolean pin) {
        public static final Display DEFAULT = new Display(List.of(), 12000, Colors.CYAN, Colors.BLACK, false, true, false ,false);
        public static final Codec<Display> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(ItemStack.CODEC).optionalFieldOf("displayItems", DEFAULT.displayItems).forGetter(Display::displayItems),
                Codec.INT.optionalFieldOf("displayTime", DEFAULT.displayTime).forGetter(Display::displayTime),
                Codec.INT.optionalFieldOf("fontColor", DEFAULT.fontColor).forGetter(Display::fontColor),
                Codec.INT.optionalFieldOf("backgroundColor", DEFAULT.backgroundColor).forGetter(Display::backgroundColor),
                Codec.BOOL.optionalFieldOf("alwaysVisible", DEFAULT.alwaysVisible).forGetter(Display::alwaysVisible),
                Codec.BOOL.optionalFieldOf("onceOnly", DEFAULT.onceOnly).forGetter(Display::onceOnly),
                Codec.BOOL.optionalFieldOf("hide", DEFAULT.hide).forGetter(Display::hide),
                Codec.BOOL.optionalFieldOf("pin", DEFAULT.pin).forGetter(Display::pin)
        ).apply(instance, Display::new));

        public Display(Collection<ItemStack> displayItems, int displayTime, int fontColor, int backgroundColor, boolean alwaysVisible, boolean onceOnly, boolean hide, boolean pin) {
            this(new ArrayList<>(displayItems == null ? List.of() : displayItems), displayTime, fontColor, backgroundColor, alwaysVisible, onceOnly, hide, pin);
        }
    }

    public static final Tip EMPTY = builder("/empty/").temporary().build();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Logger LOGGER = LogUtils.getLogger();

    Tip(String id, Collection<String> contents, String category, ResourceLocation image, String nextTip, Collection<String> unlocks, Collection<String> children, ClickActions.ClickAction clickAction, Display display) {
        this(id,
                new ArrayList<>(contents == null ? List.of() : contents),
                category,
                image.toString().equals("minecraft:") ? null : image,
                nextTip,
                new ArrayList<>(unlocks == null ? List.of() : unlocks),
                new ArrayList<>(children == null ? List.of() : children),
                clickAction,
                display,
                false);
    }
    Tip(String id, Collection<String> contents, String category, Optional<ResourceLocation> image, String nextTip, Collection<String> unlocks, Collection<String> children, ClickActions.ClickAction clickAction, Display display) {
        this(id,
                new ArrayList<>(contents == null ? List.of() : contents),
                category,
                image.orElse(null),
                nextTip,
                new ArrayList<>(unlocks == null ? List.of() : unlocks),
                new ArrayList<>(children == null ? List.of() : children),
                clickAction,
                display,
                false);
    }

    public String getTitle() {
        return contents.isEmpty() ? id : contents.get(0);
    }

    public Builder copy() {
        return new Builder(TipHelper.randomString())
                .contents(contents)
                .children(children)
                .unlocks(unlocks)
                .category(category)
                .nextTip(nextTip)
                .image(image)
                .clickAction(clickAction)
                .display(display)
                .temporary();
    }

    public Builder copy(String newId) {
        if (id.equals(newId) || TipManager.INSTANCE.hasTip(newId)) {
            return copy();
        }
        return new Builder(newId)
                .contents(contents)
                .children(children)
                .unlocks(unlocks)
                .category(category)
                .nextTip(nextTip)
                .image(image)
                .clickAction(clickAction)
                .display(display)
                .temporary();
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private final List<String> contents = new ArrayList<>();
        private final List<String> children = new ArrayList<>();
        private final List<String> unlocks = new ArrayList<>();
        private String category = "";
        private String nextTip = "";
        private ResourceLocation image;
        private ClickActions.ClickAction clickAction = ClickActions.NO_ACTION;

        private final List<ItemStack> displayItems = Display.DEFAULT.displayItems;
        private boolean alwaysVisible = Display.DEFAULT.alwaysVisible;
        private boolean onceOnly = Display.DEFAULT.onceOnly;
        private boolean hide = Display.DEFAULT.hide;
        private boolean pin = Display.DEFAULT.pin;
        private int displayTime = Display.DEFAULT.displayTime;
        private int fontColor = Display.DEFAULT.fontColor;
        private int backgroundColor = Display.DEFAULT.backgroundColor;
        private boolean temporary;

        public Builder(String id) {
            this.id = id;
        }

        public Builder contents(String... contents) {
            this.contents.addAll(List.of(contents));
            return this;
        }

        public Builder contents(Collection<String> contents) {
            this.contents.addAll(contents);
            return this;
        }

        public Builder children(Collection<String> children) {
            this.children.addAll(children);
            return this;
        }

        public Builder unlocks(Collection<String> unlocks) {
            this.unlocks.addAll(unlocks);
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder nextTip(String nextTip) {
            this.nextTip = nextTip;
            return this;
        }

        public Builder image(ResourceLocation image) {
            this.image = image;
            return this;
        }

        public Builder display(Display display) {
            displayTime(display.displayTime());
            items(display.displayItems());
            alwaysVisible(display.alwaysVisible());
            onceOnly(display.onceOnly());
            hide(display.hide());
            pin(display.pin());
            fontColor(display.fontColor());
            backgroundColor(display.backgroundColor());
            return this;
        }

        public Builder items(ItemStack... items) {
            this.displayItems.addAll(List.of(items));
            return this;
        }

        public Builder items(Collection<ItemStack> items) {
            this.displayItems.addAll(items);
            return this;
        }

        public Builder alwaysVisible() {
            this.alwaysVisible = true;
            return this;
        }

        public Builder onceOnly() {
            this.onceOnly = true;
            return this;
        }

        public Builder hide() {
            this.hide = true;
            return this;
        }

        public Builder pin() {
            this.pin = true;
            return this;
        }

        public Builder alwaysVisible(boolean alwaysVisible) {
            this.alwaysVisible = alwaysVisible;
            return this;
        }

        public Builder onceOnly(boolean onceOnly) {
            this.onceOnly = onceOnly;
            return this;
        }

        public Builder hide(boolean hide) {
            this.hide = hide;
            return this;
        }

        public Builder pin(boolean pin) {
            this.pin = pin;
            return this;
        }

        public Builder displayTime(int displayTime) {
            this.displayTime = displayTime;
            return this;
        }

        public Builder fontColor(int fontColor) {
            this.fontColor = fontColor;
            return this;
        }

        public Builder backgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder clickAction(ClickActions.ClickAction action) {
            this.clickAction = action;
            return this;
        }

        public Builder clickAction(ResourceLocation action, String content) {
            this.clickAction = ClickActions.create(action, content);
            return this;
        }

        public Builder clickAction(String action, String content) {
            return clickAction(FHMain.rl(action), content);
        }

        public Builder temporary() {
            this.temporary = true;
            return this;
        }

        public Tip build() {
            return new Tip(id,
                    new ArrayList<>(contents),
                    category,
                    image,
                    nextTip,
                    new ArrayList<>(unlocks),
                    new ArrayList<>(children),
                    clickAction,
                    new Display(displayItems, displayTime, fontColor, backgroundColor, alwaysVisible, onceOnly, hide, pin),
                    temporary);
        }
    }
}
