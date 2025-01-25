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

package com.teammoeg.frostedheart.content.tips.client.gui.widget;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipManager;
import com.teammoeg.frostedheart.content.tips.TipRenderer;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.widget.ActionStateIconButton;
import com.teammoeg.chorda.client.widget.ColorEditbox;
import com.teammoeg.chorda.client.widget.IconButton;
import com.teammoeg.chorda.client.widget.IconCheckbox;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TipEditsList extends ContainerObjectSelectionList<TipEditsList.EditEntry> {
    private final Font font;
    private String cachedId = "";

    public TipEditsList(Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight) {
        super(pMinecraft, pWidth, pHeight, pY0, pY1, pItemHeight);
        this.font = pMinecraft.font;
        setRenderHeader(true, 10);

        var idEntry = (StringEntry)children().get(addEntry(new StringEntry("id", Component.translatable("gui.frostedheart.tip_editor.id"))));
        idEntry.input.setMaxLength(240);
        idEntry.input.setResponder(s -> {
            if (TipManager.INSTANCE.hasTip(s)) {
                idEntry.input.setTextColor(ColorHelper.RED);
                updatePreview(Component.translatable("tips.frostedheart.error.load.duplicate_id").withStyle(ChatFormatting.RED));
                return;
            } else if (Tip.isTipIdInvalid(s)) {
                idEntry.input.setTextColor(ColorHelper.RED);
            } else {
                idEntry.input.setTextColor(ColorHelper.WHITE);
            }
            updatePreview();
        });

        addEntry(new MultiComponentEntry("contents", Component.translatable("gui.frostedheart.tip_editor.contents")));

        var nextTipEntry = (StringEntry)children().get(addEntry(new StringEntry("nextTip", Component.translatable("gui.frostedheart.tip_editor.next_tip"))));
        nextTipEntry.input.setResponder(s -> {
            if (Tip.isTipIdInvalid(s)) {
                nextTipEntry.input.setTextColor(ColorHelper.RED);
                updatePreview(
                        Component.translatable("tips.frostedheart.error.invalid_id").withStyle(ChatFormatting.RED),
                        Component.translatable("tips.frostedheart.error.load.tip_not_exists", s).withStyle(ChatFormatting.GOLD));
            } else if (!s.isBlank() && !TipManager.INSTANCE.hasTip(s)) {
                nextTipEntry.input.setTextColor(0xFFFF9F00);
                updatePreview(Component.translatable("tips.frostedheart.error.load.tip_not_exists", s).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            } else {
                nextTipEntry.input.setTextColor(ColorHelper.WHITE);
                updatePreview();
            }
        });

        addEntry(new StringEntry("category", Component.translatable("gui.frostedheart.tip_editor.category")));

        var imageEntry = (StringEntry)children().get(addEntry(new StringEntry("image", Component.translatable("gui.frostedheart.tip_editor.image"))));
        imageEntry.input.setResponder(s -> updatePreview(Component.translatable("gui.frostedheart.tip_editor.info.resource_location")));

        addEntry(new ColorEntry("fontColor", Component.translatable("gui.frostedheart.tip_editor.font_color"), ColorHelper.CYAN));
        addEntry(new ColorEntry("backgroundColor", Component.translatable("gui.frostedheart.tip_editor.background_color"), ColorHelper.BLACK));
        addEntry(new IntegerEntry("displayTime", Component.translatable("gui.frostedheart.tip_editor.display_time")));
        addEntry(new BooleanEntry("alwaysVisible", Component.translatable("gui.frostedheart.tip_editor.always_visible")));
        addEntry(new BooleanEntry("onceOnly", Component.translatable("gui.frostedheart.tip_editor.once_only")));
        addEntry(new BooleanEntry("hide", Component.translatable("gui.frostedheart.tip_editor.hide")));
        addEntry(new BooleanEntry("pin", Component.translatable("gui.frostedheart.tip_editor.pin")));
    }

    public void updatePreview(Component... infos) {
        TipRenderer.TIP_QUEUE.clear();
        TipRenderer.forceClose();
        var builder = Tip.builder("").fromJson(this.toJson());
        if (infos.length > 0) {
            builder.line(Component.literal("---------")).lines(infos);
        }
        Tip tip = builder.alwaysVisible(true).build();
        this.cachedId = tip.getId();
        tip.forceDisplay();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        children().forEach(e -> json.add(e.property, e.getValue()));
        return json;
    }

    @Override
    protected int getScrollbarPosition() {
        return ClientUtils.screenWidth() - 6;
    }

    public class ColorEntry extends IntegerEntry {
        public ColorEntry(String property, Component message, int defValue) {
            super(property, message);
            this.input = new ColorEditbox(font, 0, 0, 64, 12, message, true ,defValue);
            this.input.setResponder(s -> {
                try {
                    input.setTextColor(ColorHelper.WHITE);
                    Integer.parseUnsignedInt(s, 16);
                } catch (NumberFormatException e) {
                    input.setTextColor(ColorHelper.RED);
                }
                updatePreview();
            });
        }

        @Override
        public JsonElement getValue() {
            return new JsonPrimitive(input.getValue());
        }
    }

    public class IntegerEntry extends StringEntry {
        public IntegerEntry(String property, Component message) {
            super(property, message);
            this.input.setResponder(s -> {
                try {
                    input.setTextColor(ColorHelper.WHITE);
                    Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    input.setTextColor(ColorHelper.RED);
                }
                updatePreview();
            });
        }

        @Override
        public JsonElement getValue() {
            int value;
            try {
                value = Integer.parseInt(input.getValue());
            } catch (NumberFormatException e) {
                value = 0;
            }
            return new JsonPrimitive(value);
        }
    }

    public class MultiComponentEntry extends StringEntry {
        public static final String PREFIX = "tips.frostedheart.";
        protected final IconButton addButton;
        protected final IconButton deleteButton;
        protected final IconButton translationButton;
        private final List<String> contents = new ArrayList<>();
        private final List<String> translationContents = new ArrayList<>();

        private boolean translation;

        public MultiComponentEntry(String property, Component message) {
            super(property, message);
            this.input = new EditBox(font, 0, 0, 64, 12, message) {
                @Override
                public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
                    if (pKeyCode == GLFW.GLFW_KEY_ENTER || pKeyCode == GLFW.GLFW_KEY_KP_ENTER) {
                        addButton.onPress();
                        return true;
                    }
                    return super.keyPressed(pKeyCode, pScanCode, pModifiers);
                }
            };
            this.input.setResponder(s -> {
                if (contents.isEmpty()) {
                    updatePreview(Component.translatable("gui.frostedheart.tip_editor.info.enter"), Component.translatable("tips.frostedheart.error.load.empty").withStyle(ChatFormatting.GOLD));
                } else {
                    updatePreview(Component.translatable("gui.frostedheart.tip_editor.info.enter"));
                }
            });
            this.input.setMaxLength(1024);

            this.addButton = new IconButton(0, 0, IconButton.Icon.CHECK, ColorHelper.CYAN, Component.translatable("gui.frostedheart.tip_editor.add_line"), b -> {
                if (cachedId.isBlank()) return;

                String value = this.input.getValue().isBlank() ? " " : this.input.getValue();
                contents.add(value);
                String key = PREFIX + cachedId;
                if (contents.size() == 1) {
                    key += ".title";
                } else {
                    key += ".desc" + contents.size();
                }
                translationContents.add(key);

                input.setValue("");
                updatePreview(Component.translatable("gui.frostedheart.tip_editor.info.enter"));
            });

            this.deleteButton = new IconButton(0, 0, IconButton.Icon.TRASH_CAN, ColorHelper.CYAN, Component.translatable("gui.frostedheart.tip_editor.delete_last_line"), b -> {
                if (!contents.isEmpty()) {
                    contents.remove(contents.size() - 1);
                    translationContents.remove(contents.size());
                    updatePreview();
                }
            });

            this.translationButton = new ActionStateIconButton(0, 0, IconButton.Icon.LIST, ColorHelper.CYAN, Component.translatable("gui.frostedheart.tip_editor.convert_and_copy"), Component.translatable("gui.frostedheart.copied"), b -> {
                if (!contents.isEmpty()) {
                    StringBuilder copy = new StringBuilder();
                    for (int i = 0; i < translationContents.size(); i++) {
                        String content = translationContents.get(i);
                        copy.append("\"").append(content).append("\": \"").append(contents.get(i)).append("\",\n");
                    }
                    ClientUtils.mc().keyboardHandler.setClipboard(copy.substring(0, copy.length()-2)); // 删除最后一行的逗号和换行

                    translation = !translation;
                    updatePreview();
                }
            });
        }

        public List<String> getContents() {
            var c = translation ? translationContents : contents;
            return c.isEmpty() ? List.of("tips.frostedheart.error.load.empty") : c;
        }

        @Override
        public void render(@NotNull GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean p_93531_, float pPartialTick) {
            super.render(pGuiGraphics, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, p_93531_, pPartialTick);
            addButton.setPosition(pLeft + 146, pTop + (pHeight/2) - 10);
            addButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            deleteButton.setPosition(pLeft + 132, pTop + (pHeight/2) - 10);
            deleteButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            translationButton.setPosition(pLeft + 118, pTop + (pHeight/2) - 10);
            translationButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

        @Override
        public JsonElement getValue() {
            JsonArray contents = new JsonArray();
            getContents().forEach(contents::add);
            return contents;
        }

        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(addButton, deleteButton, translationButton, input);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return ImmutableList.of(addButton, deleteButton, translationButton, input);
        }
    }

    public class StringEntry extends EditEntry {
        protected EditBox input;

        public StringEntry(String property, Component message) {
            super(property, message);
            this.input = new EditBox(font, 0, 0, 64, 12, message);
            this.input.setResponder(s -> updatePreview());
            this.input.setMaxLength(1024);
        }

        @Override
        public void render(@NotNull GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean p_93531_, float pPartialTick) {
            pGuiGraphics.drawString(font, message, pLeft, pTop, ColorHelper.WHITE);
            input.setPosition(pLeft + 160, pTop);
            input.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

        @Override
        public JsonElement getValue() {
            return new JsonPrimitive(input.getValue());
        }

        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {
            return Collections.singletonList(input);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return Collections.singletonList(input);
        }
    }

    public class BooleanEntry extends EditEntry {
        protected final IconCheckbox checkbox;

        public BooleanEntry(String property, Component message) {
            super(property, message);
            this.checkbox = new IconCheckbox(0, 0, 2, message, false) {
                @Override
                public void onPress() {
                    super.onPress();
                    updatePreview();
                }
            };
        }

        @Override
        public void render(@NotNull GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean p_93531_, float pPartialTick) {
            pGuiGraphics.drawString(font, message, pLeft, pTop, ColorHelper.WHITE);
            checkbox.setX(pLeft + 160 + 64/2 - checkbox.getWidth()/2);
            checkbox.setY(pTop - 12/2);
            checkbox.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

        @Override
        public JsonElement getValue() {
            return new JsonPrimitive(checkbox.selected());
        }

        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {
            return Collections.singletonList(checkbox);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return Collections.singletonList(checkbox);
        }
    }

    public abstract static class EditEntry extends ContainerObjectSelectionList.Entry<EditEntry> {
        public final String property;
        public final Component message;

        protected EditEntry(String property, Component message) {
            this.property = property;
            this.message = message;
        }

        public abstract JsonElement getValue();
    }
}
